package com.wayfarer.rpg

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class DiagnosticStatus { INFO, OK, WARN, RETRY, CANCELLED, ERROR }

data class DiagnosticEvent(
    val timestampMs: Long,
    val category: String,
    val operation: String,
    val status: DiagnosticStatus,
    val durationMs: Long? = null,
    val detail: String = "",
    val correlationId: String = ""
)

class DiagnosticsStore(context: Context, private val capacity: Int = 1000) {    private val prefs = context.applicationContext
        .getSharedPreferences("wayfarer_diagnostics", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    @Volatile private var events: List<DiagnosticEvent> = load()

    fun record(
        category: String,
        operation: String,
        status: DiagnosticStatus,
        durationMs: Long? = null,
        detail: String = "",
        correlationId: String = UUID.randomUUID().toString()
    ): String {
        val event = DiagnosticEvent(
            System.currentTimeMillis(),
            category.take(40),
            operation.take(64),
            status,
            durationMs?.coerceAtLeast(0),
            sanitize(detail),
            correlationId.take(64)
        )
        val snapshot = synchronized(lock) {
            events = (events + event).takeLast(capacity)
            events
        }
        Log.println(
            if (status == DiagnosticStatus.ERROR) Log.ERROR
            else if (status == DiagnosticStatus.WARN) Log.WARN
            else Log.INFO,
            "LoreWiseDiag",
            "${event.category}/${event.operation} ${event.status}" +
                (event.durationMs?.let { " ${it}ms" } ?: "") +
                (event.detail.takeIf { it.isNotBlank() }?.let { " • ${it}" } ?: "")
        )
        scope.launch { persist(snapshot) }
        return correlationId
    }

    fun snapshot(): List<DiagnosticEvent> =
        synchronized(lock) { events.toList() }

    fun clear() {
        synchronized(lock) { events = emptyList() }
        scope.launch { runCatching { prefs.edit().remove(KEY).apply() } }
    }

    fun report(): String = buildString {
        val rows = snapshot()
        appendLine("LoreWise diagnostics")
        appendLine("App ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Events ${rows.size}; errors ${rows.count { it.status == DiagnosticStatus.ERROR }}")
        rows.asReversed().forEach { e ->
            append(e.timestampMs).append(" ")
            append(e.status).append(" ")
            append(e.category).append("/").append(e.operation)
            e.durationMs?.let { append(" ").append(it).append("ms") }
            if (e.correlationId.isNotBlank()) append(" #").append(e.correlationId.take(8))
            if (e.detail.isNotBlank()) append(" • ").append(e.detail)
            appendLine()
        }
    }

    private fun load(): List<DiagnosticEvent> = runCatching {
        val array = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val j = array.optJSONObject(i) ?: continue
                add(
                    DiagnosticEvent(
                        j.optLong("timestampMs"),
                        j.optString("category"),
                        j.optString("operation"),
                        runCatching {
                            DiagnosticStatus.valueOf(j.optString("status"))
                        }.getOrDefault(DiagnosticStatus.INFO),
                        if (j.isNull("durationMs")) null else j.optLong("durationMs"),
                        j.optString("detail"),
                        j.optString("correlationId")
                    )
                )
            }
        }.takeLast(capacity)
    }.getOrDefault(emptyList())

    private fun persist(snapshot: List<DiagnosticEvent>) {
        runCatching {
            val array = JSONArray()
            snapshot.forEach { e ->
                array.put(
                    JSONObject()
                        .put("timestampMs", e.timestampMs)
                        .put("category", e.category)
                        .put("operation", e.operation)
                        .put("status", e.status.name)
                        .put("durationMs", e.durationMs)
                        .put("detail", e.detail)
                        .put("correlationId", e.correlationId)
                )
            }
            prefs.edit().putString(KEY, array.toString()).apply()
        }.onFailure {
            Log.w("LoreWiseDiag", "Diagnostic persistence failed: ${it::class.java.simpleName}")
        }
    }

    companion object {
        const val SLOW_MS = 1000L
        private const val KEY = "events_v1"
        private fun sanitize(raw: String): String {
            var value = raw.replace(Regex("(?i)Bearer\\s+\\S+"), "Bearer [redacted]")
            value = value.replace(
                Regex("(?i)(token|api[_-]?key|password|secret)\\s*[:=]\\s*[^\\s,;]+"),
                "$1=[redacted]"
            )
            return value.replace(Regex("[\\r\\n]+"), " ").take(300)
        }
    }
}

object LoreWiseDiagnostics {
    @Volatile private var store: DiagnosticsStore? = null

    fun initialize(context: Context) {
        if (store == null) synchronized(this) {
            if (store == null) store = DiagnosticsStore(context)
        }
    }

    fun record(
        category: String,
        operation: String,
        status: DiagnosticStatus,
        durationMs: Long? = null,
        detail: String = "",
        correlationId: String = UUID.randomUUID().toString()
    ): String = store?.record(
        category, operation, status, durationMs, detail, correlationId
    ) ?: correlationId

    fun error(
        category: String,
        operation: String,
        error: Throwable,
        durationMs: Long? = null,
        correlationId: String = UUID.randomUUID().toString()
    ) = record(
        category, operation, DiagnosticStatus.ERROR, durationMs,
        "${error::class.java.simpleName}: ${error.message.orEmpty()}",
        correlationId
    )

    fun events(): List<DiagnosticEvent> = store?.snapshot().orEmpty()
    fun clear() = store?.clear()
    fun report(): String =
        store?.report() ?: "LoreWise diagnostics are not initialized."
}