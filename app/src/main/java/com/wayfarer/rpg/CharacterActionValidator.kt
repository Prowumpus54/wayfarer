package com.wayfarer.rpg

data class CharacterActionDecision(val action: String? = null, val blocked: Boolean = false, val explanation: String)

object CharacterActionValidator {
    fun validate(request: String, character: CharacterState): CharacterActionDecision {
        val normalized = request.trim().lowercase()
        if (
            character.isPf1() &&
            listOf("smite", "power attack", "charge").any { it in normalized }
        ) {
            return CharacterActionDecision(
                blocked = true,
                explanation =
                    "PF1 action recognized, but this compound action is not saved as an " +
                    "automatic shortcut yet. BAB/CMB/CMD and PF1 attack math are now " +
                    "available, but Smite uses, feat prerequisites, charge movement, and " +
                    "Power Attack choices must be represented explicitly before the app " +
                    "can execute this combination without GM adjudication."
            )
        }
        val match = Regex("(?:create|build|add) (?:a |an )?(.+?)(?: check)?(?: action)?[.!]?").matchEntire(normalized)
        if (match != null) {
            val name = match.groupValues[1]
            val skill = skillDefinitions.firstOrNull { it.name.equals(name, true) }
            val canonical = skill?.name ?: "Perception".takeIf {
                name == "perception"
            }
            if (canonical != null) {
                if (
                    character.isPf1() &&
                    skill?.trainedOnly == true &&
                    (character.pf1SkillRanks[skill.name] ?: 0) <= 0
                ) {
                    return CharacterActionDecision(
                        blocked = true,
                        explanation = canonical +
                            " is trained-only in PF1 and this character has no ranks in it."
                    )
                }
                val modifier = skill?.let(character::skillBonus)
                    ?: character.perception()
                return CharacterActionDecision(
                    action = "$canonical check",
                    explanation =
                        "$canonical check: 1d20 " +
                            (if (modifier >= 0) "+" else "") +
                            "$modifier from the current PF1 sheet. " +
                            "The GM supplies the situation and DC. The modifier is " +
                            "recalculated from ability, ranks, class-skill bonus, armor " +
                            "check penalty, and stored miscellaneous modifiers when used."
                )
            }
            return CharacterActionDecision(blocked = true, explanation =
                "This action is outside the current rules validator and was not saved. " +
                "Supported examples: create a Perception check; create a Stealth check; " +
                "create a Knowledge (nature) check. " +
                "Ask for advice about other actions without the create/build/add prefix.")
        }
        return CharacterActionDecision(explanation = "Advice request")
    }
}

fun appendPlayerRoll(pending: String?, next: String): String =
    listOfNotNull(pending?.takeIf { it.isNotBlank() }, next).joinToString("\n")

fun retainCheckOnRetry(recorded: CheckResult?, roll: () -> CheckResult): CheckResult = recorded ?: roll()
