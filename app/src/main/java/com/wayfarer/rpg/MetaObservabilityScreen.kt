package com.wayfarer.rpg

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONArray

@Composable
fun MetaChatScreen(modifier: Modifier = Modifier, gmContext: GmContext, scope: CoroutineScope) {
    val appContext = LocalContext.current
    val prefs = remember { appContext.getSharedPreferences("wayfarer_gm", Context.MODE_PRIVATE) }
    var model by remember {
        mutableStateOf(GmModelChoice.fromSaved(prefs.getString("model_choice", "AUTO")))
    }
    val onDeviceGemma = remember { OnDeviceGemmaRuntime.get(appContext) }
    LaunchedEffect(model, onDeviceGemma) {
        if (model == GmModelChoice.AUTO || model == GmModelChoice.ON_DEVICE) {
            try {
                onDeviceGemma.prepare()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Status is surfaced in the main GM model picker.
            }
        }
    }
    var session by remember { mutableStateOf(LoreWiseTranscripts.session("meta")) }
    var records by remember { mutableStateOf(LoreWiseTranscripts.latest(session)) }
    var input by rememberSaveable { mutableStateOf("") }
    var inspector by remember { mutableStateOf<JSONArray?>(null) }
    var picker by remember { mutableStateOf(false) }
    val busy = records.any { it.status == "started" }
    val latestContext by rememberUpdatedState(gmContext)
    LaunchedEffect(session) {
        while (isActive) {
            records = withContext(Dispatchers.IO) { LoreWiseTranscripts.latest(session) }
            delay(1000)
        }
    }
    fun send(question: String) {
        if (busy || question.isBlank()) return
        val captured = latestContext
        val selected = model
        val history = records.takeLast(12).flatMap {
            listOf("User: " + it.visibleMessage, "Meta model: " + it.response)
        }
        input = ""
        scope.launch {
            val service = MetaChatService { message ->
                GeminiGameMaster(selected, onDeviceGemma).metaChat(captured, message, history)
            }
            try { service.send(question) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { /* Error and visible question are retained in transcript. */ }
        }
    }
    Column(modifier.fillMaxSize().padding(12.dp)) {
        Text("Fourth Wall / Meta Chat", color = Gold, fontWeight = FontWeight.Bold)
        Text("Outside the fiction • read-only campaign context", color = Muted)
        Row {
            TextButton(onClick = { picker = true }, enabled = !busy) { Text(model.shortName) }
            TextButton(onClick = { inspector = gmContext.snapshot().json() }) { Text("Context Inspector") }
        }
        TextButton(enabled = !busy, onClick = { send(MetaChatService.KNOWLEDGE_QUESTION) }) {
            Text("What do you currently know?")
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.requestId }) { record ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("META • You → " + record.model.ifBlank { model.shortName }, color = Gold)
                        Text(record.visibleMessage, color = Text)
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Text(record.response.ifBlank { "Status: " + record.status }, color = Text)
                        if (record.status in listOf("error", "interrupted", "cancelled")) {
                            TextButton(enabled = !busy, onClick = { send(record.visibleMessage) }) { Text("Retry meta message") }
                        }
                        TextButton(onClick = { inspector = record.context }) { Text("Inspect supplied context") }
                    }
                }
            }
        }
        LoreWiseTranscripts.persistenceError?.let { Text("Transcript storage error: $it", color = Danger) }
        OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("Ask outside the fiction") }, maxLines = 3)
        Row {
            TextButton(enabled = !busy && input.isNotBlank(), onClick = { send(input.trim()) }) { Text(if (busy) "Asking…" else "Send meta") }
            TextButton(enabled = !busy, onClick = {
                LoreWiseTranscripts.rotate("meta")
                session = LoreWiseTranscripts.session("meta")
                records = emptyList()
            }) { Text("Archive / new session") }
        }
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("Selected GM / model") },
        text = { Column { GmModelChoice.entries.forEach { choice ->
            TextButton(onClick = {
                model = choice
                prefs.edit().putString("model_choice", choice.name).apply()
                picker = false
            }) { Text(choice.displayName) }
        } } }, confirmButton = { TextButton(onClick = { picker = false }) { Text("Close") } })
    inspector?.let { ContextInspector(it) { inspector = null } }
}

@Composable
fun ContextInspector(categories: JSONArray, onClose: () -> Unit) {
    val safe = remember(categories.toString()) { TranscriptRedactor.value(categories) as JSONArray }
    var reveal by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Context Inspector") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text("This shows supplied data, not proof that the model retained or understood it. Credentials are redacted.")
                TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) "Hide GM spoilers" else "Reveal hidden GM context (spoilers)") }
                for (i in 0 until safe.length()) {
                    val category = safe.getJSONObject(i)
                    var expanded by remember(category.optString("key")) { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(category.optString("label") + if (category.optBoolean("supplied")) " • supplied" else " • empty")
                    }
                    if (expanded) Text(
                        if (category.optBoolean("hidden") && !reveal) "Hidden GM content. Reveal spoilers to inspect."
                        else category.optString("content").ifBlank { "No content supplied in this category." }
                    )
                }
            }
        }, confirmButton = { TextButton(onClick = onClose) { Text("Close") } })
}

@Composable
fun TranscriptViewerScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf(LoreWiseTranscripts.sessions()) }
    var selected by remember { mutableStateOf(sessions.firstOrNull()) }
    var records by remember { mutableStateOf(selected?.let { LoreWiseTranscripts.latest(it) }.orEmpty()) }
    var pending by remember { mutableStateOf(LoreWiseTranscripts.pendingCount()) }
    var chooseSession by remember { mutableStateOf(false) }
    var inspector by remember { mutableStateOf<JSONArray?>(null) }
    LaunchedEffect(selected) {
        while (isActive) {
            withContext(Dispatchers.IO) {
                sessions = LoreWiseTranscripts.sessions()
                records = selected?.let { LoreWiseTranscripts.latest(it) }.orEmpty()
                pending = LoreWiseTranscripts.pendingCount()
            }
            delay(2000)
        }
    }
    Column(modifier.fillMaxSize().padding(12.dp)) {
        Text("AI transcripts", color = Gold, fontWeight = FontWeight.Bold)
        Text("User / app → model → effects and state changes", color = Muted)
        Text("$pending mirror records queued • local sessions remain complete", color = Muted)
        Row {
            TextButton(onClick = { chooseSession = true }) { Text("Sessions / archives") }
            TextButton(onClick = { scope.launch { LoreWiseTranscripts.uploadPending() } }) { Text("Retry mirror") }
        }
        Text(selected.orEmpty(), color = Muted)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.requestId }) { record ->
                var expanded by remember(record.requestId) { mutableStateOf(false) }
                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(record.channel.uppercase() + " • " + record.operation, color = Gold)
                        Text("User / app: " + record.visibleMessage, color = Text)
                        Text("Model: ${record.provider}/${record.model} (${record.profile}) • ${record.status} • ${record.latencyMs ?: 0}ms", color = Muted)
                        Text("Response: " + record.response, color = Text)
                        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Less" else "Effects / routing / IDs") }
                        if (expanded) {
                            Text("Requested effects: " + record.effects.toString(2), color = Text)
                            Text("Applied state changes: " + record.stateChanges.toString(2), color = Text)
                            Text("Routing: " + record.decisions.toString(2), color = Text)
                            Text("Started: ${record.startedAtMs} • completed: ${record.completedAtMs}\nCorrelation / request: ${record.correlationId}", color = Muted)
                            TextButton(onClick = { inspector = record.context }) { Text("Effective context") }
                            var showPrompt by remember { mutableStateOf(false) }
                            TextButton(onClick = { showPrompt = !showPrompt }) { Text("Protected exact request (GM spoilers)") }
                            if (showPrompt) Text(record.prompt, color = Text)
                        }
                    }
                }
            }
        }
    }
    if (chooseSession) AlertDialog(onDismissRequest = { chooseSession = false }, title = { Text("Complete sessions") },
        text = { LazyColumn { items(sessions) { id ->
            TextButton(onClick = { selected = id; chooseSession = false }) { Text(id) }
        } } }, confirmButton = { TextButton(onClick = { chooseSession = false }) { Text("Close") } })
    inspector?.let { ContextInspector(it) { inspector = null } }
}
