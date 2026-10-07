package com.wayfarer.rpg

import kotlin.math.floor

enum class GameRuleset(val wireName: String) {
    PF1E("pf1e"),
    PF2E_ADAPTED("pf2e-adapted");

    companion object {
        fun fromWire(value: String): GameRuleset =
            entries.firstOrNull { it.wireName.equals(value.trim(), true) }
                ?: PF1E
    }
}

enum class Pf1BabProgression { FULL, THREE_QUARTER, HALF }
enum class Pf1Save { FORTITUDE, REFLEX, WILL }
enum class Pf1SaveProgression { GOOD, POOR }
enum class Pf1ExperienceTrack { SLOW, MEDIUM, FAST }

data class Pf1ClassProfile(
    val name: String,
    val hitDie: Int,
    val bab: Pf1BabProgression,
    val goodSaves: Set<Pf1Save>,
    val skillRanksPerLevel: Int,
    val classSkills: Set<String>,
    val castingAbility: Ability? = null,
    val spellStartLevel: Int? = null
) {
    fun baseAttackBonus(level: Int): Int = when (bab) {
        Pf1BabProgression.FULL -> level
        Pf1BabProgression.THREE_QUARTER -> floor(level * 0.75).toInt()
        Pf1BabProgression.HALF -> level / 2
    }

    fun baseSave(level: Int, save: Pf1Save): Int =
        if (save in goodSaves) 2 + level / 2 else level / 3
}

private fun skills(vararg names: String): Set<String> = names.toSet()

val pf1ClassProfiles: Map<String, Pf1ClassProfile> = listOf(
    Pf1ClassProfile(
        "Barbarian", 12, Pf1BabProgression.FULL,
        setOf(Pf1Save.FORTITUDE), 4,
        skills(
            "Acrobatics", "Climb", "Craft", "Handle Animal",
            "Intimidate", "Knowledge (nature)", "Perception",
            "Ride", "Survival", "Swim"
        )
    ),
    Pf1ClassProfile(
        "Bard", 8, Pf1BabProgression.THREE_QUARTER,
        setOf(Pf1Save.REFLEX, Pf1Save.WILL), 6,
        skills(
            "Acrobatics", "Appraise", "Bluff", "Climb", "Craft",
            "Diplomacy", "Disguise", "Escape Artist", "Intimidate",
            "Knowledge (arcana)", "Knowledge (dungeoneering)",
            "Knowledge (engineering)", "Knowledge (geography)",
            "Knowledge (history)", "Knowledge (local)",
            "Knowledge (nature)", "Knowledge (nobility)",
            "Knowledge (planes)", "Knowledge (religion)",
            "Linguistics", "Perception", "Perform", "Profession",
            "Sense Motive", "Sleight of Hand", "Spellcraft",
            "Stealth", "Use Magic Device"
        ),
        Ability.CHA, 1
    ),
    Pf1ClassProfile(
        "Cleric", 8, Pf1BabProgression.THREE_QUARTER,
        setOf(Pf1Save.FORTITUDE, Pf1Save.WILL), 2,
        skills(
            "Appraise", "Craft", "Diplomacy", "Heal",
            "Knowledge (arcana)", "Knowledge (history)",
            "Knowledge (nobility)", "Knowledge (planes)",
            "Knowledge (religion)", "Linguistics", "Profession",
            "Sense Motive", "Spellcraft"
        ),
        Ability.WIS, 1
    ),
    Pf1ClassProfile(
        "Druid", 8, Pf1BabProgression.THREE_QUARTER,
        setOf(Pf1Save.FORTITUDE, Pf1Save.WILL), 4,
        skills(
            "Climb", "Craft", "Fly", "Handle Animal", "Heal",
            "Knowledge (geography)", "Knowledge (nature)",
            "Perception", "Profession", "Ride", "Spellcraft",
            "Survival", "Swim"
        ),
        Ability.WIS, 1
    ),
    Pf1ClassProfile(
        "Fighter", 10, Pf1BabProgression.FULL,
        setOf(Pf1Save.FORTITUDE), 2,
        skills(
            "Climb", "Craft", "Handle Animal", "Intimidate",
            "Knowledge (dungeoneering)", "Knowledge (engineering)",
            "Profession", "Ride", "Survival", "Swim"
        )
    ),
    Pf1ClassProfile(
        "Monk", 8, Pf1BabProgression.THREE_QUARTER,
        setOf(Pf1Save.FORTITUDE, Pf1Save.REFLEX, Pf1Save.WILL), 4,
        skills(
            "Acrobatics", "Climb", "Craft", "Escape Artist",
            "Intimidate", "Knowledge (history)", "Knowledge (religion)",
            "Perception", "Perform", "Profession", "Ride",
            "Sense Motive", "Stealth", "Swim"
        )
    ),
    Pf1ClassProfile(
        "Paladin", 10, Pf1BabProgression.FULL,
        setOf(Pf1Save.FORTITUDE, Pf1Save.WILL), 2,
        skills(
            "Craft", "Diplomacy", "Handle Animal", "Heal",
            "Knowledge (nobility)", "Knowledge (religion)",
            "Profession", "Ride", "Sense Motive", "Spellcraft"
        ),
        Ability.CHA, 4
    ),
    Pf1ClassProfile(
        "Ranger", 10, Pf1BabProgression.FULL,
        setOf(Pf1Save.FORTITUDE, Pf1Save.REFLEX), 6,
        skills(
            "Climb", "Craft", "Handle Animal", "Heal", "Intimidate",
            "Knowledge (dungeoneering)", "Knowledge (geography)",
            "Knowledge (nature)", "Perception", "Profession", "Ride",
            "Spellcraft", "Stealth", "Survival", "Swim"
        ),
        Ability.WIS, 4
    ),
    Pf1ClassProfile(
        "Rogue", 8, Pf1BabProgression.THREE_QUARTER,
        setOf(Pf1Save.REFLEX), 8,
        skills(
            "Acrobatics", "Appraise", "Bluff", "Climb", "Craft",
            "Diplomacy", "Disable Device", "Disguise", "Escape Artist",
            "Intimidate", "Knowledge (dungeoneering)",
            "Knowledge (local)", "Linguistics", "Perception",
            "Perform", "Profession", "Sense Motive", "Sleight of Hand",
            "Stealth", "Swim", "Use Magic Device"
        )
    ),
    Pf1ClassProfile(
        "Sorcerer", 6, Pf1BabProgression.HALF,
        setOf(Pf1Save.WILL), 2,
        skills(
            "Appraise", "Bluff", "Craft", "Fly", "Intimidate",
            "Knowledge (arcana)", "Profession", "Spellcraft",
            "Use Magic Device"
        ),
        Ability.CHA, 1
    ),
    Pf1ClassProfile(
        "Wizard", 6, Pf1BabProgression.HALF,
        setOf(Pf1Save.WILL), 2,
        skills(
            "Appraise", "Craft", "Fly", "Knowledge (arcana)",
            "Knowledge (dungeoneering)", "Knowledge (engineering)",
            "Knowledge (geography)", "Knowledge (history)",
            "Knowledge (local)", "Knowledge (nature)",
            "Knowledge (nobility)", "Knowledge (planes)",
            "Knowledge (religion)", "Linguistics", "Profession",
            "Spellcraft"
        ),
        Ability.INT, 1
    )
).associateBy { it.name.lowercase() }

fun pf1ClassProfile(name: String): Pf1ClassProfile? =
    pf1ClassProfiles[name.trim().lowercase()]

fun pf1BaseAttackBonus(classLevels: Map<String, Int>): Int =
    classLevels.entries.sumOf { (name, level) ->
        pf1ClassProfile(name)?.baseAttackBonus(level.coerceAtLeast(0)) ?: 0
    }

fun pf1BaseSave(classLevels: Map<String, Int>, save: Pf1Save): Int =
    classLevels.entries.sumOf { (name, level) ->
        pf1ClassProfile(name)?.baseSave(level.coerceAtLeast(0), save) ?: 0
    }

fun pf1IterativeAttackBonuses(baseAttackBonus: Int): List<Int> {
    if (baseAttackBonus <= 0) return listOf(baseAttackBonus)
    val out = mutableListOf<Int>()
    var value = baseAttackBonus
    while (value > 0 && out.size < 4) {
        out += value
        value -= 5
    }
    return out
}

fun pf1SizeModifier(size: String): Int = when (size.trim().lowercase()) {
    "fine" -> 8
    "diminutive" -> 4
    "tiny" -> 2
    "small" -> 1
    "large" -> -1
    "huge" -> -2
    "gargantuan" -> -4
    "colossal" -> -8
    else -> 0
}

fun pf1CombatManeuverSizeModifier(size: String): Int =
    -pf1SizeModifier(size)

private val pf1MediumXpThresholds = mapOf(
    1 to 0, 2 to 2_000, 3 to 5_000, 4 to 9_000, 5 to 15_000,
    6 to 23_000, 7 to 35_000, 8 to 51_000, 9 to 75_000, 10 to 105_000,
    11 to 155_000, 12 to 220_000, 13 to 315_000, 14 to 445_000,
    15 to 635_000, 16 to 890_000, 17 to 1_300_000, 18 to 1_800_000,
    19 to 2_550_000, 20 to 3_600_000
)

private val pf1SlowXpThresholds = mapOf(
    1 to 0, 2 to 3_000, 3 to 7_500, 4 to 14_000, 5 to 23_000,
    6 to 35_000, 7 to 53_000, 8 to 77_000, 9 to 115_000, 10 to 160_000,
    11 to 235_000, 12 to 330_000, 13 to 475_000, 14 to 665_000,
    15 to 955_000, 16 to 1_135_000, 17 to 1_900_000, 18 to 2_700_000,
    19 to 3_850_000, 20 to 5_350_000
)

private val pf1FastXpThresholds = mapOf(
    1 to 0, 2 to 1_300, 3 to 3_300, 4 to 6_000, 5 to 10_000,
    6 to 15_000, 7 to 23_000, 8 to 34_000, 9 to 50_000, 10 to 71_000,
    11 to 105_000, 12 to 145_000, 13 to 210_000, 14 to 295_000,
    15 to 425_000, 16 to 600_000, 17 to 850_000, 18 to 1_200_000,
    19 to 1_700_000, 20 to 2_400_000
)

fun pf1XpThreshold(level: Int, track: Pf1ExperienceTrack): Int =
    when (track) {
        Pf1ExperienceTrack.SLOW -> pf1SlowXpThresholds
        Pf1ExperienceTrack.MEDIUM -> pf1MediumXpThresholds
        Pf1ExperienceTrack.FAST -> pf1FastXpThresholds
    }[level.coerceIn(1, 20)] ?: 0

fun pf1XpForCr(cr: String): Int = when (cr.trim()) {
    "1/8" -> 50
    "1/6" -> 65
    "1/4" -> 100
    "1/3" -> 135
    "1/2" -> 200
    "1" -> 400
    "2" -> 600
    "3" -> 800
    "4" -> 1_200
    "5" -> 1_600
    "6" -> 2_400
    "7" -> 3_200
    "8" -> 4_800
    "9" -> 6_400
    "10" -> 9_600
    "11" -> 12_800
    "12" -> 19_200
    "13" -> 25_600
    "14" -> 38_400
    "15" -> 51_200
    "16" -> 76_800
    "17" -> 102_400
    "18" -> 153_600
    "19" -> 204_800
    "20" -> 307_200
    else -> 0
}

private fun indexedSlots(
    counts: IntArray,
    firstSpellLevel: Int = 0
): Map<Int, Int> =
    counts.mapIndexed { index, count ->
        (index + firstSpellLevel) to count
    }.toMap()

private val pf1FullPreparedSpellSlots = listOf(
    intArrayOf(),
    intArrayOf(3, 1),
    intArrayOf(4, 2),
    intArrayOf(4, 2, 1),
    intArrayOf(4, 3, 2),
    intArrayOf(4, 3, 2, 1),
    intArrayOf(4, 3, 3, 2),
    intArrayOf(4, 4, 3, 2, 1),
    intArrayOf(4, 4, 3, 3, 2),
    intArrayOf(4, 4, 4, 3, 2, 1),
    intArrayOf(4, 4, 4, 3, 3, 2),
    intArrayOf(4, 4, 4, 4, 3, 2, 1),
    intArrayOf(4, 4, 4, 4, 3, 3, 2),
    intArrayOf(4, 4, 4, 4, 4, 3, 2, 1),
    intArrayOf(4, 4, 4, 4, 4, 3, 3, 2),
    intArrayOf(4, 4, 4, 4, 4, 4, 3, 2, 1),
    intArrayOf(4, 4, 4, 4, 4, 4, 3, 3, 2),
    intArrayOf(4, 4, 4, 4, 4, 4, 4, 3, 2, 1),
    intArrayOf(4, 4, 4, 4, 4, 4, 4, 3, 3, 2),
    intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 3, 3),
    intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
)

private val pf1SorcererSpellSlots = listOf(
    intArrayOf(),
    intArrayOf(3),
    intArrayOf(4),
    intArrayOf(5),
    intArrayOf(6, 3),
    intArrayOf(6, 4),
    intArrayOf(6, 5, 3),
    intArrayOf(6, 6, 4),
    intArrayOf(6, 6, 5, 3),
    intArrayOf(6, 6, 6, 4),
    intArrayOf(6, 6, 6, 5, 3),
    intArrayOf(6, 6, 6, 6, 4),
    intArrayOf(6, 6, 6, 6, 5, 3),
    intArrayOf(6, 6, 6, 6, 6, 4),
    intArrayOf(6, 6, 6, 6, 6, 5, 3),
    intArrayOf(6, 6, 6, 6, 6, 6, 4),
    intArrayOf(6, 6, 6, 6, 6, 6, 5, 3),
    intArrayOf(6, 6, 6, 6, 6, 6, 6, 4),
    intArrayOf(6, 6, 6, 6, 6, 6, 6, 5, 3),
    intArrayOf(6, 6, 6, 6, 6, 6, 6, 6, 4),
    intArrayOf(6, 6, 6, 6, 6, 6, 6, 6, 6)
)

private val pf1BardSpellSlots = listOf(
    intArrayOf(),
    intArrayOf(1),
    intArrayOf(2),
    intArrayOf(3),
    intArrayOf(3, 1),
    intArrayOf(4, 2),
    intArrayOf(4, 3),
    intArrayOf(4, 3, 1),
    intArrayOf(4, 4, 2),
    intArrayOf(5, 4, 3),
    intArrayOf(5, 4, 3, 1),
    intArrayOf(5, 4, 4, 2),
    intArrayOf(5, 5, 4, 3),
    intArrayOf(5, 5, 4, 3, 1),
    intArrayOf(5, 5, 4, 4, 2),
    intArrayOf(5, 5, 5, 4, 3),
    intArrayOf(5, 5, 5, 4, 3, 1),
    intArrayOf(5, 5, 5, 4, 4, 2),
    intArrayOf(5, 5, 5, 5, 4, 3),
    intArrayOf(5, 5, 5, 5, 5, 4),
    intArrayOf(5, 5, 5, 5, 5, 5)
)

fun pf1RangerBaseSpellSlots(level: Int): Map<Int, Int> = when (level) {
    in 1..3 -> emptyMap()
    4 -> mapOf(1 to 0)
    5, 6 -> mapOf(1 to 1)
    7 -> mapOf(1 to 1, 2 to 0)
    8 -> mapOf(1 to 1, 2 to 1)
    9 -> mapOf(1 to 2, 2 to 1)
    10 -> mapOf(1 to 2, 2 to 1, 3 to 0)
    11 -> mapOf(1 to 2, 2 to 1, 3 to 1)
    12 -> mapOf(1 to 2, 2 to 2, 3 to 1)
    13 -> mapOf(1 to 3, 2 to 2, 3 to 1, 4 to 0)
    14 -> mapOf(1 to 3, 2 to 2, 3 to 1, 4 to 1)
    15 -> mapOf(1 to 3, 2 to 2, 3 to 2, 4 to 1)
    16 -> mapOf(1 to 3, 2 to 3, 3 to 2, 4 to 1)
    17 -> mapOf(1 to 4, 2 to 3, 3 to 2, 4 to 1)
    18 -> mapOf(1 to 4, 2 to 3, 3 to 2, 4 to 2)
    19 -> mapOf(1 to 4, 2 to 3, 3 to 3, 4 to 2)
    else -> mapOf(1 to 4, 2 to 4, 3 to 3, 4 to 3)
}

fun pf1BaseSpellSlots(className: String, level: Int): Map<Int, Int> {
    val safeLevel = level.coerceIn(1, 20)
    return when (className.trim().lowercase()) {
        "wizard", "cleric", "druid" ->
            indexedSlots(pf1FullPreparedSpellSlots[safeLevel])
        "sorcerer" ->
            indexedSlots(pf1SorcererSpellSlots[safeLevel], firstSpellLevel = 1)
        "bard" ->
            indexedSlots(pf1BardSpellSlots[safeLevel], firstSpellLevel = 1)
        "paladin", "ranger" -> pf1RangerBaseSpellSlots(safeLevel)
        else -> emptyMap()
    }
}

fun pf1SpellSlotsForClass(
    className: String,
    level: Int,
    castingAbilityScore: Int
): Map<Int, Int> =
    pf1BaseSpellSlots(className, level).mapValues { (spellLevel, base) ->
        if (spellLevel == 0) {
            base
        } else {
            base + pf1BonusSpells(castingAbilityScore, spellLevel)
        }
    }

fun pf1DomainSpellSlots(className: String, level: Int): Map<Int, Int> {
    if (!className.equals("Cleric", true)) return emptyMap()
    return pf1BaseSpellSlots(className, level)
        .filterKeys { it > 0 }
        .mapValues { 1 }
}

private val pf1SorcererSpellsKnown = listOf(
    intArrayOf(),
    intArrayOf(4, 2),
    intArrayOf(5, 2),
    intArrayOf(5, 3),
    intArrayOf(6, 3, 1),
    intArrayOf(6, 4, 2),
    intArrayOf(7, 4, 2, 1),
    intArrayOf(7, 5, 3, 2),
    intArrayOf(8, 5, 3, 2, 1),
    intArrayOf(8, 5, 4, 3, 2),
    intArrayOf(9, 5, 4, 3, 2, 1),
    intArrayOf(9, 5, 5, 4, 3, 2),
    intArrayOf(9, 5, 5, 4, 3, 2, 1),
    intArrayOf(9, 5, 5, 4, 4, 3, 2),
    intArrayOf(9, 5, 5, 4, 4, 3, 2, 1),
    intArrayOf(9, 5, 5, 4, 4, 4, 3, 2),
    intArrayOf(9, 5, 5, 4, 4, 4, 3, 2, 1),
    intArrayOf(9, 5, 5, 4, 4, 4, 3, 3, 2),
    intArrayOf(9, 5, 5, 4, 4, 4, 3, 3, 2, 1),
    intArrayOf(9, 5, 5, 4, 4, 4, 3, 3, 3, 2),
    intArrayOf(9, 5, 5, 4, 4, 4, 3, 3, 3, 3)
)

private val pf1BardSpellsKnown = listOf(
    intArrayOf(),
    intArrayOf(4, 2),
    intArrayOf(5, 3),
    intArrayOf(6, 4),
    intArrayOf(6, 4, 2),
    intArrayOf(6, 4, 3),
    intArrayOf(6, 4, 4),
    intArrayOf(6, 5, 4, 2),
    intArrayOf(6, 5, 4, 3),
    intArrayOf(6, 5, 4, 4),
    intArrayOf(6, 5, 5, 4, 2),
    intArrayOf(6, 6, 5, 4, 3),
    intArrayOf(6, 6, 5, 4, 4),
    intArrayOf(6, 6, 5, 5, 4, 2),
    intArrayOf(6, 6, 6, 5, 4, 3),
    intArrayOf(6, 6, 6, 5, 4, 4),
    intArrayOf(6, 6, 6, 5, 5, 4, 2),
    intArrayOf(6, 6, 6, 6, 5, 4, 3),
    intArrayOf(6, 6, 6, 6, 5, 4, 4),
    intArrayOf(6, 6, 6, 6, 5, 5, 4),
    intArrayOf(6, 6, 6, 6, 6, 5, 5)
)

fun pf1SpellsKnownLimit(
    className: String,
    level: Int
): Map<Int, Int>? {
    val safeLevel = level.coerceIn(1, 20)
    return when (className.trim().lowercase()) {
        "sorcerer" -> indexedSlots(pf1SorcererSpellsKnown[safeLevel])
        "bard" -> indexedSlots(pf1BardSpellsKnown[safeLevel])
        else -> null
    }
}

fun pf1CastingType(className: String): String =
    when (className.trim().lowercase()) {
        "sorcerer", "bard" -> "Spontaneous"
        "wizard", "cleric", "druid", "paladin", "ranger" -> "Prepared"
        else -> ""
    }

fun pf1BonusSpells(abilityScore: Int, spellLevel: Int): Int {
    if (spellLevel <= 0) return 0
    val modifier = Math.floorDiv(abilityScore - 10, 2)
    if (modifier < spellLevel) return 0
    return 1 + (modifier - spellLevel) / 4
}

object Pf1CharacterMigration {
    const val CURRENT_SCHEMA_VERSION = 1

    fun migrate(character: CharacterState): CharacterState {
        if (
            GameRuleset.fromWire(character.ruleset) == GameRuleset.PF1E &&
            character.pf1SchemaVersion >= CURRENT_SCHEMA_VERSION
        ) return character

        val classLevels = character.pf1ClassLevels.ifEmpty {
            mapOf(character.className to character.level)
        }
        val migratedRanks = character.pf1SkillRanks.toMutableMap()

        fun trained(oldName: String): Boolean =
            (character.skillProfs[oldName] ?: Proficiency.UNTRAINED) !=
                Proficiency.UNTRAINED

        fun maxRank(name: String, oldName: String = name) {
            if (trained(oldName) && migratedRanks[name] == null) {
                migratedRanks[name] = character.level
            }
        }

        maxRank("Acrobatics")
        if (trained("Athletics")) {
            migratedRanks.putIfAbsent("Climb", character.level)
            migratedRanks.putIfAbsent("Swim", character.level)
        }
        maxRank("Craft", "Crafting")
        maxRank("Bluff", "Deception")
        maxRank("Diplomacy")
        maxRank("Intimidate", "Intimidation")
        maxRank("Heal", "Medicine")
        maxRank("Knowledge (arcana)", "Arcana")
        maxRank("Knowledge (nature)", "Nature")
        maxRank("Knowledge (religion)", "Religion")
        maxRank("Perform", "Performance")
        maxRank("Stealth")
        maxRank("Survival")
        if (trained("Thievery")) {
            migratedRanks.putIfAbsent("Disable Device", character.level)
            migratedRanks.putIfAbsent("Sleight of Hand", character.level)
        }

        val track = runCatching {
            Pf1ExperienceTrack.valueOf(character.pf1ExperienceTrack.uppercase())
        }.getOrDefault(Pf1ExperienceTrack.MEDIUM)
        val migratedXp = maxOf(character.xp, pf1XpThreshold(character.level, track))

        val rangerLevel = classLevels.entries
            .firstOrNull { it.key.equals("Ranger", true) }
            ?.value ?: 0
        val castingProfile = pf1ClassProfile(character.className)
        val castingAbility = castingProfile?.castingAbility
        val classLevel = classLevels[character.className] ?: character.level
        val slots = if (
            castingAbility != null &&
            character.spellSlots.isEmpty()
        ) {
            pf1SpellSlotsForClass(
                character.className,
                classLevel,
                character.abilities[castingAbility] ?: 10
            )
        } else {
            character.spellSlots
        }

        val mappedClassFeats = character.classFeats
            .filterNot { it.equals("Hunted Shot", true) }
            .toMutableList()
            .apply {
                if (
                    character.classFeats.any { it.equals("Hunted Shot", true) } &&
                    none { it.equals("Rapid Shot", true) }
                ) {
                    add("Rapid Shot")
                }
            }
        val mappedBonusFeats = character.bonusFeats.toMutableList().apply {
            if (
                rangerLevel >= 3 &&
                none { it.equals("Endurance", true) }
            ) {
                add("Endurance")
            }
        }

        return character.copy(
            ruleset = GameRuleset.PF1E.wireName,
            pf1SchemaVersion = CURRENT_SCHEMA_VERSION,
            pf1ClassLevels = classLevels,
            pf1SkillRanks = migratedRanks,
            xp = migratedXp,
            ancestryFeats = emptyList(),
            classFeats = mappedClassFeats,
            skillFeats = emptyList(),
            bonusFeats = mappedBonusFeats,
            spellSlots = slots,
            castingType = pf1CastingType(character.className),
            spellcastingAbility = castingProfile?.castingAbility
                ?: character.spellcastingAbility,
            focusCurrent = 0,
            focusMax = 0
        )
    }
}
