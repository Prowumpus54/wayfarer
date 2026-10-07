package com.wayfarer.rpg

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CharacterStore(
    context: Context,
    campaignId: String
) {
    private val prefs = context.getSharedPreferences(
        "wayfarer_character_$campaignId",
        Context.MODE_PRIVATE
    )

    fun hasCharacter(): Boolean = prefs.contains("character_json")

    fun save(character: CharacterState) {
        prefs.edit()
            .putString("character_json", toJson(character).toString())
            .apply()
    }

    fun load(): CharacterState? {
        val raw = prefs.getString("character_json", null) ?: return null
        val parsed = runCatching { fromJson(JSONObject(raw)) }.getOrNull()
            ?: return null
        val migrated = Pf1CharacterMigration.migrate(parsed)
        if (migrated != parsed) save(migrated)
        return migrated
    }

    fun clear() {
        prefs.edit().remove("character_json").apply()
    }

    private fun toJson(c: CharacterState): JSONObject {
        val json = JSONObject()
        json.put("playerName", c.playerName)
        json.put("characterName", c.characterName)
        json.put("ruleset", c.ruleset)
        json.put("pf1SchemaVersion", c.pf1SchemaVersion)
        json.put("pf1ExperienceTrack", c.pf1ExperienceTrack)
        json.put("xp", c.xp)
        json.put("ancestry", c.ancestry)
        json.put("heritage", c.heritage)
        json.put("background", c.background)
        json.put("className", c.className)
        json.put("level", c.level)
        json.put("maxHp", c.maxHp)
        json.put("currentHp", c.currentHp)
        json.put("tempHp", c.tempHp)
        json.put("dying", c.dying)
        json.put("wounded", c.wounded)
        json.put("pf1Stable", c.pf1Stable)
        json.put("pf1Dead", c.pf1Dead)
        json.put("conditions", c.conditions)
        json.put("heroPoints", c.heroPoints)
        json.put("size", c.size)
        json.put("alignment", c.alignment)
        json.put("traits", c.traits)
        json.put("deity", c.deity)
        json.put("keyAbility", c.keyAbility.name)
        json.put("armorName", c.armorName)
        json.put("meleeWeapon", c.meleeWeapon)
        json.put("rangedWeapon", c.rangedWeapon)
        json.put("speed", c.speed)
        json.put("movementNotes", c.movementNotes)
        json.put("senses", c.senses)
        json.put("languages", c.languages)
        json.put("resistances", c.resistances)
        json.put("notes", c.notes)
        json.put("appearance", c.appearance)
        json.put("attitude", c.attitude)
        json.put("beliefs", c.beliefs)
        json.put("likes", c.likes)
        json.put("dislikes", c.dislikes)
        json.put("genderPronouns", c.genderPronouns)
        json.put("actionsAndActivities", c.actionsAndActivities)
        json.put("freeActionsAndReactions", c.freeActionsAndReactions)
        json.put("campaignNotes", c.campaignNotes)

        json.put("pf1ArmorEnhancement", c.pf1ArmorEnhancement)
        json.put("pf1ShieldBonus", c.pf1ShieldBonus)
        json.put("pf1ShieldEnhancement", c.pf1ShieldEnhancement)
        json.put("pf1NaturalArmor", c.pf1NaturalArmor)
        json.put("pf1DeflectionBonus", c.pf1DeflectionBonus)
        json.put("pf1DodgeBonus", c.pf1DodgeBonus)
        json.put("pf1MiscAcBonus", c.pf1MiscAcBonus)
        json.put("pf1AttackMiscBonus", c.pf1AttackMiscBonus)
        json.put("pf1DamageMiscBonus", c.pf1DamageMiscBonus)
        json.put("pf1InitiativeMisc", c.pf1InitiativeMisc)
        json.put("pf1CmbMisc", c.pf1CmbMisc)
        json.put("pf1CmdMisc", c.pf1CmdMisc)
        json.put("pf1SpellSaveMisc", c.pf1SpellSaveMisc)

        json.put("pf1ClassLevels", intObject(c.pf1ClassLevels))
        json.put("pf1SkillRanks", intObject(c.pf1SkillRanks))
        json.put("pf1SkillMisc", intObject(c.pf1SkillMisc))
        json.put("pf1SaveMisc", intObject(c.pf1SaveMisc))

        val abilities = JSONObject()
        c.abilities.forEach { (ability, score) ->
            abilities.put(ability.name, score)
        }
        json.put("abilities", abilities)
        json.put("ancestryFeats", JSONArray(c.ancestryFeats))
        json.put("classFeats", JSONArray(c.classFeats))
        json.put("skillFeats", JSONArray(c.skillFeats))
        json.put("generalFeats", JSONArray(c.generalFeats))
        json.put("bonusFeats", JSONArray(c.bonusFeats))
        json.put("magicTradition", c.magicTradition)
        json.put("castingType", c.castingType)
        json.put("spellcastingAbility", c.spellcastingAbility.name)
        json.put("focusCurrent", c.focusCurrent)
        json.put("focusMax", c.focusMax)
        json.put("currencyCp", c.currencyCp)
        json.put("currencySp", c.currencySp)
        json.put("currencyGp", c.currencyGp)
        json.put("currencyPp", c.currencyPp)

        val spellSlots = JSONObject()
        c.spellSlots.forEach { (level, count) ->
            spellSlots.put(level.toString(), count)
        }
        json.put("spellSlots", spellSlots)

        val spellSlotsUsed = JSONObject()
        c.spellSlotsUsed.forEach { (level, count) ->
            spellSlotsUsed.put(level.toString(), count)
        }
        json.put("spellSlotsUsed", spellSlotsUsed)

        val spells = JSONObject()
        c.spells.forEach { (level, names) ->
            spells.put(level.toString(), JSONArray(names))
        }
        json.put("spells", spells)

        val items = JSONArray()
        c.inventory.forEach { item ->
            items.put(
                JSONObject()
                    .put("name", item.name)
                    .put("category", item.category)
                    .put("quantity", item.quantity)
                    .put("weight", item.weight)
                    .put("icon", item.icon)
                    .put("description", item.description)
                    .put("mechanics", item.mechanics)
            )
        }
        json.put("inventory", items)
        return json
    }

    private fun fromJson(json: JSONObject): CharacterState {
        val base = CharacterState(level = 1)
        val abilitiesJson = json.optJSONObject("abilities")
        val abilities = Ability.entries.associateWith { ability ->
            abilitiesJson?.optInt(
                ability.name,
                base.abilities[ability] ?: 10
            ) ?: (base.abilities[ability] ?: 10)
        }

        val inventory = json.optJSONArray("inventory")
            ?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        add(
                            InventoryItem(
                                name = item.optString("name"),
                                category = item.optString("category", "Other"),
                                quantity = item.optInt("quantity", 1),
                                weight = item.optDouble("weight", 0.0),
                                icon = item.optString("icon", "🎒"),
                                description = item.optString("description"),
                                mechanics = item.optString("mechanics")
                            )
                        )
                    }
                }
            }.orEmpty()

        val spellSlots = intMap(json.optJSONObject("spellSlots"))
        val spellSlotsUsed = intMap(json.optJSONObject("spellSlotsUsed"))
        val pf1ClassLevels = stringIntMap(json.optJSONObject("pf1ClassLevels"))
        val pf1SkillRanks = stringIntMap(json.optJSONObject("pf1SkillRanks"))
        val pf1SkillMisc = stringIntMap(json.optJSONObject("pf1SkillMisc"))
        val pf1SaveMisc = stringIntMap(json.optJSONObject("pf1SaveMisc"))

        val spellsJson = json.optJSONObject("spells")
        val spells = mutableMapOf<Int, List<String>>()
        if (spellsJson != null) {
            spellsJson.keys().forEach { key ->
                key.toIntOrNull()?.let { level ->
                    spells[level] = stringList(spellsJson.optJSONArray(key))
                }
            }
        }

        return base.copy(
            playerName = json.optString("playerName"),
            characterName = json.optString("characterName", "Hero"),
            ruleset = json.optString(
                "ruleset",
                GameRuleset.PF2E_ADAPTED.wireName
            ),
            pf1SchemaVersion = json.optInt("pf1SchemaVersion", 0),
            pf1ExperienceTrack = json.optString(
                "pf1ExperienceTrack",
                Pf1ExperienceTrack.MEDIUM.name.lowercase()
            ),
            xp = json.optInt("xp", 0),
            ancestry = json.optString("ancestry", base.ancestry),
            heritage = json.optString("heritage", base.heritage),
            background = json.optString("background", base.background),
            className = json.optString("className", base.className),
            level = json.optInt("level", 1).coerceIn(1, 20),
            size = json.optString("size", base.size),
            alignment = json.optString("alignment", base.alignment),
            traits = json.optString("traits", base.traits),
            deity = json.optString("deity", base.deity),
            pf1ClassLevels = pf1ClassLevels,
            pf1SkillRanks = pf1SkillRanks,
            pf1SkillMisc = pf1SkillMisc,
            pf1ArmorEnhancement = json.optInt("pf1ArmorEnhancement", 0),
            pf1ShieldBonus = json.optInt("pf1ShieldBonus", 0),
            pf1ShieldEnhancement = json.optInt("pf1ShieldEnhancement", 0),
            pf1NaturalArmor = json.optInt("pf1NaturalArmor", 0),
            pf1DeflectionBonus = json.optInt("pf1DeflectionBonus", 0),
            pf1DodgeBonus = json.optInt("pf1DodgeBonus", 0),
            pf1MiscAcBonus = json.optInt("pf1MiscAcBonus", 0),
            pf1AttackMiscBonus = json.optInt("pf1AttackMiscBonus", 0),
            pf1DamageMiscBonus = json.optInt("pf1DamageMiscBonus", 0),
            pf1SaveMisc = pf1SaveMisc,
            pf1InitiativeMisc = json.optInt("pf1InitiativeMisc", 0),
            pf1CmbMisc = json.optInt("pf1CmbMisc", 0),
            pf1CmdMisc = json.optInt("pf1CmdMisc", 0),
            pf1SpellSaveMisc = json.optInt("pf1SpellSaveMisc", 0),
            maxHp = json.optInt("maxHp", base.maxHp),
            currentHp = json.optInt("currentHp", base.maxHp),
            tempHp = json.optInt("tempHp", base.tempHp),
            dying = json.optInt("dying", base.dying),
            wounded = json.optInt("wounded", base.wounded),
            pf1Stable = json.optBoolean("pf1Stable", false),
            pf1Dead = json.optBoolean("pf1Dead", false),
            conditions = json.optString("conditions", base.conditions),
            heroPoints = json.optInt("heroPoints", 1),
            abilities = abilities,
            keyAbility = enumValue(
                json.optString("keyAbility"),
                base.keyAbility
            ),
            armorName = json.optString("armorName", base.armorName),
            meleeWeapon = json.optString("meleeWeapon", base.meleeWeapon),
            rangedWeapon = json.optString("rangedWeapon", base.rangedWeapon),
            speed = json.optInt("speed", base.speed),
            movementNotes = json.optString("movementNotes", base.movementNotes),
            senses = json.optString("senses", base.senses),
            languages = json.optString("languages", base.languages),
            resistances = json.optString("resistances", base.resistances),
            notes = json.optString("notes"),

            appearance = json.optString("appearance"),
            attitude = json.optString("attitude"),
            beliefs = json.optString("beliefs"),
            likes = json.optString("likes"),
            dislikes = json.optString("dislikes"),
            genderPronouns = json.optString("genderPronouns"),
            actionsAndActivities = json.optString(
                "actionsAndActivities",
                base.actionsAndActivities
            ),
            freeActionsAndReactions = json.optString(
                "freeActionsAndReactions",
                base.freeActionsAndReactions
            ),
            campaignNotes = json.optString("campaignNotes", base.campaignNotes),
            ancestryFeats = stringList(json.optJSONArray("ancestryFeats")),
            classFeats = stringList(json.optJSONArray("classFeats")),
            skillFeats = stringList(json.optJSONArray("skillFeats")),
            generalFeats = stringList(json.optJSONArray("generalFeats")),
            bonusFeats = stringList(json.optJSONArray("bonusFeats")),
            inventory = inventory.ifEmpty { base.inventory },
            magicTradition = json.optString(
                "magicTradition",
                base.magicTradition
            ),
            castingType = json.optString("castingType", base.castingType),
            spellcastingAbility = enumValue(
                json.optString("spellcastingAbility"),
                base.spellcastingAbility
            ),
            focusCurrent = json.optInt("focusCurrent", base.focusCurrent),
            focusMax = json.optInt("focusMax", base.focusMax),
            spellSlots = spellSlots,
            spellSlotsUsed = spellSlotsUsed,
            spells = spells,
            currencyCp = json.optInt("currencyCp", base.currencyCp),
            currencySp = json.optInt("currencySp", base.currencySp),
            currencyGp = json.optInt("currencyGp", base.currencyGp),
            currencyPp = json.optInt("currencyPp", base.currencyPp)
        )
    }

    private fun intObject(values: Map<String, Int>): JSONObject =
        JSONObject().apply {
            values.forEach { (key, value) -> put(key, value) }
        }

    private fun stringIntMap(json: JSONObject?): Map<String, Int> {
        if (json == null) return emptyMap()
        val out = mutableMapOf<String, Int>()
        json.keys().forEach { key ->
            out[key] = json.optInt(key, 0)
        }
        return out
    }

    private fun intMap(json: JSONObject?): Map<Int, Int> {
        if (json == null) return emptyMap()
        val out = mutableMapOf<Int, Int>()
        json.keys().forEach { key ->
            key.toIntOrNull()?.let { level ->
                out[level] = json.optInt(key, 0).coerceAtLeast(0)
            }
        }
        return out
    }

    private fun stringList(array: JSONArray?): List<String> =
        buildList {
            if (array != null) {
                for (index in 0 until array.length()) {
                    array.optString(index)
                        .takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
            }
        }

    private inline fun <reified T : Enum<T>> enumValue(
        raw: String,
        fallback: T
    ): T = enumValues<T>().firstOrNull {
        it.name.equals(raw, true)
    } ?: fallback
}
