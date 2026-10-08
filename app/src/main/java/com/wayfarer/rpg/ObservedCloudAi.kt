package com.wayfarer.rpg

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

/** Helpers use the same persistence boundary as the GM, regardless of transport. */
suspend fun observedCloudText(trace: AiTranscriptTrace, prompt: String): String {
    try {
        var last: Exception? = null
        for (modelName in listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")) {
            repeat(2) { attempt ->
                try {
                    val answer = trace.attempt("firebase", modelName, trace.record.operation) {
                        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(modelName)
                            .generateContent(prompt).text.orEmpty().also {
                                if (it.isBlank()) throw IOException("Empty assistant response")
                            }
                    }
                    trace.finish(answer)
                    return answer
                } catch (cancel: CancellationException) { throw cancel
                } catch (error: Exception) {
                    last = error
                    trace.decision(if (attempt == 0) "retry" else "fallback",
                        "$modelName ${error::class.java.simpleName}")
                    if (attempt == 0) delay(700)
                }
            }
        }
        throw last ?: IllegalStateException("Assistant unavailable")
    } catch (cancel: CancellationException) {
        trace.finish("", "cancelled"); throw cancel
    } catch (error: Exception) {
        trace.decision("error", error::class.java.simpleName)
        trace.finish("", "error"); throw error
    }
}
