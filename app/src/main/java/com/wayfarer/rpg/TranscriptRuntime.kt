package com.wayfarer.rpg

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object LoreWiseTranscripts {
    @Volatile private var store: TranscriptStore? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile var activeScope: String = "app"
    @Volatile var persistenceError: String? = null
        private set
    @Synchronized fun initialize(context: Context) {
        if (store != null) return
        TranscriptRedactor.register(BuildConfig.LOCAL_LLM_TOKEN)
        store = TranscriptStore(File(context.applicationContext.noBackupFilesDir, "wayfarer_transcripts"))
        store?.sessions()?.forEach { id ->
            store?.latest(id)?.filter { it.status == "started" }?.forEach { record ->
                save(record.copy(eventId = UUID.randomUUID().toString(), status = "interrupted",
                    completedAtMs = System.currentTimeMillis(), decisions = JSONArray(record.decisions.toString())
                        .put(JSONObject().put("kind", "restart").put("detail", "Request interrupted before completion"))))
            }
        }
        scope.launch {
            while (isActive) {
                runCatching { uploadPending() }
                delay(30_000)
            }
        }
    }
    fun session(channel: String): String = store?.session("$activeScope:$channel") ?: UUID.randomUUID().toString()
    fun rotate(channel: String) = store?.rotate("$activeScope:$channel")
    fun sessions(): List<String> = store?.sessions().orEmpty()
    fun latest(session: String): List<TranscriptRecord> = store?.latest(session).orEmpty()
    fun pendingCount(): Int = store?.pending(Int.MAX_VALUE)?.size ?: 0
    fun save(record: TranscriptRecord) {
        runCatching { store?.save(record) }.onFailure {
            persistenceError = it::class.java.simpleName
            LoreWiseDiagnostics.record("transcript", "persist", DiagnosticStatus.ERROR,
                detail = persistenceError.orEmpty(), correlationId = record.correlationId)
        }
    }
    fun trace(operation: String, prompt: String, visible: String = prompt,
              snapshot: ModelContextSnapshot? = null, channel: String = "assistant",
              sessionId: String = session(channel)): AiTranscriptTrace =
        AiTranscriptTrace(TranscriptRecord(sessionId = sessionId, channel = channel,
            operation = operation, visibleMessage = visible, prompt = prompt,
            context = snapshot?.json() ?: JSONArray().put(JSONObject()
                .put("key", "effective_request").put("label", "Effective helper request")
                .put("content", prompt).put("supplied", true).put("hidden", false))))

    fun effects(requestId: String, requested: List<GmEffect>, application: GameStateApplication) {
        val record = find(requestId) ?: return
        save(record.copy(eventId = UUID.randomUUID().toString(),
            effects = record.effects,
            stateChanges = JSONArray(record.stateChanges.toString()).put(JSONObject()
                .put("kind", "effects_applied").put("requested", JSONArray(requested.map { effectJson(it) }))
                .put("characterHp", application.character.currentHp).put("characterXp", application.character.xp)
                .put("spellSlotsUsed", JSONObject(application.character.spellSlotsUsed.mapKeys { it.key.toString() }))
                .put("runtime", JSONArray(GameStateEngine.contextLines(application.runtime)))
                .put("events", JSONArray(application.events.map {
                JSONObject().put("eventId", it.id).put("title", it.title).put("body", it.body).put("type", it.type)
            })))))
    }
    fun stateChange(requestId: String, change: JSONObject) {
        val record = find(requestId) ?: return
        val changes = JSONArray(record.stateChanges.toString()).put(change)
        save(record.copy(eventId = UUID.randomUUID().toString(), stateChanges = changes))
    }
    private fun find(requestId: String): TranscriptRecord? = sessions().asSequence()
        .flatMap { latest(it).asSequence() }.firstOrNull { it.requestId == requestId }

    fun effectJson(effect: GmEffect): JSONObject = JSONObject().put("type", effect.type)
        .put("details", effect.toString()).put("disposition", "requested; see stateChanges for applied outcome")

    suspend fun uploadPending(): Int = withContext(Dispatchers.IO) {
        val base = BuildConfig.LOCAL_LLM_URL.trimEnd('/')
        if (base.isBlank() || BuildConfig.LOCAL_LLM_TOKEN.isBlank()) return@withContext 0
        store?.drain({ record ->
            val connection = (URL("$base/v1/transcripts").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 5000; readTimeout = 10000; doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${BuildConfig.LOCAL_LLM_TOKEN}")
            }
            try {
                val safe = TranscriptRedactor.value(record.json()) as JSONObject
                connection.outputStream.use { it.write(safe.toString().toByteArray(Charsets.UTF_8)) }
                connection.responseCode in 200..299
            } catch (_: Exception) { false } finally { connection.disconnect() }
        }) ?: 0
    }
}

class AiTranscriptTrace(initial: TranscriptRecord) {
    var record: TranscriptRecord = initial
        private set
    val id: String get() = record.requestId
    init { LoreWiseTranscripts.save(record) }
    private fun update(next: TranscriptRecord) {
        record = next.copy(eventId = UUID.randomUUID().toString())
        LoreWiseTranscripts.save(record)
    }
    fun decision(kind: String, detail: String) {
        update(record.copy(decisions = JSONArray(record.decisions.toString()).put(
            JSONObject().put("kind", kind).put("detail", detail).put("timestampMs", System.currentTimeMillis()))))
    }
    suspend fun <T> attempt(provider: String, model: String, profile: String = "", call: suspend () -> T): T {
        update(record.copy(provider = provider, model = model, profile = profile))
        decision("route", "$provider/$model/$profile")
        LoreWiseDiagnostics.record("ai", record.operation, DiagnosticStatus.INFO,
            detail = "provider=$provider model=$model", correlationId = id)
        val started = System.nanoTime()
        try {
            val result = call()
            val duration = (System.nanoTime() - started) / 1_000_000
            decision("attempt_ok", "$provider/$model latencyMs=$duration")
            LoreWiseDiagnostics.record("ai", record.operation, DiagnosticStatus.OK, duration,
                detail = "provider=$provider model=$model", correlationId = id)
            return result
        } catch (cancel: CancellationException) {
            finish("", "cancelled")
            LoreWiseDiagnostics.record("ai", record.operation, DiagnosticStatus.CANCELLED,
                correlationId = id)
            throw cancel
        } catch (error: Exception) {
            decision("attempt_error", "$provider/$model ${error::class.java.simpleName}")
            LoreWiseDiagnostics.record("ai", record.operation, DiagnosticStatus.ERROR,
                (System.nanoTime() - started) / 1_000_000,
                detail = error::class.java.simpleName, correlationId = id)
            throw error
        }
    }
    fun finish(response: String, status: String = "ok", model: String = record.model) {
        val now = System.currentTimeMillis()
        update(record.copy(response = response, status = status, model = model,
            completedAtMs = now, latencyMs = now - record.startedAtMs))
    }
    fun proposed(turn: GmTurn) {
        update(record.copy(effects = JSONArray(turn.effects.map { LoreWiseTranscripts.effectJson(it) })
            .put(JSONObject().put("check", turn.check?.toString()).put("modifiers", turn.modifiers.toString())
                .put("xpAward", turn.xpAward).put("disposition", "proposed"))))
    }
}
