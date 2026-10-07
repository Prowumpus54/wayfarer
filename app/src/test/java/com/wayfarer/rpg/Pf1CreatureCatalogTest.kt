package com.wayfarer.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class Pf1CreatureCatalogTest {
    @Test
    fun goblinResolvesToPf1BestiaryStats() {
        val goblin = Pf1CreatureCatalog.resolve("two goblins")
        assertNotNull(goblin)
        goblin!!
        assertEquals(GameRuleset.PF1E, goblin.ruleset)
        assertEquals("1/3", goblin.challengeRating)
        assertEquals(135, goblin.xpValue)
        assertEquals(6, goblin.maxHp)
        assertEquals(16, goblin.armorClass)
        assertEquals(13, goblin.touchArmorClass)
        assertEquals(14, goblin.flatFootedArmorClass)
        assertEquals(12, goblin.cmd)
        assertEquals(19, goblin.primaryAttack!!.criticalThreatMin)
    }

    @Test
    fun commonSunlessFoesResolveWithoutPf2Database() {
        listOf(
            "kobold",
            "dire rat",
            "human skeleton",
            "bugbear",
            "hobgoblin"
        ).forEach { name ->
            assertEquals(GameRuleset.PF1E, Pf1CreatureCatalog.resolve(name)!!.ruleset)
        }
        assertNull(Pf1CreatureCatalog.resolve("twig blight"))
    }
}
