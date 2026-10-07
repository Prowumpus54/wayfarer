package com.wayfarer.rpg

object Pf1ActionEngine {
    fun validatePowerAttack(state: Pf1CombatState): Pf1ActionValidation {
        val problems = mutableListOf<String>()
        if (state.baseAttackBonus < 1) problems += "Power Attack requires base attack bonus +1."
        if (!state.hasFeat("Power Attack")) problems += "Character does not have Power Attack."
        if (problems.isNotEmpty()) return Pf1ActionValidation(false, problems = problems)

        val step = 1 + (state.baseAttackBonus / 4)
        return Pf1ActionValidation(
            legal = true,
            modifiers = Pf1AttackModifiers(
                attackBonus = -step,
                damageBonus = step * 2,
                notes = listOf("Damage assumes a normal one-handed melee weapon; weapon handling can modify this.")
            )
        )
    }

    fun validateCharge(state: Pf1CombatState): Pf1ActionValidation =
        Pf1ActionValidation(
            legal = true,
            modifiers = Pf1AttackModifiers(
                attackBonus = 2,
                armorClassPenalty = -2,
                notes = listOf("Requires a clear charge path and follows PF1e charge movement restrictions.")
            )
        )

    fun validateSmiteEvil(state: Pf1CombatState): Pf1ActionValidation {
        val problems = mutableListOf<String>()
        val paladinLevel = state.classLevel("Paladin")
        if (paladinLevel < 1) problems += "Smite Evil requires the paladin class feature."
        val smites = state.resource("Smite Evil")
        if (smites == null || smites.current < 1) problems += "No Smite Evil use is available."
        if (problems.isNotEmpty()) return Pf1ActionValidation(false, problems = problems)

        return Pf1ActionValidation(
            legal = true,
            modifiers = Pf1AttackModifiers(
                damageBonus = paladinLevel,
                notes = listOf("Attack bonus from Charisma and target eligibility are resolved with target context.")
            ),
            resourceCosts = mapOf("Smite Evil" to 1)
        )
    }

    fun combine(vararg validations: Pf1ActionValidation): Pf1ActionValidation {
        val problems = validations.flatMap { it.problems }
        if (validations.any { !it.legal }) return Pf1ActionValidation(false, problems = problems)
        return Pf1ActionValidation(
            legal = true,
            modifiers = Pf1AttackModifiers(
                attackBonus = validations.sumOf { it.modifiers.attackBonus },
                damageBonus = validations.sumOf { it.modifiers.damageBonus },
                armorClassPenalty = validations.sumOf { it.modifiers.armorClassPenalty },
                notes = validations.flatMap { it.modifiers.notes }
            ),
            resourceCosts = validations.flatMap { it.resourceCosts.entries }
                .groupingBy { it.key }.fold(0) { total, entry -> total + entry.value }
        )
    }
}