package com.wayfarer.rpg

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MechanicsAssistantTest {
    private fun ranger() = CharacterState(className = "Ranger", level = 4,
        pf1ClassLevels = mapOf("Ranger" to 4), spells = mapOf(1 to listOf("Entangle", "Longstrider")),
        spellSlots = mapOf(1 to 2), spellSlotsUsed = mapOf(1 to 1))

    @Test fun rangerSpellQuestionBypassesNarrativeGenerator() = runBlocking {
        val context = GmContext("test", "", "", "", emptyList(), emptyList(), emptyList(),
            emptyList(), ranger(), emptyList(), emptyList(), emptyList(), "what spells can I cast?")
        val turn = GeminiGameMaster(GmModelChoice.LOCAL).adjudicate(context)
        assertEquals("Android PF1 mechanics", turn.modelName)
        assertTrue(turn.narration.contains("Entangle"))
        assertTrue(turn.narration.contains("remaining=1, total=2, used=1"))
        assertNull(turn.check)
        assertTrue(turn.effects.isEmpty())
        assertEquals(0, turn.xpAward)
    }

    @Test fun thirdLevelRangerDoesNotInventSpellsOrSlots() {
        val answer = MechanicsAssistant.answer(CharacterState(), "what spells can I cast?")!!
        assertTrue(answer.contains("none recorded"))
        assertTrue(answer.contains("begins at class level 4"))
        assertFalse(answer.contains("Entangle"))
    }

    @Test fun depletedSpellsAreBlockedAndNoStateMutates() {
        val c = ranger().copy(spellSlotsUsed = mapOf(1 to 2))
        assertEquals(false, MechanicsAssistant.previewSpell(c, "Entangle").legal)
        assertEquals(false, MechanicsAssistant.previewSpell(c, "invented spell").legal)
        assertEquals(mapOf(1 to 2), c.spellSlotsUsed)
        assertTrue(MechanicsAssistant.spellSummary(c).contains("remaining=0"))
    }

    @Test fun spellPreviewLeavesUnknownTargetingConditional() {
        val preview = MechanicsAssistant.previewSpell(ranger(), "entangle")
        assertNull(preview.legal)
        assertEquals(mapOf("Spell slot L1" to 1), preview.resourceCosts)
        assertFalse(preview.notes.isEmpty())
        assertEquals(false, MechanicsAssistant.previewSpell(ranger().copy(level = 3,
            pf1ClassLevels = mapOf("Ranger" to 3)), "Entangle").legal)
        assertEquals(false, MechanicsAssistant.previewSpell(ranger().copy(
            abilities = ranger().abilities + (Ability.WIS to 10)), "Entangle").legal)
    }

    @Test fun missingSpellDetailsAndCitationsAreExplicit() {
        val answer = MechanicsAssistant.answer(ranger(), "What spells and spell range do I have?")!!
        assertTrue(answer.contains("Targeting/range/save type/duration"))
        assertTrue(answer.contains("unavailable in local PF1 content"))
        assertTrue(answer.contains("Pf1Rules.kt"))
        assertTrue(answer.contains("citation/source unavailable"))
        assertFalse(answer.contains("wayfarer_rules.sqlite"))
    }

    @Test fun combatQuestionsUseExactSheetMath() {
        val c = ranger()
        val answer = MechanicsAssistant.answer(c, "What are my AC, saves, attacks and Survival skill?")!!
        assertTrue(answer.contains("AC=${c.ac()}; touch=${c.touchAc()}; flat-footed=${c.flatFootedAc()}"))
        assertTrue(answer.contains("Fortitude=${c.fortitudeSave()}"))
        assertTrue(answer.contains("Survival=${c.skillBonus(skillDefinitions.first { it.name == "Survival" })}"))
    }

    @Test fun fullAttackAndPowerAttackPreviewUseRulesEngine() {
        val c = ranger().copy(className = "Fighter", level = 6, pf1ClassLevels = mapOf("Fighter" to 6),
            generalFeats = listOf("Power Attack"))
        val w = weaponCatalog.first { it.name == c.meleeWeapon }
        assertEquals(c.iterativeAttackBonuses(w).map { it - 2 },
            MechanicsAssistant.previewAttack(c, w.name, fullAttack = true, powerAttack = true).bonuses)
        assertEquals(false, MechanicsAssistant.previewAttack(c, w.name, fullAttack = true, moved = true).legal)
        assertEquals(false, MechanicsAssistant.previewAttack(c.copy(generalFeats = emptyList()), w.name, powerAttack = true).legal)
        assertEquals(false, MechanicsAssistant.previewAttack(c, "Unknown weapon").legal)
        assertNull(MechanicsAssistant.previewManeuver(c).legal)
        assertEquals(listOf(c.cmb()), MechanicsAssistant.previewManeuver(c).bonuses)
    }

    @Test fun contextContainsAllStoredMechanicsWithoutTruncation() {
        val c = ranger().copy(spells = mapOf(1 to (1..50).map { "Spell$it" }),
            spellSlotsUsed = mapOf(1 to 2), conditions = "fatigued", tempHp = 3,
            currencyPp = 8, bonusFeats = listOf("Endurance"),
            inventory = (1..50).map { InventoryItem("Item$it", "Other", it, 0.1, "", "detail$it", "mechanic$it") })
        val context = MechanicsAssistant.context(c)
        listOf("Spell50", "Item50", "mechanic50", "remaining=0", "temp=3", "fatigued", "pp=8",
            "Endurance", "Skills:", "Saves:", "touch=", "class resource usage unavailable").forEach {
            assertTrue("Missing $it", context.contains(it))
        }
    }

    @Test fun narrativeCastAndAttackActionsAreNotIntercepted() {
        assertNull(MechanicsAssistant.answer(ranger(), "I cast Entangle at the goblins"))
        assertNull(MechanicsAssistant.answer(ranger(), "I attack the goblin"))
        assertNull(MechanicsAssistant.answer(ranger(), "What do the goblins look like?"))
    }

    @Test fun preparedCountsAndLegacyResourcesAreNotInvented() {
        val c = ranger().copy(spells = mapOf(-2 to listOf("Legacy Focus")))
        assertEquals(false, MechanicsAssistant.previewSpell(c, "Legacy Focus").legal)
        assertTrue(MechanicsAssistant.context(c).contains("individual preparation counts unavailable"))
        assertTrue(MechanicsAssistant.answer(c.copy(ruleset = GameRuleset.PF2E_ADAPTED.wireName), "What spells can I cast?")!!.contains("PF1 mechanics unavailable"))
    }
}
