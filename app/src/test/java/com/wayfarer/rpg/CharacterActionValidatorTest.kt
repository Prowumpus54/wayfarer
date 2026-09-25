package com.wayfarer.rpg

import org.junit.Assert.*
import org.junit.Test

class CharacterActionValidatorTest {
    @Test fun pf1CombinationCannotBeSavedOnPf2Sheet() {
        val result = CharacterActionValidator.validate("Build Smite + Charge + Power Attack", CharacterState())
        assertTrue(result.blocked)
        assertNull(result.action)
    }
    @Test fun unknownActionFailsClosed() {
        assertTrue(CharacterActionValidator.validate("create a destroy everything action", CharacterState()).blocked)
    }
    @Test fun skillShortcutUsesCurrentSheet() {
        val original = CharacterState()
        val updated = original.copy(abilities = original.abilities + (Ability.DEX to 20))
        val first = CharacterActionValidator.validate("create a Stealth check", original)
        val second = CharacterActionValidator.validate("create a Stealth check", updated)
        assertEquals("Stealth check", first.action)
        assertFalse(first.explanation == second.explanation)
    }
    @Test fun savedShortcutCanBeRevalidated() {
        val saved = CharacterActionValidator.validate("create a Perception check", CharacterState())
        assertEquals(saved.action, CharacterActionValidator.validate("create " + saved.action, CharacterState()).action)
    }
    @Test fun failedGmResolutionNeverRerolls() {
        val recorded = CheckResult(7, 3, 10, 15, Degree.FAILURE)
        val result = retainCheckOnRetry(recorded) { error("Must not roll again") }
        assertSame(recorded, result)
    }
    @Test fun firstResolutionRollsExactlyOnce() {
        var count = 0
        retainCheckOnRetry(null) { count++; CheckResult(7, 3, 10, 15, Degree.FAILURE) }
        assertEquals(1, count)
    }
    @Test fun multipleManualRollsRemainInOrder() {
        assertEquals("attack = 18\ndamage = 7", appendPlayerRoll(appendPlayerRoll(null, "attack = 18"), "damage = 7"))
    }
}
