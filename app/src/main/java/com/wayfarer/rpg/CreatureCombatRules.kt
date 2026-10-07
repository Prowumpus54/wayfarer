package com.wayfarer.rpg

import org.json.JSONObject

data class CreatureAttackProfile(
    val name: String,
    val attackBonus: Int,
    val damageDice: String,
    val damageType: String,
    val rangeFt: Int? = null,
    val criticalThreatMin: Int = 20,
    val criticalMultiplier: Int = 2
)

data class CreatureCombatProfile(
    val ruleRef: String,
    val name: String,
    val level: Int,
    val maxHp: Int,
    val armorClass: Int,
    val initiativeBonus: Int,
    val fortitude: Int,
    val reflex: Int,
    val will: Int,
    val attacks: List<CreatureAttackProfile>,
    val ruleset: GameRuleset = GameRuleset.PF2E_ADAPTED,
    val challengeRating: String = "",
    val xpValue: Int = 0,
    val touchArmorClass: Int = armorClass,
    val flatFootedArmorClass: Int = armorClass,
    val baseAttackBonus: Int = 0,
    val cmb: Int = 0,
    val cmd: Int = 10
) {
    val primaryAttack: CreatureAttackProfile?
        get() = attacks.firstOrNull()
}

object CreatureCombatProfileParser {
    fun parse(ruleRef: String, rawJson: String): CreatureCombatProfile? =
        runCatching {
            val root = JSONObject(rawJson)
            val system = root.getJSONObject("system")
            val attributes = system.getJSONObject("attributes")
            val hp = attributes.getJSONObject("hp").optInt("max", 0)
            val ac = attributes.getJSONObject("ac").optInt("value", 0)
            if (hp <= 0 || ac <= 0) return null

            val saves = system.optJSONObject("saves")
            val perception = system.optJSONObject("perception")
                ?.optInt("mod", 0) ?: 0
            val initiative = when (
                system.optJSONObject("initiative")
                    ?.optString("statistic")
                    ?.lowercase()
            ) {
                "perception", "", null -> perception
                else -> perception
            }

            val attacks = buildList {
                val items = root.optJSONArray("items") ?: return@buildList
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    if (!item.optString("type").equals("melee", true)) continue
                    val attackSystem = item.optJSONObject("system") ?: continue
                    val bonus = attackSystem.optJSONObject("bonus")
                        ?.optInt("value", Int.MIN_VALUE)
                        ?: Int.MIN_VALUE
                    if (bonus == Int.MIN_VALUE) continue
                    val damageRolls = attackSystem.optJSONObject("damageRolls")
                    val firstDamage = damageRolls?.keys()?.asSequence()
                        ?.mapNotNull { key -> damageRolls.optJSONObject(key) }
                        ?.firstOrNull()
                    val damage = firstDamage?.optString("damage").orEmpty()
                    if (damage.isBlank()) continue
                    val range = attackSystem.optJSONObject("range")
                        ?.optInt("increment", 0)
                        ?.takeIf { it > 0 }
                    add(
                        CreatureAttackProfile(
                            name = item.optString("name", "Attack"),
                            attackBonus = bonus,
                            damageDice = damage,
                            damageType = firstDamage
                                ?.optString("damageType")
                                .orEmpty(),
                            rangeFt = range
                        )
                    )
                }
            }

            CreatureCombatProfile(
                ruleRef = ruleRef,
                name = root.optString("name", "Creature"),
                level = system.optJSONObject("details")
                    ?.optJSONObject("level")
                    ?.optInt("value", 0) ?: 0,
                maxHp = hp,
                armorClass = ac,
                initiativeBonus = initiative,
                fortitude = saves?.optJSONObject("fortitude")
                    ?.optInt("value", 0) ?: 0,
                reflex = saves?.optJSONObject("reflex")
                    ?.optInt("value", 0) ?: 0,
                will = saves?.optJSONObject("will")
                    ?.optInt("value", 0) ?: 0,
                attacks = attacks
            )
        }.getOrNull()
}
