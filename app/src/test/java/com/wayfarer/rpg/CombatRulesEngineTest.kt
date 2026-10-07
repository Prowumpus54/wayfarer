package com.wayfarer.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatRulesEngineTest {
    private val goblin = EncounterCreatureState(
        id = "g1",
        name = "Goblin Warrior",
        ruleRef = "creature:goblin-warrior",
        maxHp = 6,
        currentHp = 6,
        armorClass = 16,
        initiativeBonus = 2,
        attackName = "Dogslicer",
        attackBonus = 7,
        damageDice = "1d6",
        damageType = "slashing",
        xpValue = 20,
        statsResolved = true
    )

    @Test
    fun strikeRequiresInitiativeBeforeRollingAttack() {
        val result = CombatRulesEngine.resolvePlayerStrike(
            character = CharacterState(),
            runtime = CampaignRuntimeState(
                activeEncounter = EncounterState(
                    name = "Ambush",
                    location = "Hall",
                    creatures = listOf(goblin)
                )
            ),
            selectedTargetId = goblin.id,
            actionText = "I strike the goblin.",
            ruleset = "pf2e-adapted"
        )

        assertEquals(CombatResolutionStatus.NEED_INITIATIVE, result.status)
        assertEquals(GmEffectType.START_INITIATIVE.wireName, result.effects.single().type)
    }

    @Test
    fun selectedResolvedTargetUsesLocalAttackAndDamage() {
        val character = CharacterState(characterName = "Thorne")
        val encounter = EncounterState(
            name = "Ambush",
            location = "Hall",
            creatures = listOf(goblin),
            initiativeOrder = listOf(
                CombatTurnEntry(
                    CombatActorType.PLAYER,
                    "player",
                    character.characterName,
                    20,
                    6
                ),
                CombatTurnEntry(
                    CombatActorType.CREATURE,
                    goblin.id,
                    goblin.name,
                    10,
                    2
                )
            )
        )

        val result = CombatRulesEngine.resolvePlayerStrike(
            character = character,
            runtime = CampaignRuntimeState(activeEncounter = encounter),
            selectedTargetId = goblin.id,
            actionText = "I strike with my Longsword.",
            ruleset = "pf2e-adapted",
            d20Roller = { modifier, dc ->
                CheckResult(12, modifier, 18, dc, Degree.SUCCESS)
            },
            damageRoller = {
                DiceRollResult(8, 1, listOf(5), 1, 6)
            }
        )

        assertEquals(CombatResolutionStatus.RESOLVED, result.status)
        assertTrue(result.summary.contains("vs Goblin Warrior AC 16"))
        assertEquals(6, result.effects.first { it.type == "damage_creature" }.amount)
        assertEquals(GmEffectType.ADVANCE_TURN.wireName, result.effects.last().type)
    }

    @Test
    fun creatureTurnLocallyDamagesPlayerAndAdvances() {
        val character = CharacterState(
            characterName = "Thorne",
            maxHp = 20,
            currentHp = 20
        )
        val encounter = EncounterState(
            name = "Ambush",
            location = "Hall",
            creatures = listOf(goblin),
            initiativeOrder = listOf(
                CombatTurnEntry(
                    CombatActorType.CREATURE,
                    goblin.id,
                    goblin.name,
                    20,
                    2
                ),
                CombatTurnEntry(
                    CombatActorType.PLAYER,
                    "player",
                    character.characterName,
                    10,
                    6
                )
            )
        )

        val result = CombatRulesEngine.resolveCreatureStrike(
            character = character,
            encounter = encounter,
            creatureNameOrId = goblin.id,
            ruleset = "pf2e-adapted",
            d20Roller = { modifier, dc ->
                CheckResult(13, modifier, 20, dc, Degree.SUCCESS)
            },
            damageRoller = {
                DiceRollResult(6, 1, listOf(4), 0, 4)
            }
        )

        assertEquals(16, result.character.currentHp)
        assertEquals("player", result.encounter.initiativeOrder[result.encounter.currentTurnIndex].actorId)
        assertTrue(result.summary.contains("Dogslicer"))
    }

    @Test
    fun unresolvedCreatureCannotBeUsedForAuthoritativeStrike() {
        val unresolved = goblin.copy(statsResolved = false)
        val character = CharacterState()
        val encounter = EncounterState(
            name = "Ambush",
            location = "Hall",
            creatures = listOf(unresolved),
            initiativeOrder = listOf(
                CombatTurnEntry(
                    CombatActorType.PLAYER,
                    "player",
                    character.characterName,
                    20,
                    6
                )
            )
        )

        val result = CombatRulesEngine.resolvePlayerStrike(
            character = character,
            runtime = CampaignRuntimeState(activeEncounter = encounter),
            selectedTargetId = unresolved.id,
            actionText = "I strike.",
            ruleset = "pf2e-adapted"
        )

        assertEquals(CombatResolutionStatus.NEED_VALIDATED_STATS, result.status)
    }
}
