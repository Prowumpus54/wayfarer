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

data class SkillDefinition(val name: String, val ability: Ability)
val skillDefinitions = listOf(
    SkillDefinition("Acrobatics", Ability.DEX),
    SkillDefinition("Arcana", Ability.INT),
    SkillDefinition("Athletics", Ability.STR),
    SkillDefinition("Crafting", Ability.INT),
    SkillDefinition("Deception", Ability.CHA),
    SkillDefinition("Diplomacy", Ability.CHA),
    SkillDefinition("Intimidation", Ability.CHA),
    SkillDefinition("Medicine", Ability.WIS),
    SkillDefinition("Nature", Ability.WIS),
    SkillDefinition("Occultism", Ability.INT),
    SkillDefinition("Performance", Ability.CHA),
    SkillDefinition("Religion", Ability.WIS),
    SkillDefinition("Society", Ability.INT),
    SkillDefinition("Stealth", Ability.DEX),
    SkillDefinition("Survival", Ability.WIS),
    SkillDefinition("Thievery", Ability.DEX),
    SkillDefinition("Lore 1", Ability.INT),
    SkillDefinition("Lore 2", Ability.INT)
)
data class WeaponDefinition(
    val name: String,
    val category: String,
    val damageDice: String,
    val damageType: String,
    val traits: String,
    val bulk: Double,
    val ability: Ability = Ability.STR,
    val itemBonus: Int = 0
)

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
    WeaponDefinition("Unarmed", "Unarmed", "1d4", "Bludgeoning", "Agile, Finesse", 0.0, Ability.DEX),
    WeaponDefinition("Dagger", "Simple", "1d4", "Piercing", "Agile, Finesse, Thrown 10 ft, Versatile S", 0.1, Ability.DEX),
    WeaponDefinition("Longsword", "Martial", "1d8", "Slashing", "Versatile P", 1.0),
    WeaponDefinition("Shortbow", "Martial", "1d6", "Piercing", "Deadly d10, Range 60 ft", 1.0, Ability.DEX),
    WeaponDefinition("Longbow", "Martial", "1d8", "Piercing", "Deadly d10, Volley 30 ft, Range 100 ft", 2.0, Ability.DEX)
)

val armorCatalog = listOf(
    ArmorDefinition("Unarmored", "Unarmored", 0, 99, 0, 0, 0.0),
    ArmorDefinition("Leather Armor", "Light", 1, 4, 0, 0, 1.0),
    ArmorDefinition("Studded Leather", "Light", 2, 3, 0, 0, 1.0),
    ArmorDefinition("Chain Mail", "Medium", 4, 1, -2, -5, 2.0),
    ArmorDefinition("Full Plate", "Heavy", 6, 0, -3, -10, 4.0)
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
    InventoryItem("Leather Armor", "Armor", 1, 1.0, "🥋", "Light armor.", "AC +1 • Dex cap +4"),
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
    val xp: Int = 0,
    val ancestry: String = "Human",
    val heritage: String = "Versatile Human",
    val background: String = "Hunter",
    val className: String = "Ranger",
    val size: String = "Medium",
    val alignment: String = "Neutral Good",
    val traits: String = "Human, Humanoid",
    val deity: String = "None",
    val level: Int = 3,
    val heroPoints: Int = 1,
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
    val skillProfs: Map<String, Proficiency> = skillDefinitions.associate { definition ->
        definition.name to when (definition.name) {
            "Acrobatics", "Athletics", "Nature", "Stealth", "Survival" -> Proficiency.TRAINED
            else -> Proficiency.UNTRAINED
        }
    },
    val armorName: String = "Leather Armor",
    val meleeWeapon: String = "Longsword",
    val rangedWeapon: String = "Shortbow",
    val speed: Int = 25,
    val movementNotes: String = "",
    val senses: String = "Normal vision",
    val languages: String = "Common",
    val resistances: String = "",
    val conditions: String = "",
    val notes: String = "",
    val ancestryFeats: List<String> = listOf("Natural Ambition"),
    val classFeats: List<String> = listOf("Hunted Shot"),
    val skillFeats: List<String> = listOf("Assurance (Survival)"),
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
    val actionsAndActivities: String = "Hunted Shot",
    val freeActionsAndReactions: String = "",
    val campaignNotes: String = "",
    val magicTradition: String = "Primal",
    val castingType: String = "Prepared",
    val spellcastingAbility: Ability = Ability.WIS,
    val spellProf: Proficiency = Proficiency.TRAINED,
    val focusCurrent: Int = 1,
    val focusMax: Int = 1,
    val spellSlots: Map<Int, Int> = emptyMap(),
    val spells: Map<Int, List<String>> = emptyMap()
) {
    fun abilityModifier(ability: Ability): Int =
        Math.floorDiv((abilities[ability] ?: 10) - 10, 2)

    fun skillBonus(skill: SkillDefinition): Int =
        abilityModifier(skill.ability) + (skillProfs[skill.name] ?: Proficiency.UNTRAINED).bonus(level)

    fun saveBonus(ability: Ability, proficiency: Proficiency): Int =
        abilityModifier(ability) + proficiency.bonus(level)

    fun perception(): Int =
        abilityModifier(Ability.WIS) + perceptionProf.bonus(level)
    fun classDc(): Int =
        10 + abilityModifier(keyAbility) + classDcProf.bonus(level)

    fun armor(): ArmorDefinition =
        armorCatalog.firstOrNull { it.name == armorName } ?: armorCatalog.first()

    fun ac(): Int {
        val selectedArmor = armor()
        val dex = minOf(abilityModifier(Ability.DEX), selectedArmor.dexCap)
        val prof = armorProf[selectedArmor.category] ?: Proficiency.UNTRAINED
        return 10 + dex + prof.bonus(level) + selectedArmor.itemBonus
    }

    fun attackBonus(weapon: WeaponDefinition): Int {
        val prof = weaponProf[weapon.category] ?: Proficiency.UNTRAINED
        return abilityModifier(weapon.ability) + prof.bonus(level) + weapon.itemBonus
    }

    fun spellAttack(): Int =
        abilityModifier(spellcastingAbility) + spellProf.bonus(level)

    fun spellDc(): Int = 10 + spellAttack()
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
    listOf(CharacterState())
