package com.wayfarer.rpg

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

data class InventoryPlan(
    val summary: String,
    val items: List<InventoryItem>,
    val transcriptId: String = ""
)

class InventoryAssistant {
    private val modelNames = listOf(
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite"
    )

    suspend fun manage(
        character: CharacterState,
        goal: String
    ): InventoryPlan {
        val prompt = buildPrompt(character, goal)
        val trace = LoreWiseTranscripts.trace("inventory_assistant", prompt, goal)
        val raw = observedCloudText(trace, prompt)
        return try {
            parse(raw, character.inventory).copy(transcriptId = trace.id).also { plan ->
                trace.decision("proposal", "Replacement inventory parsed; requires user confirmation")
                LoreWiseTranscripts.stateChange(trace.id, org.json.JSONObject()
                    .put("kind", "inventory_proposal").put("saved", false).put("items", plan.items.toString()))
            }
        } catch (error: Exception) {
            trace.decision("parse_error", error::class.java.simpleName)
            trace.finish(raw, "parse_error")
            throw error
        }
    }
    private fun buildPrompt(
        character: CharacterState,
        goal: String
    ): String {
        val currentItems = character.inventory.joinToString("\n") {
            "- " + it.name + " x" + it.quantity +
                " [" + it.category + "] " + it.mechanics
        }

        return """
You are LoreWise's inventory assistant.

Help a casual player manage this ${if (character.isPf1()) "Pathfinder 1e" else "legacy PF2e-adapted"} character's pack.
Never remove a quest item. Keep essential weapons, armor, class tools,
spellcasting needs, food, light, and basic adventuring supplies unless the
player explicitly asks otherwise.

CHARACTER
Name: ${character.characterName}
Class: ${character.className}
Level: ${character.level}
STR: ${character.abilities[Ability.STR]}
Weapons: ${character.meleeWeapon}, ${character.rangedWeapon}
Armor: ${character.armorName}

PLAYER GOAL
$goal

CURRENT INVENTORY
$currentItems

Return JSON only:
{
  "summary": "one short explanation",
  "items": [
    {
      "name":"",
      "category":"",
      "quantity":1,
      "weight":0.0,
      "icon":"🎒",
      "description":"",
      "mechanics":""
    }
  ]
}

Rules:
- preserve named weapons and armor unless the goal explicitly says otherwise.
- preserve anything whose category contains Quest.
- quantities must be practical.
- do not invent powerful magic items for a level 1 character.
- this is a proposed replacement inventory; the player confirms before saving.
""".trimIndent()
    }

    private fun parse(
        raw: String,
        fallback: List<InventoryItem>
    ): InventoryPlan {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        require(start >= 0 && end > start) {
            "Inventory AI returned invalid data."
        }

        val json = JSONObject(raw.substring(start, end + 1))
        val array = json.optJSONArray("items") ?: JSONArray()
        val items = buildList {
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

        return InventoryPlan(
            summary = json.optString(
                "summary",
                "Suggested inventory update."
            ),
            items = items.ifEmpty { fallback }
        )
    }
}
