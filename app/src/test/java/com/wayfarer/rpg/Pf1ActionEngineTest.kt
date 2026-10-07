package com.wayfarer.rpg

import org.junit.Assert.*
import org.junit.Test

class Pf1ActionEngineTest {
    private fun paladin() = Pf1CombatState(
        baseAttackBonus = 6,
        fortitude = 7, reflex = 3, will = 5,
        armorClass = 20, touchArmorClass = 11, flatFootedArmorClass = 19,
        cmb = 9, cmd = 20,
        feats = setOf("Power Attack"),
        classLevels = listOf(Pf1ClassLevel("Paladin", 6)),
        resources = listOf(Pf1LimitedResource("Smite Evil", 2, 2))
    )

    @Test fun iterativeAttacksUseBabSteps() {
        assertEquals(listOf(6, 1), paladin().iterativeAttackBonuses())
    }

    @Test fun compoundSmiteChargePowerAttackCombines() {
        val state = paladin()
        val result = Pf1ActionEngine.combine(
            Pf1ActionEngine.validateSmiteEvil(state),
            Pf1ActionEngine.validateCharge(state),
            Pf1ActionEngine.validatePowerAttack(state)
        )
        assertTrue(result.legal)
        assertEquals(0, result.modifiers.attackBonus)
        assertEquals(10, result.modifiers.damageBonus)
        assertEquals(-2, result.modifiers.armorClassPenalty)
        assertEquals(1, result.resourceCosts["Smite Evil"])
    }
    @Test fun missingFeatBlocksPowerAttack() {
        val state = paladin().copy(feats = emptySet())
        val result = Pf1ActionEngine.validatePowerAttack(state)
        assertFalse(result.legal)
        assertTrue(result.problems.any { "Power Attack" in it })
    }

    @Test fun depletedSmiteBlocksCompoundAction() {
        val state = paladin().copy(
            resources = listOf(Pf1LimitedResource("Smite Evil", 0, 2))
        )
        val result = Pf1ActionEngine.combine(
            Pf1ActionEngine.validateSmiteEvil(state),
            Pf1ActionEngine.validateCharge(state),
            Pf1ActionEngine.validatePowerAttack(state)
        )
        assertFalse(result.legal)
        assertTrue(result.problems.any { "Smite Evil" in it })
    }
}
