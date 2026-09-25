package com.wayfarer.rpg

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class JevCombatInput(
    val action: String,
    val actorName: String,
    val weapon: String = "",
    val targetName: String? = null,
    val targetRelation: String = "",
    val targetDistanceFt: Int? = null,
    val scene: String = ""
)

class JevCombatClient(
    private val endpoint: String = DEFAULT_ENDPOINT
) {
    companion object {
        const val DEFAULT_ENDPOINT =
            "https://jev-proxy-three.vercel.app/api/classify-combat"
    }

    val configured: Boolean
        get() = endpoint.startsWith("https://")

    suspend fun classify(input: JevCombatInput): JevCombatDecision {
        require(configured) { "Jev proxy endpoint is not configured." }
        val token = firebaseIdToken()
        val actor = JSONObject()
            .put("name", input.actorName)
            .put("weapon", input.weapon)
            .put("position", input.scene.take(120))
        val target = JSONObject()
            .put("name", input.targetName ?: "")
            .put("relation", input.targetRelation)
        input.targetDistanceFt?.let { target.put("distanceFt", it) }

        val body = JSONObject()
            .put("playerText", input.action)
            .put("actor", actor)
            .put("target", target)
            .put("scene", input.scene.take(400))
            .toString()

        return withContext(Dispatchers.IO) {
            val connection = (
                URL(endpoint).openConnection() as HttpURLConnection
                ).apply {
                requestMethod = "POST"
                connectTimeout = 2_500
                readTimeout = 4_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
            }

            try {
                connection.outputStream.bufferedWriter().use { writer ->
                    writer.write(body)
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val message = runCatching {
                        JSONObject(raw).optString("error")
                    }.getOrNull().orEmpty()
                    throw IllegalStateException(
                        if (message.isBlank()) {
                            "Jev proxy failed with HTTP $status"
                        } else {
                            "Jev: $message"
                        }
                    )
                }
                parseDecision(JSONObject(raw))
            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun firebaseIdToken(): String =
        suspendCancellableCoroutine { continuation ->
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                continuation.resumeWithException(
                    IllegalStateException("Sign in before using Jev.")
                )
                return@suspendCancellableCoroutine
            }

            user.getIdToken(false)
                .addOnSuccessListener { result ->
                    val token = result.token
                    if (token.isNullOrBlank()) {
                        continuation.resumeWithException(
                            IllegalStateException("Firebase token was empty.")
                        )
                    } else {
                        continuation.resume(token)
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }

    private fun parseDecision(json: JSONObject): JevCombatDecision {
        val route = json.optJSONObject("route")
        val action = json.optJSONObject("action")
        val target = json.optJSONObject("target")
        val movement = json.optJSONObject("movement")
        val needsGm = json.optJSONObject("needsGm")

        val suggestedRoute = when (route?.optString("choice")?.lowercase()) {
            "rules" -> JevRoute.ANDROID_RULES
            "gm" -> JevRoute.GEMINI_ADJUDICATION
            else -> null
        }

        return JevCombatDecision(
            suggestedRoute = suggestedRoute,
            routeConfidence = route?.optDouble("confidence", 0.0)
                ?.coerceIn(0.0, 1.0) ?: 0.0,
            actionType = enumValue(
                action?.optString("choice").orEmpty(),
                JevActionType.OTHER
            ),
            targetType = enumValue(
                target?.optString("choice").orEmpty(),
                JevTargetType.NONE
            ),
            actionConfidence = action?.optDouble("confidence", 0.0)
                ?.coerceIn(0.0, 1.0) ?: 0.0,
            targetConfidence = target?.optDouble("confidence", 0.0)
                ?.coerceIn(0.0, 1.0) ?: 0.0,
            movementProbability = movement?.optDouble("probability", 0.0)
                ?.coerceIn(0.0, 1.0) ?: 0.0,
            needsGmProbability = needsGm?.optDouble("probability", 1.0)
                ?.coerceIn(0.0, 1.0) ?: 1.0,
            latencyMs = json.optLong("latencyMs", 0L)
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(
        raw: String,
        fallback: T
    ): T {
        val normalized = raw.trim().uppercase()
        return enumValues<T>().firstOrNull {
            it.name == normalized
        } ?: fallback
    }
}
