package com.wayfarer.rpg

data class CharacterActionDecision(val action: String? = null, val blocked: Boolean = false, val explanation: String)

object CharacterActionValidator {
    fun validate(request: String, character: CharacterState): CharacterActionDecision {
        val normalized = request.trim().lowercase()
        if (listOf("smite", "power attack", "charge", "pf1", "pathfinder 1").any { it in normalized }) {
            return CharacterActionDecision(blocked = true, explanation =
                "PF1e action not saved: this character uses PF2e-adapted calculations. " +
                "The sheet lacks PF1e base attack bonus, paladin class levels and Smite Evil resources. " +
                "We cannot verify the requested combination against this sheet. " +
                "A PF1e implementation must check feat prerequisites, the type of smite, weapon handling, " +
                "target eligibility, remaining resources and the charge path before calculating any rolls. " +
                "PF1e references: Archives of Nethys, Charge (Rules.aspx?ID=187), " +
                "Paladin (legacy.aonprd.com/coreRuleBook/classes/paladin.html).")
        }
        val match = Regex("(?:create|build|add) (?:a |an )?(.+?)(?: check)?(?: action)?[.!]?").matchEntire(normalized)
        if (match != null) {
            val name = match.groupValues[1]
            val skill = skillDefinitions.firstOrNull { it.name.equals(name, true) }
            val canonical = skill?.name ?: "Perception".takeIf { name == "perception" }
            if (canonical != null) {
                val modifier = skill?.let(character::skillBonus) ?: character.perception()
                return CharacterActionDecision(action = "$canonical check", explanation =
                    "$canonical check: 1d20 ${if (modifier >= 0) "+" else ""}$modifier from the current sheet. " +
                    "The GM must confirm the task, any training requirement, situational modifiers and DC. " +
                    "This saves a check shortcut, not automatic permission to perform every use of the skill. " +
                    "No damage or resource consumption. The modifier is recalculated from the sheet when used.")
            }
            return CharacterActionDecision(blocked = true, explanation =
                "This action is outside the current rules validator and was not saved. " +
                "Supported examples: create a Perception check; create a Stealth check. " +
                "Ask for advice about other actions without the create/build/add prefix.")
        }
        return CharacterActionDecision(explanation = "Advice request")
    }
}

fun appendPlayerRoll(pending: String?, next: String): String =
    listOfNotNull(pending?.takeIf { it.isNotBlank() }, next).joinToString("\n")

fun retainCheckOnRetry(recorded: CheckResult?, roll: () -> CheckResult): CheckResult = recorded ?: roll()
