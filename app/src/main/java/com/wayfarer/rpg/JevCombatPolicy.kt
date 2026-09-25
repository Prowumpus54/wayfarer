package com.wayfarer.rpg

enum class JevActionType {
    STRIKE,
    MOVE,
    CAST_SPELL,
    INTERACT,
    SKILL_ACTION,
    DEFEND,
    AID,
    READY,
    OTHER
}

enum class JevTargetType {
    ENEMY,
    ALLY,
    SELF,
    ENVIRONMENT,
    NONE
}

enum class JevRoute {
    ANDROID_RULES,
    GEMINI_ADJUDICATION
}

data class JevCombatDecision(
    val suggestedRoute: JevRoute? = null,
    val routeConfidence: Double = 0.0,
    val actionType: JevActionType,
    val targetType: JevTargetType,
    val actionConfidence: Double,
    val targetConfidence: Double,
    val movementProbability: Double,
    val needsGmProbability: Double,
    val latencyMs: Long = 0L
) {
    val includesMovement: Boolean
        get() = movementProbability >= 0.5

    val needsGm: Boolean
        get() = needsGmProbability >= 0.5
}

data class JevRoutingResult(
    val route: JevRoute,
    val reason: String
)

object JevCombatPolicy {
    const val ROUTE_CONFIDENCE_MIN = 0.70
    const val ACTION_CONFIDENCE_MIN = 0.70
    const val GM_ESCALATION_MAX = 0.50

    fun route(decision: JevCombatDecision): JevRoutingResult {
        if (decision.suggestedRoute == JevRoute.GEMINI_ADJUDICATION &&
            decision.routeConfidence >= ROUTE_CONFIDENCE_MIN
        ) {
            return JevRoutingResult(
                JevRoute.GEMINI_ADJUDICATION,
                "Jev identified an improvised or ambiguous action"
            )
        }

        if (decision.routeConfidence < ROUTE_CONFIDENCE_MIN) {
            return JevRoutingResult(
                JevRoute.GEMINI_ADJUDICATION,
                "Low routing confidence"
            )
        }

        if (decision.actionConfidence < ACTION_CONFIDENCE_MIN) {
            return JevRoutingResult(
                JevRoute.GEMINI_ADJUDICATION,
                "Low action confidence"
            )
        }

        if (decision.needsGmProbability >= GM_ESCALATION_MAX) {
            return JevRoutingResult(
                JevRoute.GEMINI_ADJUDICATION,
                "Creative GM adjudication requested"
            )
        }

        if (decision.actionType == JevActionType.OTHER) {
            return JevRoutingResult(
                JevRoute.GEMINI_ADJUDICATION,
                "Unbounded action"
            )
        }

        return JevRoutingResult(
            JevRoute.ANDROID_RULES,
            "Confident bounded combat intent"
        )
    }
}
