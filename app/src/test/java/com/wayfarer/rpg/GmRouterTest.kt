package com.wayfarer.rpg

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GmRouterTest {
    private val calls = mutableListOf<GmRoute>()
    private val prompts = mutableListOf<String>()
    private fun router(
        failures: Set<GmRoute> = emptySet(),
        desktopConfigured: Boolean = true,
        blank: Set<GmRoute> = emptySet(),
        cancel: GmRoute? = null
    ): GmRouter {
        fun runtime(route: GmRoute) = GmRuntime { prompt ->
            calls += route
            prompts += prompt
            if (route == cancel) throw CancellationException("cancel")
            if (route in failures) error("unavailable")
            if (route in blank) "" else "response"
        }
        return GmRouter(runtime(GmRoute.ON_DEVICE), runtime(GmRoute.DESKTOP),
            runtime(GmRoute.FLASH), runtime(GmRoute.FLASH_LITE)) { desktopConfigured }
    }

    @Test fun autoPrefersOnDevice() = runBlocking {
        val result = router().generate(GmModelChoice.AUTO, "roll=17")
        assertEquals(listOf(GmRoute.ON_DEVICE), calls)
        assertEquals(GmRoute.ON_DEVICE, result.route)
    }

    @Test fun autoUsesDesktopWhenOnDeviceUnavailable() = runBlocking {
        val result = router(setOf(GmRoute.ON_DEVICE)).generate(GmModelChoice.AUTO, "roll=17")
        assertEquals(listOf(GmRoute.ON_DEVICE, GmRoute.DESKTOP), calls)
        assertEquals(GmRoute.DESKTOP, result.route)
    }

    @Test fun autoSkipsUnconfiguredDesktop() = runBlocking {
        val result = router(setOf(GmRoute.ON_DEVICE), false).generate(GmModelChoice.AUTO, "roll=17")
        assertEquals(listOf(GmRoute.ON_DEVICE, GmRoute.FLASH), calls)
        assertEquals(GmRoute.FLASH, result.route)
    }

    @Test fun autoTriesAllRoutesInOrderAndPreservesRoll() = runBlocking {
        val result = router(setOf(GmRoute.ON_DEVICE, GmRoute.DESKTOP, GmRoute.FLASH))
            .generate(GmModelChoice.AUTO, "player already rolled 17")
        assertEquals(GmRoute.entries, calls)
        assertEquals(List(4) { "player already rolled 17" }, prompts)
        assertEquals(GmRoute.FLASH_LITE, result.route)
    }

    @Test fun everyExplicitChoiceStaysOnItsRoute() = runBlocking {
        val selections = mapOf(GmModelChoice.ON_DEVICE to GmRoute.ON_DEVICE,
            GmModelChoice.DESKTOP to GmRoute.DESKTOP, GmModelChoice.FLASH to GmRoute.FLASH,
            GmModelChoice.FLASH_LITE to GmRoute.FLASH_LITE)
        for ((choice, route) in selections) {
            calls.clear()
            try {
                router(GmRoute.entries.toSet(), false).generate(choice, "prompt")
                fail("Expected explicit failure")
            } catch (error: GmRouteException) {
                assertEquals(route, error.route)
                assertTrue(error.message!!.contains(route.label))
            }
            assertEquals(listOf(route), calls)
        }
    }

    @Test fun explicitSuccessRecordsSelectedRoute() = runBlocking {
        for ((choice, route) in listOf(GmModelChoice.ON_DEVICE to GmRoute.ON_DEVICE,
            GmModelChoice.DESKTOP to GmRoute.DESKTOP, GmModelChoice.FLASH to GmRoute.FLASH,
            GmModelChoice.FLASH_LITE to GmRoute.FLASH_LITE)) {
            calls.clear()
            val result = router().generate(choice, "prompt")
            assertEquals(route, result.route)
            assertEquals(route.label, result.modelName)
            assertEquals(listOf(route), calls)
        }
    }

    @Test fun autoFallsBackOnEmptyResponse() = runBlocking {
        assertEquals(GmRoute.DESKTOP, router(blank = setOf(GmRoute.ON_DEVICE))
            .generate(GmModelChoice.AUTO, "prompt").route)
    }

    @Test fun autoExhaustionReturnsClearError() = runBlocking {
        try {
            router(GmRoute.entries.toSet()).generate(GmModelChoice.AUTO, "prompt")
            fail("Expected all routes to fail")
        } catch (error: GmRouteException) {
            assertEquals(GmRoute.FLASH_LITE, error.route)
        }
        assertEquals(GmRoute.entries, calls)
    }

    @Test fun cancellationNeverStartsFallback() = runBlocking {
        try {
            router(cancel = GmRoute.ON_DEVICE).generate(GmModelChoice.AUTO, "prompt")
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals(listOf(GmRoute.ON_DEVICE), calls)
        }
    }

    @Test fun legacyHttpChoicesRemainDesktop() {
        assertEquals(GmModelChoice.DESKTOP, GmModelChoice.fromSaved("LOCAL"))
        assertEquals(GmModelChoice.DESKTOP, GmModelChoice.fromSaved("LOCAL_FAST"))
        assertEquals(GmModelChoice.ON_DEVICE, GmModelChoice.fromSaved("ON_DEVICE"))
        assertEquals(GmModelChoice.AUTO, GmModelChoice.fromSaved(null))
    }
}
