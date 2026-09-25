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
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove("character_json").apply()
    }

    private fun toJson(c: CharacterState): JSONObject {
        val json = JSONObject()
        json.put("playerName", c.playerName)
        json.put("characterName", c.characterName)
        json.put("xp", c.xp)
        json.put("ancestry", c.ancestry)
        json.put("heritage", c.heritage)
        json.put("background", c.background)
        json.put("className", c.className)
        json.put("level", c.level)
        json.put("maxHp", c.maxHp)
        json.put("currentHp", c.currentHp)
        json.put("heroPoints", c.heroPoints)
        json.put("keyAbility", c.keyAbility.name)
        json.put("armorName", c.armorName)
        json.put("meleeWeapon", c.meleeWeapon)
        json.put("rangedWeapon", c.rangedWeapon)
        json.put("speed", c.speed)
        json.put("languages", c.languages)
        json.put("notes", c.notes)
        json.put("appearance", c.appearance)
        json.put("attitude", c.attitude)
        json.put("beliefs", c.beliefs)
        json.put("likes", c.likes)
        json.put("dislikes", c.dislikes)
        json.put("genderPronouns", c.genderPronouns)

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
            xp = json.optInt("xp", 0),
            ancestry = json.optString("ancestry", base.ancestry),
            heritage = json.optString("heritage", base.heritage),
            background = json.optString("background", base.background),
            className = json.optString("className", base.className),
            level = json.optInt("level", 1).coerceIn(1, 20),
            maxHp = json.optInt("maxHp", base.maxHp),
            currentHp = json.optInt("currentHp", base.maxHp),
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
            languages = json.optString("languages", base.languages),
            notes = json.optString("notes"),

            appearance = json.optString("appearance"),
            attitude = json.optString("attitude"),
            beliefs = json.optString("beliefs"),
            likes = json.optString("likes"),
            dislikes = json.optString("dislikes"),
            genderPronouns = json.optString("genderPronouns"),
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
            spells = spells
        )
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
