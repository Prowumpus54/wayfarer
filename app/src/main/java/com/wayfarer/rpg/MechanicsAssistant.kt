package com.wayfarer.rpg

/** Read-only calculation; legal=null means target/turn/rules information is still needed. */
data class MechanicsPreview(
    val legal: Boolean?,
    val bonuses: List<Int> = emptyList(),
    val resourceCosts: Map<String, Int> = emptyMap(),
    val notes: List<String> = emptyList(),
    val reference: String
)

object MechanicsAssistant {
    private const val rules = "app/src/main/java/com/wayfarer/rpg/Pf1Rules.kt"
    private const val model = "app/src/main/java/com/wayfarer/rpg/Models.kt"
    private const val engine = "app/src/main/java/com/wayfarer/rpg/Pf1ActionEngine.kt"
    private const val missing = "Published PF1 citation/source unavailable in local content."

    fun feats(c: CharacterState): List<String> =
        (c.ancestryFeats + c.classFeats + c.skillFeats + c.generalFeats + c.bonusFeats).distinct()

    /** No narrative action is interpreted as a question or committed by this assistant. */
    fun answer(c: CharacterState, question: String): String? {
        val q = question.trim().lowercase()
        val asks = q.endsWith("?") || Regex("^(what|which|how|can i|do i|show|list|tell me)\\b").containsMatchIn(q)
        if (!asks) return null
        val sections = mutableListOf<String>()
        if (Regex("\\b(spells?|cast|slots?|spellcasting)\\b").containsMatchIn(q)) sections += spellSummary(c)
        if (Regex("\\b(ac|armor class|touch|flat[- ]footed)\\b").containsMatchIn(q)) sections += defense(c)
        if (Regex("\\b(saves?|saving throws?|fortitude|reflex|will save)\\b").containsMatchIn(q)) sections += saves(c)
        if (Regex("\\b(feats?)\\b").containsMatchIn(q)) sections +=
            "Recorded feats/features: ${feats(c).joinToString().ifBlank { "none recorded" }}. Applicability beyond validated actions is unavailable; recorded names do not automatically modify calculated bonuses."
        if (Regex("\\b(attacks?|bonuses|combat maneuvers?|cmb|cmd)\\b").containsMatchIn(q)) {
            sections += weapons(c)
            sections += "Full attacks require a full-round action; movement, target eligibility and feat-specific effects require context. CMB=${c.cmb()}; CMD=${c.cmd()}."
        }
        val skills = skillDefinitions.filter { q.contains(it.name.lowercase()) }
        if (skills.isNotEmpty() || Regex("\\bskills?\\b").containsMatchIn(q)) sections += skillSummary(c, skills.ifEmpty { skillDefinitions })
        if (Regex("\\b(resources?|hit points?|hp|conditions?|inventory|currency|gold)\\b").containsMatchIn(q)) sections += resources(c)
        if (sections.isEmpty()) return null
        if (!c.isPf1()) return "PF1 mechanics unavailable for this ruleset. $missing"
        return sections.joinToString("\n") + "\nLocal calculation references: $model; $rules; $engine. $missing"
    }

    fun spellSummary(c: CharacterState): String = buildString {
        append("Spells (${c.castingType}; stored known/prepared list, individual preparation counts unavailable): ")
        if (c.spells.values.all { it.isEmpty() }) append("none recorded.")
        c.spells.toSortedMap().forEach { (level, names) ->
            append("\nLevel $level: ${names.joinToString().ifBlank { "none recorded" }}")
            if (level > 0) append("; remaining ${c.spellSlotsRemaining(level)}/${c.spellSlots[level] ?: 0}")
            if (level == 0) append("; reusable cantrips/orisons (no slot expenditure)")
            if (level < 0) append("; legacy special resource, PF1 casting legality unavailable")
        }
        append("\nSpell slots: ")
        val levels = (c.spellSlots.keys + c.spellSlotsUsed.keys + c.spells.keys.filter { it > 0 }).sorted()
        append(levels.joinToString("; ") { "L$it remaining=${c.spellSlotsRemaining(it)}, total=${c.spellSlots[it] ?: 0}, used=${c.spellSlotsUsed[it] ?: 0}" }.ifBlank { "none recorded" })
        val profile = pf1ClassProfile(c.className)
        val classLevel = c.effectivePf1ClassLevels().entries.firstOrNull { it.key.equals(c.className, true) }?.value ?: c.level
        if (profile?.spellStartLevel != null && classLevel < profile.spellStartLevel) {
            append("\n${profile.name} spellcasting begins at class level ${profile.spellStartLevel}; current class level $classLevel.")
        }
        append("\nCast availability requires a recorded spell, available resource and legal casting ability/class progression. Targeting/range/save type/duration and spell-specific PF1 citation: unavailable in local PF1 content. Save DC by spell level uses Android spellDc; this does not establish whether a spell allows a save.")
    }

    private fun defense(c: CharacterState) = "AC=${c.ac()}; touch=${c.touchAc()}; flat-footed=${c.flatFootedAc()}; armor=${c.armorName}. Sheet calculations; condition/target adjustments are not inferred."
    private fun saves(c: CharacterState) = "Saves: Fortitude=${c.fortitudeSave()}; Reflex=${c.reflexSave()}; Will=${c.willSave()}."
    private fun weapons(c: CharacterState) = listOf(c.meleeWeapon, c.rangedWeapon).distinct().joinToString("\n") { name ->
        val w = weaponCatalog.firstOrNull { it.name == name }
        if (w == null) "Weapon $name: mechanics unavailable"
        else "$name: attack=${c.attackBonus(w)}, fullAttack=${c.iterativeAttackBonuses(w)}, damage=${w.damageDice}+(${c.damageBonus(w)}), crit=${w.criticalThreatMin}-20/x${w.criticalMultiplier}, range=${w.rangeIncrementFt ?: "unavailable"} ft; ${w.traits}"
    }
    private fun skillSummary(c: CharacterState, skills: List<SkillDefinition>) = "Skills: " + skills.joinToString("; ") {
        "${it.name}=${c.skillBonus(it)} (ranks=${c.pf1SkillRanks[it.name] ?: 0}, misc=${c.pf1SkillMisc[it.name] ?: 0}" +
            if (it.trainedOnly && (c.pf1SkillRanks[it.name] ?: 0) == 0) ", trained-only: unavailable)" else ")"
    }
    private fun resources(c: CharacterState) = "HP=${c.currentHp}/${c.maxHp}, temp=${c.tempHp}, status=${c.pf1HpState()}, stable=${c.pf1Stable}, dead=${c.pf1Dead}; conditions=${c.conditions.ifBlank { "none recorded" }}; currency cp=${c.currencyCp},sp=${c.currencySp},gp=${c.currencyGp},pp=${c.currencyPp}; legacy focus=${c.focusCurrent}/${c.focusMax}; PF1 class resource usage unavailable (no persistent ledger).\nInventory: " +
        c.inventory.joinToString("; ") { "${it.name} x${it.quantity} [${it.category}; weight=${it.weight}; ${it.description}; ${it.mechanics}]" }

    /** Complete stored mechanics without truncating spells, feats or inventory. */
    fun context(c: CharacterState): String = listOf(
        "AUTHORITATIVE CHARACTER MECHANICS (Android values; explanation cannot override these):",
        "ruleset=${c.ruleset}; classes=${c.effectivePf1ClassLevels().toSortedMap()}; abilities=${c.abilities}; BAB=${c.baseAttackBonus()}; initiative=${c.initiative()}; speed=${c.speed}; size=${c.size}",
        defense(c), saves(c), "CMB=${c.cmb()}; CMD=${c.cmd()}", weapons(c),
        "Feats/features=${feats(c)}; actions=${c.actionsAndActivities}; free/reactions=${c.freeActionsAndReactions}",
        skillSummary(c, skillDefinitions), spellSummary(c),
        "castingAbility=${c.spellcastingAbility}; casterLevel=${c.casterLevel()}; spellAttack=${c.spellAttack()}; spellDCs=${c.spells.keys.filter { it >= 0 }.sorted().associateWith { c.spellDc(it) }}",
        resources(c), "senses=${c.senses}; resistances=${c.resistances}; movement=${c.movementNotes}",
        "References=$model; $rules; $engine. $missing"
    ).joinToString("\n")

    fun previewSpell(c: CharacterState, spell: String): MechanicsPreview {
        val levels = c.spells.filterValues { names -> names.any { it.equals(spell.trim(), true) } }.keys
        val level = levels.singleOrNull()
        val problems = mutableListOf<String>()
        if (!c.isPf1()) problems += "PF1 ruleset required."
        if (level == null) problems += "Spell missing or ambiguous in stored spell list."
        else {
            if (level < 0) problems += "Legacy special casting is unsupported by PF1 preview."
            if (level > 0 && c.spellSlotsRemaining(level) <= 0) problems += "No level $level spell slot remains."
            if ((c.abilities[c.spellcastingAbility] ?: 10) < 10 + level) problems += "Casting ability too low."
            val profile = pf1ClassProfile(c.className)
            val classLevel = c.effectivePf1ClassLevels().entries.firstOrNull { it.key.equals(c.className, true) }?.value ?: 0
            if (profile?.castingAbility == null || classLevel < (profile.spellStartLevel ?: Int.MAX_VALUE)) problems += "Class spellcasting unavailable."
            if (level > 0 && level !in pf1SpellSlotsForClass(c.className, classLevel, c.abilities[c.spellcastingAbility] ?: 10)) problems += "Spell level unavailable to class progression."
        }
        return MechanicsPreview(if (problems.isEmpty()) null else false,
            resourceCosts = if (problems.isEmpty() && level != null && level > 0) mapOf("Spell slot L$level" to 1) else emptyMap(),
            notes = problems + "Preview only; spell-specific preparation, targeting and casting restrictions unavailable.", reference = rules)
    }

    fun previewAttack(c: CharacterState, weaponName: String, fullAttack: Boolean = false,
                      moved: Boolean? = null, powerAttack: Boolean = false): MechanicsPreview {
        val w = weaponCatalog.firstOrNull { it.name == weaponName }
            ?: return MechanicsPreview(false, notes = listOf("Weapon mechanics unavailable."), reference = model)
        if (!c.isPf1()) return MechanicsPreview(false, notes = listOf("PF1 ruleset required."), reference = rules)
        val state = Pf1CombatState(c.baseAttackBonus(), c.fortitudeSave(), c.reflexSave(), c.willSave(),
            c.ac(), c.touchAc(), c.flatFootedAc(), c.cmb(), c.cmd(), feats(c).toSet(),
            c.effectivePf1ClassLevels().map { Pf1ClassLevel(it.key, it.value) })
        val validation = if (powerAttack) Pf1ActionEngine.validatePowerAttack(state) else Pf1ActionValidation(true)
        val problems = validation.problems.toMutableList()
        if (powerAttack && w.ability == Ability.DEX) problems += "Power Attack preview supports melee only."
        if (fullAttack && moved == true) problems += "Full attack unavailable after a move action (a 5-foot step is distinct)."
        return MechanicsPreview(if (problems.isNotEmpty()) false else null,
            (if (fullAttack) c.iterativeAttackBonuses(w) else listOf(c.attackBonus(w))).map { it + validation.modifiers.attackBonus },
            notes = problems + validation.modifiers.notes + "Target, turn, movement and condition legality require encounter context; feat effects beyond requested Power Attack are not inferred.", reference = "$model; $engine")
    }

    fun previewManeuver(c: CharacterState): MechanicsPreview = MechanicsPreview(
        if (c.isPf1()) null else false, listOf(c.cmb()),
        notes = listOf("Compare CMB against validated target CMD; maneuver type, reach, target and attack-of-opportunity eligibility require context."), reference = rules)
}
