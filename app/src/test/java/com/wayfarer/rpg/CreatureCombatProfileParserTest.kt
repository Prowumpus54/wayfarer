package com.wayfarer.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CreatureCombatProfileParserTest {
    @Test
    fun parsesCreatureCombatStatsAndPrimaryAttack() {
        val raw = """
            {
              "name":"Goblin Warrior",
              "type":"npc",
              "system":{
                "attributes":{
                  "ac":{"value":16},
                  "hp":{"max":6,"value":6}
                },
                "details":{"level":{"value":-1}},
                "initiative":{"statistic":"perception"},
                "perception":{"mod":2},
                "saves":{
                  "fortitude":{"value":5},
                  "reflex":{"value":7},
                  "will":{"value":3}
                }
              },
              "items":[
                {
                  "name":"Dogslicer",
                  "type":"melee",
                  "system":{
                    "bonus":{"value":7},
                    "damageRolls":{
                      "one":{"damage":"1d6","damageType":"slashing"}
                    },
                    "range":null
                  }
                }
              ]
            }
        """.trimIndent()

        val result = CreatureCombatProfileParser.parse("creature:goblin", raw)
        assertNotNull(result)
        result!!
        assertEquals("Goblin Warrior", result.name)
        assertEquals(6, result.maxHp)
        assertEquals(16, result.armorClass)
        assertEquals(2, result.initiativeBonus)
        assertEquals(5, result.fortitude)
        assertEquals(7, result.reflex)
        assertEquals(3, result.will)
        assertEquals("Dogslicer", result.primaryAttack!!.name)
        assertEquals(7, result.primaryAttack!!.attackBonus)
        assertEquals("1d6", result.primaryAttack!!.damageDice)
    }
}
