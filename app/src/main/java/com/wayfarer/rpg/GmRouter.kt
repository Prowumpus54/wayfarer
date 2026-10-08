package com.wayfarer.rpg

import kotlinx.coroutines.CancellationException

enum class GmModelChoice(val displayName: String, val shortName: String) {
    AUTO("Auto (recommended)", "Auto"),
    ON_DEVICE("On-device Gemma", "On-device"),
    DESKTOP("Desktop Gemma", "Desktop"),
    FLASH("Gemini 3.8 Flash", "3.8 Flash"),
    FLASH_LITE("Gemini 3.5 Flash Lite", "3.5 Flash Lite");

    companion object {
        fun fromSaved(value: String?): GmModelChoice = when (value) {
            // Legacy LOCAL was HTTP, never Android inference.
            "LOCAL", "LOCAL_FAST" -> DESKTOP
            else -> entries.firstOrNull { it.name == value } ?: AUTO
        }
    }
}

enum class GmRoute(val label: String) {
    ON_DEVICE("On-device Gemma"), DESKTOP("Desktop Gemma"),
    FLASH("Gemini 3.8 Flash"), FLASH_LITE("Gemini 3.5 Flash Lite")
}

data class GmAnswer(val text: String, val modelName: String, val route: GmRoute)
fun interface GmRuntime { suspend fun generate(prompt: String): String }

class GmRouteException(val route: GmRoute, cause: Exception) :
    IllegalStateException("${route.label} unavailable or failed: ${cause.message ?: cause.javaClass.simpleName}", cause)

/** Only Auto crosses provider boundaries. Every attempt receives the identical prompt/roll. */
class GmRouter(
    private val onDevice: GmRuntime,
    private val desktop: GmRuntime,
    private val flash: GmRuntime,
    private val lite: GmRuntime,
    private val desktopConfigured: () -> Boolean
) {
    suspend fun generate(choice: GmModelChoice, prompt: String): GmAnswer {
        val routes = when (choice) {
            GmModelChoice.AUTO -> buildList {
                add(GmRoute.ON_DEVICE)
                if (desktopConfigured()) add(GmRoute.DESKTOP)
                add(GmRoute.FLASH)
                add(GmRoute.FLASH_LITE)
            }
            GmModelChoice.ON_DEVICE -> listOf(GmRoute.ON_DEVICE)
            GmModelChoice.DESKTOP -> listOf(GmRoute.DESKTOP)
            GmModelChoice.FLASH -> listOf(GmRoute.FLASH)
            GmModelChoice.FLASH_LITE -> listOf(GmRoute.FLASH_LITE)
        }
        var lastError: GmRouteException? = null
        for (route in routes) {
            try {
                val runtime = when (route) {
                    GmRoute.ON_DEVICE -> onDevice
                    GmRoute.DESKTOP -> desktop
                    GmRoute.FLASH -> flash
                    GmRoute.FLASH_LITE -> lite
                }
                val text = runtime.generate(prompt)
                check(text.isNotBlank()) { "Empty response" }
                return GmAnswer(text, route.label, route)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastError = GmRouteException(route, error)
                if (choice != GmModelChoice.AUTO) throw lastError
            }
        }
        throw lastError ?: IllegalStateException("No GM route available")
    }
}
