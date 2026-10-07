package com.wayfarer.rpg

enum class Ability { STR, DEX, CON, INT, WIS, CHA }

enum class Proficiency(val code: String, val rankBonus: Int) {
    UNTRAINED("U", 0),
    TRAINED("T", 2),
    EXPERT("E", 4),
    MASTER("M", 6),
    LEGENDARY("L", 8);

    fun bonus(level: Int): Int =
        if (this == UNTRAINED) 0 else level + rankBonus

    fun promoted(): Proficiency = when (this) {
        UNTRAINED -> TRAINED
        TRAINED -> EXPERT
        EXPERT -> MASTER
        MASTER -> LEGENDARY
        LEGENDARY -> LEGENDARY
    }
}

data class PartyMember(
    val name: String,
    val className: String,
    val level: Int,
    val hp: Int,
    val maxHp: Int,
    val portrait: String,
    val accent: String
)

data class SkillDefinition(
    val name: String,
    val ability: Ability,
    val armorCheckPenalty: Boolean = false,
    val trainedOnly: Boolean = false
)

val skillDefinitions = listOf(
    SkillDefinition("Acrobatics", Ability.DEX, armorCheckPenalty = true),
    SkillDefinition("Appraise", Ability.INT),
    SkillDefinition("Bluff", Ability.CHA),
    SkillDefinition("Climb", Ability.STR, armorCheckPenalty = true),
    SkillDefinition("Craft", Ability.INT),
    SkillDefinition("Diplomacy", Ability.CHA),
    SkillDefinition("Disable Device", Ability.DEX, armorCheckPenalty = true, trainedOnly = true),
    SkillDefinition("Disguise", Ability.CHA),
    SkillDefinition("Escape Artist", Ability.DEX, armorCheckPenalty = true),
    SkillDefinition("Fly", Ability.DEX, armorCheckPenalty = true),
    SkillDefinition("Handle Animal", Ability.CHA, trainedOnly = true),
    SkillDefinition("Heal", Ability.WIS),
    SkillDefinition("Intimidate", Ability.CHA),
    SkillDefinition("Knowledge (arcana)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (dungeoneering)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (engineering)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (geography)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (history)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (local)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (nature)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (nobility)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (planes)", Ability.INT, trainedOnly = true),
    SkillDefinition("Knowledge (religion)", Ability.INT, trainedOnly = true),
    SkillDefinition("Linguistics", Ability.INT, trainedOnly = true),
    SkillDefinition("Perception", Ability.WIS),
    SkillDefinition("Perform", Ability.CHA),
    SkillDefinition("Profession", Ability.WIS, trainedOnly = true),
    SkillDefinition("Ride", Ability.DEX, armorCheckPenalty = true),
    SkillDefinition("Sense Motive", Ability.WIS),
    SkillDefinition("Sleight of Hand", Ability.DEX, armorCheckPenalty = true, trainedOnly = true),
    SkillDefinition("Spellcraft", Ability.INT, trainedOnly = true),
    SkillDefinition("Stealth", Ability.DEX, armorCheckPenalty = true),
    SkillDefinition("Survival", Ability.WIS),
    SkillDefinition("Swim", Ability.STR, armorCheckPenalty = true),
    SkillDefinition("Use Magic Device", Ability.CHA, trainedOnly = true)
)
data class WeaponDefinition(
    val name: String,
    val category: String,
    val damageDice: String,
    val damageType: String,
    val traits: String,
    val bulk: Double,
    val ability: Ability = Ability.STR,
    val itemBonus: Int = 0,
    val criticalThreatMin: Int = 20,
    val criticalMultiplier: Int = 2,
    val rangeIncrementFt: Int? = null
) {
    val isRanged: Boolean
        get() = rangeIncrementFt != null
}

data class ArmorDefinition(
    val name: String,
    val category: String,
    val itemBonus: Int,
    val dexCap: Int,
    val checkPenalty: Int,
    val speedPenalty: Int,
    val bulk: Double
)

val weaponCatalog = listOf(
    WeaponDefinition("Unarmed", "Unarmed", "1d3", "Bludgeoning", "Nonlethal", 0.0),
    WeaponDefinition("Dagger", "Simple", "1d4", "Piercing", "Finesse-compatible, Thrown 10 ft", 0.1, criticalThreatMin = 19, rangeIncrementFt = 10),
    WeaponDefinition("Longsword", "Martial", "1d8", "Slashing", "One-handed martial", 1.0, criticalThreatMin = 19),
    WeaponDefinition("Shortbow", "Martial", "1d6", "Piercing", "Range 60 ft", 1.0, Ability.DEX, criticalMultiplier = 3, rangeIncrementFt = 60),
    WeaponDefinition("Longbow", "Martial", "1d8", "Piercing", "Range 100 ft", 2.0, Ability.DEX, criticalMultiplier = 3, rangeIncrementFt = 100)
)

val armorCatalog = listOf(
    ArmorDefinition("Unarmored", "Unarmored", 0, 99, 0, 0, 0.0),
    ArmorDefinition("Leather Armor", "Light", 2, 6, 0, 0, 1.0),
    ArmorDefinition("Studded Leather", "Light", 3, 5, -1, 0, 1.0),
    ArmorDefinition("Chain Mail", "Medium", 6, 2, -5, -10, 2.0),
    ArmorDefinition("Full Plate", "Heavy", 9, 1, -6, -10, 4.0)
)
data class InventoryItem(
    val name: String,
    val category: String,
    val quantity: Int,
    val weight: Double,
    val icon: String,
    val description: String,
    val mechanics: String = ""
)

fun defaultInventory(): List<InventoryItem> = listOf(
    InventoryItem("Longsword", "Weapons", 1, 1.0, "🗡️", "A martial one-handed blade.", "1d8 Slashing • Versatile P"),
    InventoryItem("Leather Armor", "Armor", 1, 1.0, "🥋", "Light armor.", "Armor +2 • Max Dex +6 • ACP 0"),
    InventoryItem("Health Potion", "Consumables", 3, 0.1, "🧪", "Restorative potion.", "Consumable"),
    InventoryItem("Rope", "Other", 1, 1.0, "🪢", "50 feet of rope."),
    InventoryItem("Torch", "Other", 5, 0.1, "🔥", "Simple light source."),
    InventoryItem("Rations", "Other", 4, 0.1, "🥖", "Trail food.")
)

data class GameEvent(
    val title: String,
    val body: String,
    val type: String,
    val timeLabel: String = "Now",
    val id: String = java.util.UUID.randomUUID().toString()
)
data class CharacterState(
    val playerName: String = "",
    val characterName: String = "Thorne",
    val ruleset: String = GameRuleset.PF1E.wireName,
    val pf1SchemaVersion: Int = Pf1CharacterMigration.CURRENT_SCHEMA_VERSION,
    val xp: Int = 0,
    val pf1ExperienceTrack: String = Pf1ExperienceTrack.MEDIUM.name.lowercase(),
    val ancestry: String = "Human",
    val heritage: String = "Versatile Human",
    val background: String = "Hunter",
    val className: String = "Ranger",
    val size: String = "Medium",
    val alignment: String = "Neutral Good",
    val traits: String = "Human, Humanoid",
    val deity: String = "None",
    val level: Int = 3,
    val heroPoints: Int = 0,
    val pf1ClassLevels: Map<String, Int> = emptyMap(),
    val pf1SkillRanks: Map<String, Int> = emptyMap(),
    val pf1SkillMisc: Map<String, Int> = emptyMap(),
    val pf1ArmorEnhancement: Int = 0,
    val pf1ShieldBonus: Int = 0,
    val pf1ShieldEnhancement: Int = 0,
    val pf1NaturalArmor: Int = 0,
    val pf1DeflectionBonus: Int = 0,
    val pf1DodgeBonus: Int = 0,
    val pf1MiscAcBonus: Int = 0,
    val pf1AttackMiscBonus: Int = 0,
    val pf1DamageMiscBonus: Int = 0,
    val pf1SaveMisc: Map<String, Int> = emptyMap(),
    val pf1InitiativeMisc: Int = 0,
    val pf1CmbMisc: Int = 0,
    val pf1CmdMisc: Int = 0,
    val pf1SpellSaveMisc: Int = 0,
    val abilities: Map<Ability, Int> = mapOf(
        Ability.STR to 12, Ability.DEX to 18, Ability.CON to 14,
        Ability.INT to 10, Ability.WIS to 16, Ability.CHA to 10
    ),
    val maxHp: Int = 28,
    val currentHp: Int = 28,
    val tempHp: Int = 0,
    val dying: Int = 0,
    val wounded: Int = 0,
    val perceptionProf: Proficiency = Proficiency.TRAINED,
    val fortitudeProf: Proficiency = Proficiency.TRAINED,
    val reflexProf: Proficiency = Proficiency.EXPERT,
    val willProf: Proficiency = Proficiency.TRAINED,
    val classDcProf: Proficiency = Proficiency.TRAINED,
    val keyAbility: Ability = Ability.DEX,
    val armorProf: Map<String, Proficiency> = mapOf(
        "Unarmored" to Proficiency.TRAINED,
        "Light" to Proficiency.TRAINED,
        "Medium" to Proficiency.UNTRAINED,
        "Heavy" to Proficiency.UNTRAINED
    ),
    val weaponProf: Map<String, Proficiency> = mapOf(
        "Unarmed" to Proficiency.TRAINED,
        "Simple" to Proficiency.TRAINED,
        "Martial" to Proficiency.TRAINED
    ),
    val skillProfs: Map<String, Proficiency> = skillDefinitions.associate {
        it.name to Proficiency.UNTRAINED
    },
    val armorName: String = "Leather Armor",
    val meleeWeapon: String = "Longsword",
    val rangedWeapon: String = "Shortbow",
    val speed: Int = 30,
    val movementNotes: String = "",
    val senses: String = "Normal vision",
    val languages: String = "Common",
    val resistances: String = "",
    val conditions: String = "",
    val notes: String = "",
    val ancestryFeats: List<String> = emptyList(),
    val classFeats: List<String> = emptyList(),
    val skillFeats: List<String> = emptyList(),
    val generalFeats: List<String> = emptyList(),
    val bonusFeats: List<String> = emptyList(),
    val inventory: List<InventoryItem> = defaultInventory(),
    val currencyCp: Int = 0,
    val currencySp: Int = 5,
    val currencyGp: Int = 18,
    val currencyPp: Int = 0,
    val appearance: String = "",
    val attitude: String = "",
    val beliefs: String = "",
    val likes: String = "",
    val dislikes: String = "",
    val catchphrases: String = "",
    val ethnicity: String = "",
    val nationality: String = "",
    val birthplace: String = "",
    val age: String = "",
    val genderPronouns: String = "",
    val height: String = "",
    val weight: String = "",
    val allies: String = "",
    val enemies: String = "",
    val organizations: String = "",
    val actionsAndActivities: String = "Attack, Full Attack, Combat Maneuver",
    val freeActionsAndReactions: String = "",
    val campaignNotes: String = "",
    val magicTradition: String = "Divine",
    val castingType: String = "Prepared",
    val spellcastingAbility: Ability = Ability.WIS,
    val spellProf: Proficiency = Proficiency.TRAINED,
    val focusCurrent: Int = 0,
    val focusMax: Int = 0,
    val spellSlots: Map<Int, Int> = emptyMap(),
    val spellSlotsUsed: Map<Int, Int> = emptyMap(),
    val spells: Map<Int, List<String>> = emptyMap()
) {
    fun abilityModifier(ability: Ability): Int =
        Math.floorDiv((abilities[ability] ?: 10) - 10, 2)

    fun isPf1(): Boolean =
        GameRuleset.fromWire(ruleset) == GameRuleset.PF1E

    fun effectivePf1ClassLevels(): Map<String, Int> =
        pf1ClassLevels.ifEmpty { mapOf(className to level) }

    fun pf1ClassSkills(): Set<String> =
        effectivePf1ClassLevels().keys.flatMap { className ->
            pf1ClassProfile(className)?.classSkills.orEmpty()
        }.toSet()

    fun skillBonus(skill: SkillDefinition): Int {
        if (!isPf1()) {
            return abilityModifier(skill.ability) +
                (skillProfs[skill.name] ?: Proficiency.UNTRAINED).bonus(level)
        }
        val ranks = (pf1SkillRanks[skill.name] ?: 0).coerceIn(0, level)
        val classSkill = skill.name in pf1ClassSkills()
        val classBonus = if (ranks > 0 && classSkill) 3 else 0
        val armorPenalty = if (skill.armorCheckPenalty) armor().checkPenalty else 0
        return ranks + abilityModifier(skill.ability) + classBonus +
            armorPenalty + (pf1SkillMisc[skill.name] ?: 0)
    }

    fun baseAttackBonus(): Int =
        if (isPf1()) {
            pf1BaseAttackBonus(effectivePf1ClassLevels())
        } else {
            level
        }

    fun pf1BaseSave(save: Pf1Save): Int =
        pf1BaseSave(effectivePf1ClassLevels(), save)

    fun pf1DeathThreshold(): Int =
        -(abilities[Ability.CON] ?: 10)

    fun pf1HpState(): String =
        when {
            !isPf1() -> ""
            pf1Dead || currentHp <= pf1DeathThreshold() -> "Dead"
            currentHp < 0 && pf1Stable -> "Stable"
            currentHp < 0 -> "Dying"
            currentHp == 0 -> "Disabled"
            else -> "Conscious"
        }

    fun fortitudeSave(): Int =
        if (isPf1()) {
            pf1BaseSave(Pf1Save.FORTITUDE) + abilityModifier(Ability.CON) +
                (pf1SaveMisc["Fortitude"] ?: 0)
        } else {
            saveBonus(Ability.CON, fortitudeProf)
        }

    fun reflexSave(): Int =
        if (isPf1()) {
            pf1BaseSave(Pf1Save.REFLEX) + abilityModifier(Ability.DEX) +
                (pf1SaveMisc["Reflex"] ?: 0)
        } else {
            saveBonus(Ability.DEX, reflexProf)
        }

    fun willSave(): Int =
        if (isPf1()) {
            pf1BaseSave(Pf1Save.WILL) + abilityModifier(Ability.WIS) +
                (pf1SaveMisc["Will"] ?: 0)
        } else {
            saveBonus(Ability.WIS, willProf)
        }

    fun saveBonus(ability: Ability, proficiency: Proficiency): Int {
        if (!isPf1()) return abilityModifier(ability) + proficiency.bonus(level)
        return when (ability) {
            Ability.CON -> fortitudeSave()
            Ability.DEX -> reflexSave()
            Ability.WIS -> willSave()
            else -> abilityModifier(ability)
        }
    }

    fun perception(): Int =
        skillDefinitions.firstOrNull { it.name == "Perception" }
            ?.let(::skillBonus)
            ?: abilityModifier(Ability.WIS)

    fun initiative(): Int =
        abilityModifier(Ability.DEX) + pf1InitiativeMisc

    fun classDc(): Int =
        if (isPf1()) {
            10 + level / 2 + abilityModifier(keyAbility)
        } else {
            10 + abilityModifier(keyAbility) + classDcProf.bonus(level)
        }

    fun armor(): ArmorDefinition =
        armorCatalog.firstOrNull { it.name == armorName } ?: armorCatalog.first()

    fun ac(): Int {
        val selectedArmor = armor()
        val dex = minOf(abilityModifier(Ability.DEX), selectedArmor.dexCap)
        if (isPf1()) {
            return 10 + selectedArmor.itemBonus + pf1ArmorEnhancement +
                pf1ShieldBonus + pf1ShieldEnhancement + dex +
                pf1SizeModifier(size) + pf1NaturalArmor + pf1DeflectionBonus +
                pf1DodgeBonus + pf1MiscAcBonus
        }
        val prof = armorProf[selectedArmor.category] ?: Proficiency.UNTRAINED
        return 10 + dex + prof.bonus(level) + selectedArmor.itemBonus
    }

    fun touchAc(): Int =
        if (isPf1()) {
            val dex = minOf(abilityModifier(Ability.DEX), armor().dexCap)
            10 + dex + pf1SizeModifier(size) +
                pf1DeflectionBonus + pf1DodgeBonus + pf1MiscAcBonus
        } else ac()

    fun flatFootedAc(): Int =
        if (isPf1()) {
            10 + armor().itemBonus + pf1ArmorEnhancement +
                pf1ShieldBonus + pf1ShieldEnhancement + pf1SizeModifier(size) +
                pf1NaturalArmor + pf1DeflectionBonus + pf1MiscAcBonus
        } else ac()

    fun attackBonus(weapon: WeaponDefinition): Int {
        if (isPf1()) {
            val ability = weapon.ability
            return baseAttackBonus() + abilityModifier(ability) +
                pf1SizeModifier(size) + weapon.itemBonus + pf1AttackMiscBonus
        }
        val prof = weaponProf[weapon.category] ?: Proficiency.UNTRAINED
        return abilityModifier(weapon.ability) + prof.bonus(level) + weapon.itemBonus
    }

    fun damageBonus(weapon: WeaponDefinition): Int {
        if (!isPf1()) {
            return if (weapon.name.equals(meleeWeapon, true)) {
                abilityModifier(Ability.STR)
            } else 0
        }
        val ability = if (weapon.ability == Ability.DEX) {
            0
        } else {
            abilityModifier(Ability.STR)
        }
        return ability + weapon.itemBonus + pf1DamageMiscBonus
    }

    fun iterativeAttackBonuses(weapon: WeaponDefinition): List<Int> {
        if (!isPf1()) return listOf(attackBonus(weapon))
        val top = attackBonus(weapon)
        val base = baseAttackBonus()
        return pf1IterativeAttackBonuses(base).map { iterativeBab ->
            top - base + iterativeBab
        }
    }

    fun cmb(): Int =
        baseAttackBonus() + abilityModifier(Ability.STR) +
            pf1CombatManeuverSizeModifier(size) + pf1CmbMisc

    fun cmd(): Int =
        10 + baseAttackBonus() + abilityModifier(Ability.STR) +
            abilityModifier(Ability.DEX) +
            pf1CombatManeuverSizeModifier(size) + pf1DeflectionBonus +
            pf1DodgeBonus + pf1CmdMisc

    fun spellAttack(): Int =
        if (isPf1()) {
            baseAttackBonus() + abilityModifier(Ability.DEX) +
                pf1SizeModifier(size)
        } else {
            abilityModifier(spellcastingAbility) + spellProf.bonus(level)
        }

    fun spellDc(spellLevel: Int): Int =
        if (isPf1()) {
            10 + spellLevel + abilityModifier(spellcastingAbility) +
                pf1SpellSaveMisc
        } else {
            10 + spellAttack()
        }

    fun spellDc(): Int = spellDc(0)

    fun casterLevel(): Int {
        if (!isPf1()) return level
        val classLevel = effectivePf1ClassLevels().entries
            .firstOrNull { pf1ClassProfile(it.key)?.castingAbility != null }
            ?.value ?: return 0
        val profile = pf1ClassProfile(className)
        return if (profile?.spellStartLevel == 4) {
            (classLevel - 3).coerceAtLeast(0)
        } else {
            classLevel
        }
    }

    fun spellSlotsRemaining(level: Int): Int =
        ((spellSlots[level] ?: 0) - (spellSlotsUsed[level] ?: 0)).coerceAtLeast(0)
}
enum class AppScreen(val label: String, val glyph: String) {
    Home("Home", "⌂"),
    Play("Play", "⚔"),
    Map("Map", "▱"),
    Journal("Journal", "▤"),
    Glossary("Glossary", "⌕"),
    Party("Party", "♟")
}

fun starterCharacters(): List<CharacterState> =
    listOf(
        CharacterState(
            xp = pf1XpThreshold(3, Pf1ExperienceTrack.MEDIUM),
            pf1ClassLevels = mapOf("Ranger" to 3),
            pf1SkillRanks = mapOf(
                "Climb" to 3,
                "Heal" to 3,
                "Knowledge (nature)" to 3,
                "Perception" to 3,
                "Ride" to 3,
                "Stealth" to 3,
                "Survival" to 3
            ),
            generalFeats = listOf("Point-Blank Shot", "Precise Shot"),
            classFeats = listOf("Rapid Shot"),
            bonusFeats = listOf("Endurance")
        )
    )
