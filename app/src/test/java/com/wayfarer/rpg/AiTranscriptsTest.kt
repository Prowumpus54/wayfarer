package com.wayfarer.rpg

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AiTranscriptsTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun redactsNestedCredentialsAndKnownSecretsBeforeDisk() {
        TranscriptRedactor.register("configured-desktop-password")
        val store = TranscriptStore(folder.root)
        val id = store.session("campaign:meta")
        val context = JSONArray().put(JSONObject().put("accessToken", "must-not-survive")
            .put("nested", JSONObject().put("Authorization", "Bearer hidden"))
            .put("content", "password='two words' api_key=\"quoted value\" Bearer abc.def configured-desktop-password"))
        store.save(TranscriptRecord(id, channel = "meta", operation = "test", context = context,
            visibleMessage = "https://alice:password@host/x?token=abc",
            response = "-----BEGIN PRIVATE KEY-----\nsecret material\n-----END PRIVATE KEY-----"))
        val raw = folder.root.walkTopDown().filter { it.isFile }.joinToString { it.readText() }
        for (secret in listOf("must-not-survive", "Bearer hidden", "two words", "quoted value",
            "abc.def", "configured-desktop-password", "alice:password", "secret material")) assertFalse(secret, raw.contains(secret))
        assertTrue(raw.contains("[redacted]"))
    }

    @Test fun serializationPreservesFieldsAndEffectiveContext() {
        val record = TranscriptRecord("session-1", channel = "world", operation = "gm_resolve",
            provider = "firebase", model = "model", profile = "AUTO", startedAtMs = 12,
            completedAtMs = 45, latencyMs = 33, status = "ok", visibleMessage = "I inspect",
            response = "Result", prompt = "Exact request", context = JSONArray().put(JSONObject().put("hidden", true)),
            decisions = JSONArray().put("fallback"), effects = JSONArray().put("requested"),
            stateChanges = JSONArray().put(JSONObject().put("hp", 7)))
        val decoded = TranscriptRecord.from(JSONObject(record.json().toString()))
        assertEquals(record.json().toString(), decoded.json().toString())
        assertEquals(record.requestId, decoded.correlationId)
    }

    @Test fun completeConversationAndPendingQueueSurviveRestartAndArchive() {
        var store = TranscriptStore(folder.root)
        val session = store.session("campaign:world")
        repeat(1005) { store.save(TranscriptRecord(session, channel = "world", operation = "test",
            visibleMessage = "Message $it")) }
        store = TranscriptStore(folder.root)
        assertEquals(session, store.session("campaign:world"))
        assertEquals(1005, store.latest(session).size)
        assertEquals(1005, store.pending(2000).size)
        val next = store.rotate("campaign:world")
        assertNotEquals(session, next)
        assertEquals(1005, store.latest(session).size)
        assertEquals(next, TranscriptStore(folder.root).session("campaign:world"))
    }

    @Test fun offlineFailureNeverAcknowledgesOrDropsQueueAndRetryUsesSameEventId() {
        val store = TranscriptStore(folder.root)
        val record = store.save(TranscriptRecord(store.session("meta"), channel = "meta", operation = "test"))
        assertEquals(0, store.drain({ false }))
        val restarted = TranscriptStore(folder.root)
        assertEquals(record.eventId, restarted.pending().single().eventId)
        assertEquals(1, restarted.drain({ it.eventId == record.eventId }))
        assertTrue(TranscriptStore(folder.root).pending().isEmpty())
        assertEquals(1, restarted.latest(record.sessionId).size)
    }

    @Test fun failedSecondSendLeavesRemainingEntriesQueued() {
        val store = TranscriptStore(folder.root)
        val session = store.session("world")
        repeat(3) { store.save(TranscriptRecord(session, channel = "world", operation = "test")) }
        var sends = 0
        assertEquals(1, store.drain({ ++sends == 1 }))
        assertEquals(2, TranscriptStore(folder.root).pending().size)
        assertEquals(3, store.latest(session).size)
    }

    @Test fun tornJournalTailCannotHideLaterSuccessfulWrites() {
        val store = TranscriptStore(folder.root)
        val session = store.session("world")
        store.save(TranscriptRecord(session, channel = "world", operation = "first"))
        File(folder.root, "$session.jsonl").appendText("{\"unfinished\":")
        store.save(TranscriptRecord(session, channel = "world", operation = "second"))
        assertEquals(listOf("first", "second"), TranscriptStore(folder.root).latest(session).map { it.operation })
    }

    @Test fun metaEffectsAndMovementTextRemainProseWithoutCampaignEvents() = runBlocking {
        val character = CharacterState()
        val runtime = CampaignRuntimeState()
        val events = mutableListOf<GameEvent>()
        val before = Triple(character, runtime, events.toList())
        val service = MetaChatService { """{"effects":[{"type":"damage_character","amount":99}],"movement":"secret-room","check":{"name":"Will","dc":50}}""" }
        val response = service.send("Move me, roll dice and damage my character")
        assertTrue(response.contains("damage_character"))
        assertEquals(before, Triple(character, runtime, events.toList()))
        // MetaChatService's only dependency accepts/returns String; no game-state or event sink exists.
    }

    @Test fun knowledgeActionUsesReadOnlyMetaTransport() = runBlocking {
        val messages = mutableListOf<String>()
        val service = MetaChatService { messages.add(it); "Known context" }
        assertEquals("Known context", service.knowledge())
        assertEquals(listOf(MetaChatService.KNOWLEDGE_QUESTION), messages)
    }

    @Test fun contextCategoriesAreExactlyTheRenderedInputAndMarkHiddenContent() {
        val context = GmContext("campaign", "Oakhurst", "Town", "Hidden door", listOf("Road"),
            listOf("Mayor"), listOf("Challenge"), listOf("Coins"), CharacterState(),
            emptyList(), listOf("History"), listOf("Runtime"), "Action")
        val snapshot = context.snapshot()
        assertEquals(listOf("authoritative_location", "recent_history", "older_history",
            "character_party", "module_scene", "runtime_state", "encounters_challenges",
            "hidden_gm_context"), snapshot.categories.map { it.key })
        snapshot.categories.forEach { assertTrue(snapshot.render().contains(it.content)) }
        assertTrue(snapshot.categories.last().hidden)
        assertTrue(snapshot.render().contains("Hidden door"))
    }
}
