package com.wayfarer.rpg

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

data class CharacterBrief(
    val name: String,
    val pronouns: String,
    val concept: String,
    val personality: String,
    val combatStyle: String,
    val magicPreference: String,
    val complexity: String
)

class CharacterArchitect {
    private val modelNames = listOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite"
    )

    suspend fun create(brief: CharacterBrief): CharacterState {
        val raw = generateWithFallback(buildPrompt(brief))
        return parseCharacter(raw, brief)
    }
    private fun buildPrompt(brief: CharacterBrief): String = """
You are Wayfarer's character architect.

Create one legal, easy-to-play Pathfinder 2e-adapted level 1 hero.
Favor the player's fantasy over optimization, but make the build coherent.
The player does not want to manage rules manually.

PLAYER BRIEF
Name: ${brief.name.ifBlank { "Choose a fitting name" }}
Pronouns: ${brief.pronouns.ifBlank { "unspecified" }}
Concept: ${brief.concept}
Personality: ${brief.personality}
Combat style: ${brief.combatStyle}
Magic preference: ${brief.magicPreference}
Complexity: ${brief.complexity}

Supported weapons: Unarmed, Dagger, Longsword, Shortbow, Longbow.
Supported armor: Unarmored, Leather Armor, Studded Leather, Chain Mail, Full Plate.

Return JSON only:
{
  "name": "",
  "ancestry": "",
  "heritage": "",
  "background": "",
  "className": "",
  "maxHp": 18,
  "keyAbility": "DEX",
  "abilities": {"STR":10,"DEX":10,"CON":10,"INT":10,"WIS":10,"CHA":10},
  "armorName": "",
  "meleeWeapon": "",
  "rangedWeapon": "",
  "ancestryFeats": [],
  "classFeats": [],
  "skillFeats": [],
  "generalFeats": [],
  "spells": {"0": [], "1": []},
  "magicTradition": "",
  "castingType": "",
  "spellcastingAbility": "WIS",
  "inventory": [
    {
      "name":"",
      "category":"",
      "quantity":1,
      "weight":0.0,
      "icon":"🎒",
      "description":"",
      "mechanics":""
    }
  ],
  "appearance": "",
  "attitude": "",
  "beliefs": "",
  "likes": "",
  "dislikes": "",
  "notes": ""
}

Rules:
- level is always 1.
- ability scores should fit a level 1 PF2e-style hero.
- give a small practical starter inventory.
- choose feats and spells matching the concept.
- nonmagical heroes may have no spells.
- notes should be one short sentence describing how the hero feels to play.
""".trimIndent()

    private suspend fun generateWithFallback(prompt: String): String {
        var last: Exception? = null
        for (modelName in modelNames) {
            repeat(2) { attempt ->
                try {
                    val model = Firebase
                        .ai(backend = GenerativeBackend.googleAI())
                        .generativeModel(modelName)
                    return model.generateContent(prompt).text.orEmpty()
                } catch (error: Exception) {
                    last = error
                    if (attempt == 0) delay(700)
                }
            }
        }
        throw last ?: IllegalStateException("Character AI did not respond.")
    }
    private fun parseCharacter(
        raw: String,
        brief: CharacterBrief
    ): CharacterState {
        val clean = raw.trim()
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        require(start >= 0 && end > start) {
            "Character AI returned invalid data."
        }

        val json = JSONObject(clean.substring(start, end + 1))
        val base = CharacterState(level = 1)
        val abilityJson = json.optJSONObject("abilities")
        val abilities = Ability.entries.associateWith { ability ->
            abilityJson?.optInt(
                ability.name,
                base.abilities[ability] ?: 10
            )?.coerceIn(8, 18)
                ?: (base.abilities[ability] ?: 10)
        }

        val items = json.optJSONArray("inventory")
            ?.let(::parseInventory)
            .orEmpty()
            .ifEmpty { defaultInventory() }

        val spells = mutableMapOf<Int, List<String>>()
        json.optJSONObject("spells")?.let { spellsJson ->
            spellsJson.keys().forEach { key ->
                key.toIntOrNull()?.let { level ->
                    spells[level] = stringList(
                        spellsJson.optJSONArray(key)
                    )
                }
            }
        }

        val hp = json.optInt("maxHp", 18).coerceIn(12, 30)
        return base.copy(
            playerName = "",
            characterName = json.optString("name")
                .ifBlank { brief.name.ifBlank { "Hero" } },
            ancestry = json.optString("ancestry", base.ancestry),
            heritage = json.optString("heritage", base.heritage),
            background = json.optString("background", base.background),
            className = json.optString("className", base.className),
            level = 1,
            maxHp = hp,
            currentHp = hp,
            abilities = abilities,
            keyAbility = enumValue(
                json.optString("keyAbility"),
                base.keyAbility
            ),
            armorName = supportedArmor(
                json.optString("armorName"),
                base.armorName
            ),
            meleeWeapon = supportedWeapon(
                json.optString("meleeWeapon"),
                base.meleeWeapon
            ),
            rangedWeapon = supportedWeapon(
                json.optString("rangedWeapon"),
                base.rangedWeapon
            ),
            ancestryFeats = stringList(json.optJSONArray("ancestryFeats")),
            classFeats = stringList(json.optJSONArray("classFeats")),
            skillFeats = stringList(json.optJSONArray("skillFeats")),
            generalFeats = stringList(json.optJSONArray("generalFeats")),
            inventory = items,
            appearance = json.optString("appearance"),
            attitude = json.optString("attitude"),
            beliefs = json.optString("beliefs"),
            likes = json.optString("likes"),
            dislikes = json.optString("dislikes"),
            genderPronouns = brief.pronouns,
            notes = json.optString("notes"),
            magicTradition = json.optString("magicTradition"),
            castingType = json.optString("castingType"),
            spellcastingAbility = enumValue(
                json.optString("spellcastingAbility"),
                base.spellcastingAbility
            ),
            spells = spells
        )
    }
    private fun parseInventory(array: JSONArray): List<InventoryItem> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                if (name.isBlank()) continue
                add(
                    InventoryItem(
                        name = name,
                        category = item.optString("category", "Other"),
                        quantity = item.optInt("quantity", 1).coerceIn(1, 99),
                        weight = item.optDouble("weight", 0.0)
                            .coerceIn(0.0, 20.0),
                        icon = item.optString("icon", "🎒"),
                        description = item.optString("description"),
                        mechanics = item.optString("mechanics")
                    )
                )
            }
        }

    private fun stringList(array: JSONArray?): List<String> =
        buildList {
            if (array != null) {
                for (index in 0 until array.length()) {
                    array.optString(index)
                        .trim()
                        .takeIf { it.isNotBlank() }
                        ?.let(::add)
                }
            }
        }
    private fun supportedWeapon(raw: String, fallback: String): String {
        val candidate = raw.trim()
        return weaponCatalog.firstOrNull {
            it.name.equals(candidate, true)
        }?.name ?: fallback
    }

    private fun supportedArmor(raw: String, fallback: String): String {
        val candidate = raw.trim()
        return armorCatalog.firstOrNull {
            it.name.equals(candidate, true)
        }?.name ?: fallback
    }

    private inline fun <reified T : Enum<T>> enumValue(
        raw: String,
        fallback: T
    ): T = enumValues<T>().firstOrNull {
        it.name.equals(raw.trim(), true)
    } ?: fallback
}
