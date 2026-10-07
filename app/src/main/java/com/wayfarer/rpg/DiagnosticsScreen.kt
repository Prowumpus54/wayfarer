package com.wayfarer.rpg

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var events by remember { mutableStateOf(LoreWiseDiagnostics.events()) }
    var confirmClear by remember { mutableStateOf(false) }
    val errors = events.count { it.status == DiagnosticStatus.ERROR }
    val slow = events.count {
        (it.durationMs ?: 0) >= DiagnosticsStore.SLOW_MS
    }
    val slowest = events.maxByOrNull { it.durationMs ?: -1L }
        ?.takeIf { it.durationMs != null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Diagnostics",
            style = MaterialTheme.typography.headlineSmall,
            color = Text
        )
        Text(
            "${events.size} events • $errors errors • $slow slow operations",
            color = if (errors > 0) Danger else Muted,
            fontSize = 12.sp
        )
        slowest?.let {
            Text(
                "Slowest: ${it.category}/${it.operation} ${it.durationMs} ms",
                color = Muted,
                fontSize = 11.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { events = LoreWiseDiagnostics.events() }
            ) { Text("Refresh") }
            OutlinedButton(onClick = {
                val clipboard = context.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "LoreWise diagnostics",
                        LoreWiseDiagnostics.report()
                    )
                )
            }) { Text("Copy report") }
            OutlinedButton(
                onClick = { confirmClear = true }
            ) { Text("Clear") }
        }

        HorizontalDivider(color = GoldDark)
        if (events.isEmpty()) {
            Text("No diagnostic events recorded yet.", color = Muted)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    events.asReversed(),
                    key = {
                        "${it.timestampMs}-${it.correlationId}-${it.operation}"
                    }
                ) { event ->
                    FramedCard(Modifier.fillMaxWidth()) {
                        Text(
                            "${event.status} • ${event.category}/${event.operation}",
                            color = when (event.status) {
                                DiagnosticStatus.ERROR -> Danger
                                DiagnosticStatus.OK -> Green
                                else -> Gold
                            },
                            fontSize = 12.sp
                        )
                        Text(
                            DateFormat.getDateTimeInstance(
                                DateFormat.SHORT,
                                DateFormat.MEDIUM
                            ).format(Date(event.timestampMs)) +
                                (event.durationMs?.let {
                                    " • ${it} ms"
                                } ?: ""),
                            color = Muted,
                            fontSize = 10.sp
                        )
                        if (event.detail.isNotBlank()) {
                            Text(
                                event.detail,
                                color = Text,
                                fontSize = 11.sp
                            )
                        }
                        if (event.correlationId.isNotBlank()) {
                            Text(
                                "ID ${event.correlationId.take(8)}",
                                color = Muted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear diagnostics?") },
            text = {
                Text(
                    "This clears LoreWise's local diagnostic history only."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    LoreWiseDiagnostics.clear()
                    events = emptyList()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmClear = false }
                ) { Text("Cancel") }
            },
            containerColor = Surface
        )
    }
}