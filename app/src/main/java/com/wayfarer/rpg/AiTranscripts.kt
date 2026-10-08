package com.wayfarer.rpg

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Redaction runs recursively at the persistence boundary, never just in the UI. */
object TranscriptRedactor {
    private val secretKey = Regex("(?i).*(authorization|credential|password|secret|token|api[_-]?key|private[_-]?key).*")
    private val knownSecrets = mutableSetOf<String>()
    @Synchronized fun register(secret: String) { if (secret.isNotBlank()) knownSecrets.add(secret) }
    @Synchronized fun text(raw: String): String {
        var value = raw
        knownSecrets.sortedByDescending { it.length }.forEach { value = value.replace(it, "[redacted]") }
        value = value.replace(Regex("(?i)Bearer\\s+[^\\s\\\"<>]+"), "Bearer [redacted]")
        value = value.replace(Regex("(?i)([\\\"']?(?:token|api[_-]?key|password|secret|authorization|credential)[\\\"']?\\s*[:=]\\s*)(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;}&]+)"), "$1[redacted]")
        value = value.replace(Regex("(?i)https?://[^/\\s:@]+:[^/\\s@]+@"), "https://[redacted]@")
        value = value.replace(Regex("-----BEGIN [^-]*PRIVATE KEY-----[\\s\\S]*?-----END [^-]*PRIVATE KEY-----"), "[redacted]")
        return value.replace(Regex("(?:AIza[0-9A-Za-z_-]{20,}|sk-(?:proj-)?[0-9A-Za-z_-]{20,}|eyJ[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+)"), "[redacted]")
    }
    fun value(raw: Any?): Any? = when (raw) {
        is JSONObject -> JSONObject().also { safe ->
            raw.keys().forEach { key -> safe.put(key, if (secretKey.matches(key)) "[redacted]" else value(raw.opt(key))) }
        }
        is JSONArray -> JSONArray().also { safe -> for (i in 0 until raw.length()) safe.put(value(raw.opt(i))) }
        is String -> text(raw)
        else -> raw
    }
}

data class ContextCategory(val key: String, val label: String, val content: String, val hidden: Boolean = false)
data class ModelContextSnapshot(val categories: List<ContextCategory>) {
    fun json(): JSONArray = JSONArray().also { array ->
        categories.forEach { array.put(JSONObject().put("key", it.key).put("label", it.label)
            .put("content", it.content).put("hidden", it.hidden).put("supplied", it.content.isNotBlank())) }
    }
    fun render(): String = categories.joinToString("\n\n") { it.label.uppercase() + ":\n" + it.content }
}

data class TranscriptRecord(
    val sessionId: String,
    val requestId: String = UUID.randomUUID().toString(),
    val eventId: String = UUID.randomUUID().toString(),
    val correlationId: String = requestId,
    val channel: String,
    val operation: String,
    val provider: String = "",
    val model: String = "",
    val profile: String = "",
    val startedAtMs: Long = System.currentTimeMillis(),
    val completedAtMs: Long? = null,
    val latencyMs: Long? = null,
    val status: String = "started",
    val visibleMessage: String = "",
    val response: String = "",
    val prompt: String = "",
    val context: JSONArray = JSONArray(),
    val decisions: JSONArray = JSONArray(),
    val effects: JSONArray = JSONArray(),
    val stateChanges: JSONArray = JSONArray()
) {
    fun json(): JSONObject = JSONObject().put("schemaVersion", 1).put("sessionId", sessionId)
        .put("conversationId", sessionId).put("requestId", requestId).put("messageId", requestId)
        .put("eventId", eventId).put("correlationId", correlationId).put("channel", channel)
        .put("operation", operation).put("provider", provider).put("model", model).put("profile", profile)
        .put("startedAtMs", startedAtMs).put("completedAtMs", completedAtMs).put("latencyMs", latencyMs)
        .put("status", status).put("visibleMessage", visibleMessage).put("response", response)
        .put("prompt", prompt).put("contextCategories", context).put("decisions", decisions)
        .put("effects", effects).put("stateChanges", stateChanges)
    companion object {
        fun from(j: JSONObject) = TranscriptRecord(
            sessionId = j.getString("sessionId"), requestId = j.getString("requestId"),
            eventId = j.getString("eventId"), correlationId = j.getString("correlationId"),
            channel = j.getString("channel"), operation = j.getString("operation"),
            provider = j.optString("provider"), model = j.optString("model"), profile = j.optString("profile"),
            startedAtMs = j.getLong("startedAtMs"),
            completedAtMs = if (j.isNull("completedAtMs")) null else j.getLong("completedAtMs"),
            latencyMs = if (j.isNull("latencyMs")) null else j.getLong("latencyMs"),
            status = j.getString("status"), visibleMessage = j.optString("visibleMessage"),
            response = j.optString("response"), prompt = j.optString("prompt"),
            context = j.optJSONArray("contextCategories") ?: JSONArray(),
            decisions = j.optJSONArray("decisions") ?: JSONArray(),
            effects = j.optJSONArray("effects") ?: JSONArray(),
            stateChanges = j.optJSONArray("stateChanges") ?: JSONArray()
        )
    }
}

/** Append-only session journals. Ack journals are separate: offline entries are never evicted. */
class TranscriptStore(private val root: File) {
    init { root.mkdirs() }
    private fun file(id: String, suffix: String): File {
        require(id.matches(Regex("[a-zA-Z0-9-]{1,80}")))
        return File(root, id + suffix)
    }
    @Synchronized fun session(scope: String): String {
        val key = UUID.nameUUIDFromBytes(scope.toByteArray(Charsets.UTF_8)).toString()
        val active = file(key, ".active")
        return if (active.exists()) active.readText().trim() else rotate(scope)
    }
    @Synchronized fun rotate(scope: String): String {
        val key = UUID.nameUUIDFromBytes(scope.toByteArray(Charsets.UTF_8)).toString()
        val id = UUID.randomUUID().toString()
        val active = file(key, ".active")
        val temp = file(key, ".tmp")
        FileOutputStream(temp).use { it.write(id.toByteArray()); it.fd.sync() }
        // Prefer same-directory atomic replacement, but older/provider filesystems may reject it.
        try {
            java.nio.file.Files.move(
                temp.toPath(), active.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            java.nio.file.Files.move(
                temp.toPath(), active.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
        }
        return id
    }
    private fun append(file: File, line: String) {
        FileOutputStream(file, true).use {
            // Newline first also isolates a torn final write after a process crash.
            it.write(("\n" + line + "\n").toByteArray(Charsets.UTF_8)); it.fd.sync()
        }
    }
    @Synchronized fun save(record: TranscriptRecord): TranscriptRecord {
        val safe = TranscriptRecord.from(TranscriptRedactor.value(record.json()) as JSONObject)
        append(file(safe.sessionId, ".jsonl"), safe.json().toString())
        return safe
    }
    @Synchronized fun sessions(): List<String> = root.listFiles().orEmpty()
        .filter { it.extension == "jsonl" }.sortedByDescending { it.lastModified() }.map { it.nameWithoutExtension }
    @Synchronized fun records(session: String): List<TranscriptRecord> {
        val source = file(session, ".jsonl")
        if (!source.exists()) return emptyList()
        return source.useLines { lines -> lines.mapNotNull { line ->
            runCatching { TranscriptRecord.from(JSONObject(line)) }.getOrNull()
        }.toList() }
    }
    @Synchronized fun latest(session: String): List<TranscriptRecord> =
        records(session).associateBy { it.requestId }.values.sortedBy { it.startedAtMs }
    @Synchronized fun pending(limit: Int = 50): List<TranscriptRecord> = buildList {
        for (session in sessions().asReversed()) {
            val ack = file(session, ".ack")
            val sent = if (ack.exists()) ack.readLines().toHashSet() else emptySet()
            for (record in records(session)) if (record.eventId !in sent) {
                add(record)
                if (size >= limit) return@buildList
            }
        }
    }
    @Synchronized fun acknowledge(record: TranscriptRecord) =
        append(file(record.sessionId, ".ack"), record.eventId)

    /** Ack only after success. A lost ack replays the same event ID safely to the mirror. */
    fun drain(send: (TranscriptRecord) -> Boolean, limit: Int = 50): Int {
        var delivered = 0
        for (record in pending(limit)) {
            if (!send(record)) break
            acknowledge(record)
            delivered++
        }
        return delivered
    }
}

/** Meta responses are prose only; no GmTurn/effect parsing or game-event callback is available. */
class MetaChatService(private val ask: suspend (String) -> String) {
    suspend fun send(question: String): String = ask(question)
    suspend fun knowledge(): String = send(KNOWLEDGE_QUESTION)
    companion object { const val KNOWLEDGE_QUESTION = "What do you currently know? Summarize your working context by category, distinguish supplied facts from inference and missing information." }
}
