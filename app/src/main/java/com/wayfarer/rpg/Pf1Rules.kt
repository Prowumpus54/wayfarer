package com.wayfarer.rpg

enum class Pf1Save { FORTITUDE, REFLEX, WILL }
enum class Pf1ActionType { FREE, SWIFT, IMMEDIATE, MOVE, STANDARD, FULL_ROUND }

data class Pf1ClassLevel(val name: String, val level: Int)
data class Pf1Weapon(
    val name: String,
    val damage: String,
    val criticalThreat: Int = 20,
    val criticalMultiplier: Int = 2,
    val enhancement: Int = 0,
    val twoHanded: Boolean = false
)

data class Pf1LimitedResource(
    val name: String,
    val current: Int,
    val maximum: Int
)

data class Pf1CombatState(
    val baseAttackBonus: Int,
    val fortitude: Int,
    val reflex: Int,
    val will: Int,
    val armorClass: Int,
    val touchArmorClass: Int,
    val flatFootedArmorClass: Int,
    val cmb: Int,
    val cmd: Int,
    val feats: Set<String> = emptySet(),
    val classLevels: List<Pf1ClassLevel> = emptyList(),
    val resources: List<Pf1LimitedResource> = emptyList()
)
fun Pf1CombatState.iterativeAttackBonuses(): List<Int> {
    if (baseAttackBonus <= 0) return listOf(baseAttackBonus)
    return generateSequence(baseAttackBonus) { previous ->
        (previous - 5).takeIf { it > 0 }
    }.toList()
}

fun Pf1CombatState.hasFeat(name: String): Boolean =
    feats.any { it.equals(name, ignoreCase = true) }

fun Pf1CombatState.classLevel(name: String): Int =
    classLevels.firstOrNull { it.name.equals(name, ignoreCase = true) }?.level ?: 0

fun Pf1CombatState.resource(name: String): Pf1LimitedResource? =
    resources.firstOrNull { it.name.equals(name, ignoreCase = true) }

data class Pf1AttackModifiers(
    val attackBonus: Int = 0,
    val damageBonus: Int = 0,
    val armorClassPenalty: Int = 0,
    val notes: List<String> = emptyList()
)

data class Pf1ActionValidation(
    val legal: Boolean,
    val modifiers: Pf1AttackModifiers = Pf1AttackModifiers(),
    val resourceCosts: Map<String, Int> = emptyMap(),
    val problems: List<String> = emptyList()
)