package com.wayfarer.rpg

import org.junit.Assert.assertEquals
import org.junit.Test

class JevCombatPolicyTest {
    private fun decision(
        suggestedRoute: JevRoute? = JevRoute.ANDROID_RULES,
        routeConfidence: Double = 0.95,
        actionType: JevActionType = JevActionType.STRIKE,
        actionConfidence: Double = 0.95,
        needsGmProbability: Double = 0.10
    ) = JevCombatDecision(
        suggestedRoute = suggestedRoute,
        routeConfidence = routeConfidence,
        actionType = actionType,
        targetType = JevTargetType.ENEMY,
        actionConfidence = actionConfidence,
        targetConfidence = 0.95,
        movementProbability = 0.0,
        needsGmProbability = needsGmProbability,
        latencyMs = 250
    )

    @Test
    fun confidentBoundedActionRoutesToAndroid() {
        assertEquals(
            JevRoute.ANDROID_RULES,
            JevCombatPolicy.route(decision()).route
        )
    }

    @Test
    fun confidentGmRouteEscalates() {
        assertEquals(
            JevRoute.GEMINI_ADJUDICATION,
            JevCombatPolicy.route(
                decision(
                    suggestedRoute = JevRoute.GEMINI_ADJUDICATION,
                    routeConfidence = 0.92
                )
            ).route
        )
    }

    @Test
    fun lowRouteConfidenceEscalates() {
        assertEquals(
            JevRoute.GEMINI_ADJUDICATION,
            JevCombatPolicy.route(
                decision(routeConfidence = 0.42)
            ).route
        )
    }

    @Test
    fun creativeProbabilityEscalates() {
        assertEquals(
            JevRoute.GEMINI_ADJUDICATION,
            JevCombatPolicy.route(
                decision(needsGmProbability = 0.81)
            ).route
        )
    }
}
