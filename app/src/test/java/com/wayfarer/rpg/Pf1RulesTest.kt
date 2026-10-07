package com.wayfarer.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Pf1RulesTest {
    @Test
    fun rangerLevelThreeUsesPf1BabSavesSkillsAndXp() {
        val ranger = CharacterState(
            ruleset = GameRuleset.PF1E.wireName,
            level = 3,
            xp = 5_000,
            className = "Ranger",
            pf1ClassLevels = mapOf("Ranger" to 3),
            abilities = mapOf(
                Ability.STR to 14,
                Ability.DEX to 18,
                Ability.CON to 14,
                Ability.INT to 12,
                Ability.WIS to 14,
                Ability.CHA to 10
            ),
            pf1SkillRanks = mapOf("Survival" to 3),
            armorName = "Leather Armor"
        )

        assertEquals(3, ranger.baseAttackBonus())
        assertEquals(5, ranger.fortitudeSave())
        assertEquals(7, ranger.reflexSave())
        assertEquals(3, ranger.willSave())
        assertEquals(8, ranger.skillBonus(skillDefinitions.first { it.name == "Survival" }))
        assertEquals(16, ranger.ac())
        assertEquals(14, ranger.touchAc())
        assertEquals(12, ranger.flatFootedAc())
        assertEquals(5, ranger.cmb())
        assertEquals(19, ranger.cmd())
        assertEquals(9_000, pf1XpThreshold(4, Pf1ExperienceTrack.MEDIUM))
    }

    @Test
    fun fullBabCreatesIterativeAttacksAtSix() {
        assertEquals(
            listOf(6, 1),
            pf1IterativeAttackBonuses(
                pf1BaseAttackBonus(mapOf("Fighter" to 6))
            )
        )
        assertEquals(6, pf1BaseAttackBonus(mapOf("Rogue" to 8)))
    }

    @Test
    fun throwableDaggerStillUsesStrengthInMelee() {
        val character = CharacterState(
            ruleset = GameRuleset.PF1E.wireName,
            className = "Fighter",
            level = 1,
            pf1ClassLevels = mapOf("Fighter" to 1),
            abilities = CharacterState().abilities +
                (Ability.STR to 16) + (Ability.DEX to 12)
        )
        val dagger = weaponCatalog.first { it.name == "Dagger" }
        assertEquals(4, character.attackBonus(dagger))
        assertEquals(3, character.damageBonus(dagger))
    }

    @Test
    fun pf1ChecksUseBinarySuccessAndNaturalSaveRules() {
        val skill = DiceEngine.pf1Check(
            modifier = 20,
            dc = 15,
            automaticOnNatural = false,
            dieRoller = { 1 }
        )
        assertEquals(Degree.SUCCESS, skill.degree)

        val save = DiceEngine.pf1Check(
            modifier = 20,
            dc = 15,
            automaticOnNatural = true,
            dieRoller = { 1 }
        )
        assertEquals(Degree.FAILURE, save.degree)

        val naturalTwenty = DiceEngine.pf1Check(
            modifier = -20,
            dc = 50,
            automaticOnNatural = true,
            dieRoller = { 20 }
        )
        assertEquals(Degree.SUCCESS, naturalTwenty.degree)
    }

    @Test
    fun coreCasterSpellSlotsFollowPf1TablesAndAbilityBonuses() {
        assertEquals(
            mapOf(0 to 3, 1 to 2),
            pf1SpellSlotsForClass("Wizard", 1, 12)
        )
        assertEquals(
            mapOf(1 to 4),
            pf1SpellSlotsForClass("Sorcerer", 1, 16)
        )
        assertEquals(
            mapOf(1 to 2),
            pf1SpellSlotsForClass("Bard", 1, 16)
        )
        assertEquals(
            mapOf(1 to 1),
            pf1SpellSlotsForClass("Paladin", 4, 14)
        )
        assertEquals(
            mapOf(1 to 1),
            pf1SpellSlotsForClass("Ranger", 4, 14)
        )
        assertEquals(
            mapOf(1 to 1),
            pf1DomainSpellSlots("Cleric", 1)
        )
    }

    @Test
    fun spontaneousKnownLimitsAreIndependentOfAbilityScore() {
        assertEquals(
            mapOf(0 to 4, 1 to 2),
            pf1SpellsKnownLimit("Sorcerer", 1)
        )
        assertEquals(
            mapOf(0 to 6, 1 to 4, 2 to 2),
            pf1SpellsKnownLimit("Bard", 4)
        )
    }

    @Test
    fun legacyPf2StarterMigratesWithoutPf2DefaultFeats() {
        val legacy = CharacterState(
            ruleset = GameRuleset.PF2E_ADAPTED.wireName,
            pf1SchemaVersion = 0,
            level = 3,
            xp = 0,
            className = "Ranger",
            classFeats = listOf("Hunted Shot"),
            ancestryFeats = listOf("Natural Ambition"),
            skillFeats = listOf("Assurance (Survival)"),
            skillProfs = skillDefinitions.associate { definition ->
                definition.name to if (definition.name == "Stealth") {
                    Proficiency.TRAINED
                } else {
                    Proficiency.UNTRAINED
                }
            }
        )

        val migrated = Pf1CharacterMigration.migrate(legacy)
        assertTrue(migrated.isPf1())
        assertEquals(5_000, migrated.xp)
        assertEquals(3, migrated.pf1ClassLevels["Ranger"])
        assertEquals(3, migrated.pf1SkillRanks["Stealth"])
        assertTrue("Rapid Shot" in migrated.classFeats)
        assertTrue("Endurance" in migrated.bonusFeats)
        assertTrue(migrated.ancestryFeats.isEmpty())
        assertTrue(migrated.skillFeats.isEmpty())
        assertFalse("Hunted Shot" in migrated.classFeats)
    }

    @Test
    fun pf1NegativeHpUsesConstitutionDeathThreshold() {
        val character = CharacterState(
            ruleset = GameRuleset.PF1E.wireName,
            currentHp = -5,
            abilities = CharacterState().abilities + (Ability.CON to 14)
        )
        assertEquals(-14, character.pf1DeathThreshold())
        assertEquals("Dying", character.pf1HpState())
        assertEquals(
            "Dead",
            character.copy(currentHp = -14, pf1Dead = true).pf1HpState()
        )
    }
}
