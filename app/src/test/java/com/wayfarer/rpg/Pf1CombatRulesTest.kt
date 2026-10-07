package com.wayfarer.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Pf1CombatRulesTest {
    private fun fighter(level: Int = 6): CharacterState =
        CharacterState(
            ruleset = GameRuleset.PF1E.wireName,
            characterName = "Fighter",
            className = "Fighter",
            level = level,
            pf1ClassLevels = mapOf("Fighter" to level),
            abilities = CharacterState().abilities +
                (Ability.STR to 16) + (Ability.DEX to 14),
            meleeWeapon = "Longsword"
        )

    private fun target(ac: Int = 16): EncounterCreatureState =
        EncounterCreatureState(
            id = "goblin",
            name = "Goblin",
            ruleRef = "pf1:creature:goblin",
            maxHp = 20,
            currentHp = 20,
            armorClass = ac,
            touchArmorClass = 13,
            flatFootedArmorClass = 14,
            cmb = 0,
            cmd = 12,
            initiativeBonus = 6,
            attackName = "Short Sword",
            attackBonus = 2,
            damageDice = "1d4",
            damageType = "piercing",
            criticalThreatMin = 19,
            criticalMultiplier = 2,
            xpValue = 135,
            statsResolved = true
        )

    private fun runtime(creature: EncounterCreatureState): CampaignRuntimeState =
        CampaignRuntimeState(
            activeEncounter = EncounterState(
                name = "Fight",
                location = "Room",
                creatures = listOf(creature),
                initiativeOrder = listOf(
                    CombatTurnEntry(
                        CombatActorType.PLAYER,
                        "player",
                        "Fighter",
                        20,
                        2
                    ),
                    CombatTurnEntry(
                        CombatActorType.CREATURE,
                        creature.id,
                        creature.name,
                        10,
                        6
                    )
                )
            )
        )

    @Test
    fun longswordThreatRequiresConfirmationAndMultipliesDamage() {
        val dice = ArrayDeque(listOf(19, 10))
        val result = CombatRulesEngine.resolvePlayerStrike(
            character = fighter(),
            runtime = runtime(target()),
            selectedTargetId = "goblin",
            actionText = "I attack with my Longsword.",
            ruleset = GameRuleset.PF1E.wireName,
            d20Roller = { modifier, dc ->
                val die = dice.removeFirst()
                CheckResult(
                    die,
                    modifier,
                    die + modifier,
                    dc,
                    if (die + modifier >= dc) Degree.SUCCESS else Degree.FAILURE
                )
            },
            damageRoller = {
                if (it.contains("d8")) {
                    DiceRollResult(8, 1, listOf(4), 3, 7)
                } else null
            }
        )

        assertEquals(CombatResolutionStatus.RESOLVED, result.status)
        assertTrue(result.summary.contains("threat confirmation"))
        assertTrue(result.summary.contains("confirmed"))
        assertEquals(
            14,
            result.effects.first { it.type == "damage_creature" }.amount
        )
    }

    @Test
    fun naturalOneMissesEvenWhenTotalWouldHit() {
        val result = CombatRulesEngine.resolvePlayerStrike(
            character = fighter(),
            runtime = runtime(target(ac = 5)),
            selectedTargetId = "goblin",
            actionText = "I attack.",
            ruleset = GameRuleset.PF1E.wireName,
            d20Roller = { modifier, dc ->
                CheckResult(1, modifier, 1 + modifier, dc, Degree.SUCCESS)
            }
        )
        assertTrue(result.summary.contains("miss"))
        assertTrue(result.effects.none { it.type == "damage_creature" })
    }

    @Test
    fun fullAttackUsesIterativeBonuses() {
        val dice = ArrayDeque(listOf(10, 15))
        val result = CombatRulesEngine.resolvePlayerStrike(
            character = fighter(),
            runtime = runtime(target(ac = 16)),
            selectedTargetId = "goblin",
            actionText = "I make a Full Attack with my Longsword.",
            ruleset = GameRuleset.PF1E.wireName,
            d20Roller = { modifier, dc ->
                val die = dice.removeFirst()
                CheckResult(
                    die,
                    modifier,
                    die + modifier,
                    dc,
                    if (die + modifier >= dc) Degree.SUCCESS else Degree.FAILURE
                )
            },
            damageRoller = {
                DiceRollResult(8, 1, listOf(4), 3, 7)
            }
        )
        assertTrue(result.summary.contains("Attack 1"))
        assertTrue(result.summary.contains("Attack 2"))
        assertEquals(
            14,
            result.effects.first { it.type == "damage_creature" }.amount
        )
    }

    @Test
    fun grappleUsesCmbAgainstCmd() {
        val result = CombatRulesEngine.resolvePlayerManeuver(
            character = fighter(level = 3),
            runtime = runtime(target()),
            selectedTargetId = "goblin",
            actionText = "I grapple the goblin.",
            ruleset = GameRuleset.PF1E.wireName,
            d20Roller = { modifier, dc ->
                CheckResult(12, modifier, 12 + modifier, dc, Degree.SUCCESS)
            }
        )
        assertEquals(CombatResolutionStatus.RESOLVED, result.status)
        assertTrue(
            result.effects.any {
                it.type == "apply_condition" && it.condition == "grappled"
            }
        )
    }
}
