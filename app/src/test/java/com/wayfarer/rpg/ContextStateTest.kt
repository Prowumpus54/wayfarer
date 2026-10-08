package com.wayfarer.rpg

import org.junit.Assert.*
import org.junit.Test

class ContextStateTest {
    private val road = ModuleDestination("road", "Old Road", "A road.")
    private fun scene(id: String = "town", name: String = "Oakhurst") = ModuleSceneContext(
        ModuleLocation(id, name, null, null, "Visible $name", "Secret $name"),
        listOf(road), listOf(ModuleNpcSummary("Local NPC", "guide", "friendly")),
        emptyList(), emptyList(), emptyList())

    @Test fun explicitMovementPersistsOnlyConnectedDestination() {
        var stored = "town"
        val proposal = LocationTransitionResolver.propose("I head to the Old Road.", scene())
        assertEquals(road, proposal.destination)
        LocationTransitionResolver.commit(proposal.destination!!, scene()) { stored = it }
        assertEquals("road", stored)
        // A stale proposal must not commit against a different origin's connections.
        val result = LocationTransitionResolver.commit(road, scene("road").copy(destinations = emptyList())) { stored = it }
        assertNull(result.destination)
        assertEquals("road", stored)
    }

    @Test fun climbMovementWithContractionResolvesConnectedDestination() {
        val ravine = ModuleDestination("ravine", "Ravine", "Climb down.")
        val live = scene().copy(destinations = listOf(ravine))
        assertEquals(ravine, LocationTransitionResolver.propose("I'll climb down the ravine.", live).destination)
        assertEquals(ravine, LocationTransitionResolver.propose("I’ll climb down the Ravine", live).destination)
        assertNull(LocationTransitionResolver.propose("What moves can I make?", live).destination)
    }

    @Test fun missingLockedAmbiguousAndBlockedRoutesDoNotPersist() {
        var calls = 0
        assertNull(LocationTransitionResolver.propose("We travel to the Citadel", scene()).destination)
        assertNull(LocationTransitionResolver.propose("I go to Old Road and attack", scene()).destination)
        assertNull(LocationTransitionResolver.propose("I ask about Old Road", scene()).destination)
        assertNull(LocationTransitionResolver.propose("I go to Old Road", null).destination)
        assertNull(LocationTransitionResolver.propose("I go to Old Road", scene().copy(destinations = listOf(road, road.copy(id = "other")))).destination)
        LocationTransitionResolver.commit(road, scene(), blocked = true) { calls++ }
        LocationTransitionResolver.commit(road, null) { calls++ }
        assertEquals(0, calls)
    }

    @Test fun recentHistoryDeduplicatesRetriesAndPreservesRollsAndOrder() {
        val older = (1..14).map { GameEvent("State", "Fact $it", "world", id = "fact$it") }
        val action = GameEvent("Player action", "I search", "action", id = "a")
        val gm = GameEvent("Game Master", "You find tracks", "gm", id = "g")
        val roll = GameEvent("Dice roll", "17 + 2 = 19", "roll", id = "r")
        val compact = HistoryCompactor.select(listOf(gm, gm.copy(id = "retry"), roll, action, action.copy(id = "retry2")) + older)
        assertEquals(8, compact.recent.size)
        assertEquals(1, compact.recent.count { it.contains("I search") })
        assertEquals(1, compact.recent.count { it.contains("You find tracks") })
        assertTrue(compact.recent.any { it.contains("17 + 2 = 19") })
        assertEquals("Game Master: You find tracks", compact.recent.last())
        assertTrue(compact.summary.contains("Recorded world:"))
        assertFalse(compact.summary.contains("You find tracks"))
    }

    @Test fun summaryIsBoundedExtractiveAndKeepsArchivedRecords() {
        val records = (1..40).map { GameEvent("Changed", "Flag $it", "world") }
        val summary = HistoryCompactor.summarize(records, "Recorded travel: Arrived at gate")
        assertTrue(summary.contains("Arrived at gate"))
        assertTrue(summary.length <= 4000)
        assertTrue(summary.lines().size <= 16)
        assertTrue(HistoryCompactor.summarize(listOf(GameEvent("GM", "A secret invented", "gm"))).startsWith("Historical gm excerpt (not authoritative):"))
    }

    private fun context(id: String, module: ModuleSceneContext?) = ContextAssembler.assemble(
        "Campaign", id, module, CharacterState(), emptyList(), CampaignRuntimeState(),
        listOf(GameEvent("GM", "We are in Oakhurst", "gm")), "Look", playerRoll = "Natural 20")

    @Test fun contextUsesSavedLocationAndRejectsStaleTownScene() {
        val invalid = context("citadel", scene())
        assertEquals("Unresolved saved location [citadel]", invalid.location)
        assertEquals("", invalid.gmNotes)
        assertTrue(invalid.npcs.isEmpty())
        val current = context("citadel", scene("citadel", "Citadel"))
        assertEquals("Citadel", current.location)
        assertEquals("Secret Citadel", current.gmNotes)
        assertFalse(current.gmNotes.contains("Oakhurst"))
        assertFalse(current.recentHistory.any { it.contains("Secret Citadel") })
        assertEquals("Natural 20", current.playerRoll)
        assertEquals(listOf("Old Road"), current.destinations)
    }

    @Test fun contextBoundsNotesAndCleansTouchedEncoding() {
        val current = context("town", scene().copy(location = scene().location.copy(gmNotes = "x".repeat(9000))))
        assertEquals(1800, current.gmNotes.length)
        assertEquals("A — B × 2 • …", ContextText.clean("A â€” B Ã— 2 â€¢ â€¦"))
    }
}
