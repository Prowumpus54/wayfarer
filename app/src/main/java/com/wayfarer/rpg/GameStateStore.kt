package com.wayfarer.rpg

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class GameStateStore(
    context: Context,
    campaignId: String
) {
    private val prefs = context.getSharedPreferences(
        "wayfarer_runtime_$campaignId",
        Context.MODE_PRIVATE
    )

    fun load(): CampaignRuntimeState {
        val raw = prefs.getString("runtime_json", null) ?: return CampaignRuntimeState()
        return runCatching { fromJson(JSONObject(raw)) }
            .getOrDefault(CampaignRuntimeState())
    }

    fun save(state: CampaignRuntimeState) {
        prefs.edit()
            .putString("runtime_json", toJson(state).toString())
            .apply()
    }

    private fun toJson(state: CampaignRuntimeState): JSONObject =
        JSONObject()
            .put("encounter", state.activeEncounter?.let(::encounterToJson))
            .put("challenge", state.activeChallenge?.let(::challengeToJson))
            .put("flags", JSONArray(state.worldFlags.toList()))

    private fun fromJson(json: JSONObject): CampaignRuntimeState =
        CampaignRuntimeState(
            activeEncounter = json.optJSONObject("encounter")?.let(::encounterFromJson),
            activeChallenge = json.optJSONObject("challenge")?.let(::challengeFromJson),
            worldFlags = stringList(json.optJSONArray("flags")).toSet()
        )

    private fun encounterToJson(value: EncounterState): JSONObject {
        val creatures = JSONArray()
        value.creatures.forEach { creature ->
            creatures.put(
                JSONObject()
                    .put("id", creature.id)
                    .put("name", creature.name)
                    .put("ruleRef", creature.ruleRef)
                    .put("maxHp", creature.maxHp)
                    .put("currentHp", creature.currentHp)
                    .put("armorClass", creature.armorClass)
                    .put("touchArmorClass", creature.touchArmorClass)
                    .put("flatFootedArmorClass", creature.flatFootedArmorClass)
                    .put("cmb", creature.cmb)
                    .put("cmd", creature.cmd)
                    .put("initiative", creature.initiative)
                    .put("initiativeBonus", creature.initiativeBonus)
                    .put("attackName", creature.attackName)
                    .put("attackBonus", creature.attackBonus)
                    .put("damageDice", creature.damageDice)
                    .put("damageType", creature.damageType)
                    .put("criticalThreatMin", creature.criticalThreatMin)
                    .put("criticalMultiplier", creature.criticalMultiplier)
                    .put("fortitude", creature.fortitude)
                    .put("reflex", creature.reflex)
                    .put("will", creature.will)
                    .put("xpValue", creature.xpValue)
                    .put("conditions", JSONArray(creature.conditions))
                    .put("status", creature.status.name)
                    .put("statsResolved", creature.statsResolved)
            )
        }

        val loot = JSONArray()
        value.loot.forEach { item ->
            loot.put(
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

        val initiative = JSONArray()
        value.initiativeOrder.forEach { turn ->
            initiative.put(
                JSONObject()
                    .put("actorType", turn.actorType.name)
                    .put("actorId", turn.actorId)
                    .put("name", turn.name)
                    .put("initiative", turn.initiative)
                    .put("initiativeBonus", turn.initiativeBonus)
            )
        }

        return JSONObject()
            .put("id", value.id)
            .put("name", value.name)
            .put("location", value.location)
            .put("sourceEncounterId", value.sourceEncounterId)
            .put("round", value.round)
            .put("initiativeOrder", initiative)
            .put("currentTurnIndex", value.currentTurnIndex)
            .put("creatures", creatures)
            .put("loot", loot)
            .put("lootCp", value.lootCp)
            .put("lootSp", value.lootSp)
            .put("lootGp", value.lootGp)
            .put("lootPp", value.lootPp)
            .put("status", value.status.name)
            .put("xpAwarded", value.xpAwarded)
    }

    private fun encounterFromJson(json: JSONObject): EncounterState {
        val creaturesJson = json.optJSONArray("creatures")
        val creatures = buildList {
            if (creaturesJson != null) {
                for (index in 0 until creaturesJson.length()) {
                    val item = creaturesJson.optJSONObject(index) ?: continue
                    val maxHp = item.optInt("maxHp", 1).coerceAtLeast(1)
                    add(
                        EncounterCreatureState(
                            id = item.optString("id").ifBlank {
                                java.util.UUID.randomUUID().toString()
                            },
                            name = item.optString("name", "Creature"),
                            ruleRef = item.optString("ruleRef"),
                            maxHp = maxHp,
                            currentHp = item.optInt("currentHp", maxHp)
                                .coerceIn(0, maxHp),
                            armorClass = item.optInt("armorClass", 10)
                                .coerceIn(1, 99),
                            touchArmorClass = item.optInt(
                                "touchArmorClass",
                                item.optInt("armorClass", 10)
                            ).coerceIn(1, 99),
                            flatFootedArmorClass = item.optInt(
                                "flatFootedArmorClass",
                                item.optInt("armorClass", 10)
                            ).coerceIn(1, 99),
                            cmb = item.optInt("cmb", 0),
                            cmd = item.optInt("cmd", 10),
                            initiative = if (item.isNull("initiative")) null
                                else item.optInt("initiative"),
                            initiativeBonus = item.optInt("initiativeBonus", 0),
                            attackName = item.optString("attackName"),
                            attackBonus = if (item.isNull("attackBonus")) null
                                else item.optInt("attackBonus"),
                            damageDice = item.optString("damageDice"),
                            damageType = item.optString("damageType"),
                            criticalThreatMin = item.optInt(
                                "criticalThreatMin",
                                20
                            ).coerceIn(2, 20),
                            criticalMultiplier = item.optInt(
                                "criticalMultiplier",
                                2
                            ).coerceIn(2, 4),
                            fortitude = item.optInt("fortitude", 0),
                            reflex = item.optInt("reflex", 0),
                            will = item.optInt("will", 0),
                            xpValue = item.optInt("xpValue", 0).coerceAtLeast(0),
                            conditions = stringList(item.optJSONArray("conditions")),
                            status = enumValue(
                                item.optString("status"),
                                CreatureStatus.ACTIVE
                            ),
                            statsResolved = item.optBoolean("statsResolved", false)
                        )
                    )
                }
            }
        }

        val lootJson = json.optJSONArray("loot")
        val loot = buildList {
            if (lootJson != null) {
                for (index in 0 until lootJson.length()) {
                    val item = lootJson.optJSONObject(index) ?: continue
                    add(
                        InventoryItem(
                            name = item.optString("name", "Item"),
                            category = item.optString("category", "Other"),
                            quantity = item.optInt("quantity", 1).coerceAtLeast(1),
                            weight = item.optDouble("weight", 0.0).coerceAtLeast(0.0),
                            icon = item.optString("icon", "🎒"),
                            description = item.optString("description"),
                            mechanics = item.optString("mechanics")
                        )
                    )
                }
            }
        }

        val initiativeJson = json.optJSONArray("initiativeOrder")
        val initiativeOrder = buildList {
            if (initiativeJson != null) {
                for (index in 0 until initiativeJson.length()) {
                    val item = initiativeJson.optJSONObject(index) ?: continue
                    add(
                        CombatTurnEntry(
                            actorType = enumValue(
                                item.optString("actorType"),
                                CombatActorType.CREATURE
                            ),
                            actorId = item.optString("actorId"),
                            name = item.optString("name"),
                            initiative = item.optInt("initiative", 0),
                            initiativeBonus = item.optInt("initiativeBonus", 0)
                        )
                    )
                }
            }
        }

        return EncounterState(
            id = json.optString("id").ifBlank {
                java.util.UUID.randomUUID().toString()
            },
            name = json.optString("name", "Encounter"),
            location = json.optString("location"),
            sourceEncounterId = json.optString("sourceEncounterId"),
            round = json.optInt("round", 1).coerceAtLeast(1),
            initiativeOrder = initiativeOrder,
            currentTurnIndex = json.optInt("currentTurnIndex", 0)
                .coerceIn(0, (initiativeOrder.size - 1).coerceAtLeast(0)),
            creatures = creatures,
            loot = loot,
            lootCp = json.optInt("lootCp", 0).coerceAtLeast(0),
            lootSp = json.optInt("lootSp", 0).coerceAtLeast(0),
            lootGp = json.optInt("lootGp", 0).coerceAtLeast(0),
            lootPp = json.optInt("lootPp", 0).coerceAtLeast(0),
            status = enumValue(
                json.optString("status"),
                EncounterStatus.ACTIVE
            ),
            xpAwarded = json.optBoolean("xpAwarded", false)
        )
    }

    private fun challengeToJson(value: ChallengeState): JSONObject =
        JSONObject()
            .put("id", value.id)
            .put("name", value.name)
            .put("description", value.description)
            .put("dc", value.dc)
            .put("progress", value.progress)
            .put("goal", value.goal)
            .put("xpValue", value.xpValue)
            .put("status", value.status.name)

    private fun challengeFromJson(json: JSONObject): ChallengeState =
        ChallengeState(
            id = json.optString("id").ifBlank {
                java.util.UUID.randomUUID().toString()
            },
            name = json.optString("name", "Challenge"),
            description = json.optString("description"),
            dc = json.optInt("dc", 0).coerceIn(0, 99),
            progress = json.optInt("progress", 0).coerceAtLeast(0),
            goal = json.optInt("goal", 1).coerceAtLeast(1),
            xpValue = json.optInt("xpValue", 0).coerceAtLeast(0),
            status = enumValue(
                json.optString("status"),
                ChallengeStatus.ACTIVE
            )
        )

    private fun stringList(array: JSONArray?): List<String> = buildList {
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
