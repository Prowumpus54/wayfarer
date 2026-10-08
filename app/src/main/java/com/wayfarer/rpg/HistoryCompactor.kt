package com.wayfarer.rpg

data class CompactedHistory(val recent: List<String>, val summary: String)

/** Extractive only: narration is never promoted to authoritative world state. */
object HistoryCompactor {
    private val conversational = setOf("action", "roll", "gm", "travel", "state", "encounter",
        "loot", "challenge", "resource", "world", "xp")
    private val durable = conversational - setOf("action", "gm", "roll")
    private fun key(event: GameEvent) = event.type + ":" + ContextText.clean(event.body)
        .lowercase().replace(Regex("\\s+"), " ").trim()

    fun select(eventsNewestFirst: List<GameEvent>, archivedSummary: String = ""): CompactedHistory {
        val useful = eventsNewestFirst.filter { it.type in conversational && it.body.isNotBlank() }
        val seenIds = hashSetOf<String>()
        val selected = mutableListOf<GameEvent>()
        for (event in useful) {
            if (!seenIds.add(event.id)) continue
            if (selected.takeLast(4).any { key(it) == key(event) }) continue
            selected += event
            if (selected.size == 8) break
        }
        val recentIds = selected.map { it.id }.toSet()
        val oldestSelected = useful.indexOfLast { it.id in recentIds }
        val older = if (oldestSelected < 0) useful else useful.drop(oldestSelected + 1)
        return CompactedHistory(selected.asReversed().map {
            ContextText.clean(it.title + ": " + it.body).take(1200)
        }, summarize(older, archivedSummary))
    }

    fun summarize(eventsNewestFirst: List<GameEvent>, previous: String = ""): String {
        val recorded = eventsNewestFirst.filter { it.type in durable }
            .distinctBy(::key).take(12).asReversed()
            .map { "Recorded ${it.type}: " + ContextText.clean(it.title + ": " + it.body).take(240) }
        val dialogue = eventsNewestFirst.filter { it.type == "action" || it.type == "gm" }
            .distinctBy(::key).take(4).asReversed().map {
                "Historical ${it.type} excerpt (not authoritative): " + ContextText.clean(it.body).take(180)
            }
        return (previous.lines().filter { it.isNotBlank() } + recorded + dialogue)
            .distinct().takeLast(16).joinToString("\n").takeLast(4000)
    }
}

object ContextText {
    fun clean(text: String): String = text
        .replace("â€”", "—").replace("â€“", "–").replace("â€¢", "•")
        .replace("â€¦", "…").replace("Ã—", "×").replace("â€™", "’")
        .replace("\u0000", "").replace("\r\n", "\n").trim()
}
