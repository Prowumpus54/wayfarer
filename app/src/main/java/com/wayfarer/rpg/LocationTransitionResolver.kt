package com.wayfarer.rpg

data class LocationTransition(val destination: ModuleDestination? = null, val message: String = "")

/** Only explicit single-destination travel is interpreted; narration never calls this API. */
object LocationTransitionResolver {
    private val movement = Regex(
        """^(?:i(?:['’]ll| will)?|we(?:['’]ll| will)?|the party)\s+(?:go|walk|travel|move|head|return|leave|enter|visit|proceed|journey|climb|descend|ascend|cross|follow|advance|step)\b\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )
    private val leadingTravelWords = Regex(
        """^(?:(?:back|on|to|toward|towards|into|for|down|up|through|across)\s+|the\s+)+""",
        RegexOption.IGNORE_CASE
    )

    fun propose(action: String, scene: ModuleSceneContext?): LocationTransition {
        val match = movement.matchEntire(action.trim().trimEnd('.', '!'))
            ?: return LocationTransition()
        val normalized = match.groupValues[1]
            .replace(leadingTravelWords, "")
            .trim()
        if (normalized.isBlank()) {
            return LocationTransition(
                message = "Movement unresolved: use explicit travel to one known adjacent destination. Authoritative location unchanged."
            )
        }
        val matches = scene?.destinations.orEmpty().filter {
            it.name.equals(normalized, true) || it.id.equals(normalized, true)
        }
        return if (matches.size == 1) LocationTransition(matches.single())
        else LocationTransition(message =
            "Movement unresolved: no unique unlocked connection to '$normalized'. Authoritative location unchanged.")
    }
    /** Revalidate against the live origin before invoking persistence. */
    fun commit(proposal: ModuleDestination, scene: ModuleSceneContext?, blocked: Boolean = false,
        persist: (String) -> Unit): LocationTransition {
        val destination = scene?.destinations?.singleOrNull { it.id == proposal.id }
        if (blocked || destination == null || destination.id == scene.location.id) {
            return LocationTransition(message = "Movement blocked or unresolved. Authoritative location unchanged.")
        }
        persist(destination.id)
        return LocationTransition(destination, "Validated travel to ${destination.name}.")
    }
}
