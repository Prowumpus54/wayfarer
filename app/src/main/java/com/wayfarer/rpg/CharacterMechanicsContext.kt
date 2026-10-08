package com.wayfarer.rpg

import org.json.JSONArray
import org.json.JSONObject

/** No truncation: every recorded item/spell is retained. Strings are escaped as JSON data. */
object CharacterMechanicsContext {
    fun project(c: CharacterState): String {
        val root = JSONObject()
            .put("authority", "Android sheet/rules; explanation cannot override values")
            .put("ruleset", c.ruleset).put("name", c.characterName)
            .put("classes", JSONObject(c.effectivePf1ClassLevels().toSortedMap()))
            .put("abilities", JSONObject(c.abilities.mapKeys { it.key.name }.toSortedMap()))
            .put("hp", JSONObject().put("current", c.currentHp).put("max", c.maxHp).put("temporary", c.tempHp)
                .put("state", c.pf1HpState()).put("stable", c.pf1Stable).put("dead", c.pf1Dead))
            .put("conditions", c.conditions).put("resistances", c.resistances)
            .put("size", c.size).put("speed", c.speed).put("movement", c.movementNotes).put("senses", c.senses)
            .put("defense", JSONObject().put("armor", c.armorName).put("ac", c.ac()).put("touch", c.touchAc())
                .put("flatFooted", c.flatFootedAc()).put("fortitude", c.fortitudeSave())
                .put("reflex", c.reflexSave()).put("will", c.willSave()).put("cmd", c.cmd()))
            .put("combat", JSONObject().put("bab", c.baseAttackBonus()).put("cmb", c.cmb()).put("initiative", c.initiative()))
            .put("feats", JSONArray(MechanicsAssistant.feats(c)))
            .put("skills", JSONArray(skillDefinitions.map { skill -> JSONObject().put("name", skill.name)
                .put("bonus", c.skillBonus(skill)).put("ranks", c.pf1SkillRanks[skill.name] ?: 0)
                .put("usableUntrained", !skill.trainedOnly) }))
            .put("otherSkillRanks", JSONObject(c.pf1SkillRanks.filterKeys { name -> skillDefinitions.none { it.name == name } }))
            .put("skillMisc", JSONObject(c.pf1SkillMisc))
            .put("weapons", JSONArray(listOf(c.meleeWeapon, c.rangedWeapon).distinct().map { name ->
                val w = weaponCatalog.firstOrNull { it.name == name }
                JSONObject().put("name", name).put("resolved", w != null).apply {
                    if (w != null) put("attack", c.attackBonus(w)).put("fullAttack", JSONArray(c.iterativeAttackBonuses(w)))
                        .put("damage", w.damageDice).put("damageBonus", c.damageBonus(w)).put("type", w.damageType)
                        .put("criticalThreat", w.criticalThreatMin).put("criticalMultiplier", w.criticalMultiplier)
                        .put("rangeIncrementFt", w.rangeIncrementFt ?: JSONObject.NULL).put("traits", w.traits)
                }
            }))
            .put("inventory", JSONArray(c.inventory.map { JSONObject().put("name", it.name).put("quantity", it.quantity)
                .put("category", it.category).put("weight", it.weight).put("description", it.description).put("recordedMechanics", it.mechanics) }))
            .put("currency", JSONObject().put("cp", c.currencyCp).put("sp", c.currencySp).put("gp", c.currencyGp).put("pp", c.currencyPp))
            .put("spellcasting", JSONObject().put("type", c.castingType).put("tradition", c.magicTradition)
                .put("ability", c.spellcastingAbility.name).put("casterLevel", c.casterLevel()).put("spellAttack", c.spellAttack())
                .put("levels", JSONArray((c.spells.keys + c.spellSlots.keys + c.spellSlotsUsed.keys).sorted().map { level ->
                    JSONObject().put("level", level).put("recordedSpells", JSONArray(c.spells[level].orEmpty()))
                        .put("slots", c.spellSlots[level] ?: 0).put("used", c.spellSlotsUsed[level] ?: 0)
                        .put("remaining", c.spellSlotsRemaining(level)).put("sheetSaveDc", if (level >= 0) c.spellDc(level) else JSONObject.NULL)
                })).put("detailSource", "unavailable; no local PF1 spell-detail catalog")
                .put("preparationTracking", "level/name list only; exact per-spell remaining preparations unavailable"))
            .put("classResources", "unavailable: CharacterState has no PF1 class resource counters")
            .put("legacyFocus", JSONObject().put("current", c.focusCurrent).put("max", c.focusMax).put("pf1Validated", false))
            .put("actions", c.actionsAndActivities).put("freeActionsAndReactions", c.freeActionsAndReactions)
            .put("limitations", "Unknown classes/equipment, conditional feats, condition effects and target legality may be unresolved; do not infer missing mechanics.")
            .put("localReferences", JSONArray(listOf("Models.kt#CharacterState", "Pf1Rules.kt", "Pf1ActionEngine.kt")))
            .put("publishedRulesCitation", "source unavailable")
        return root.toString()
    }
}
