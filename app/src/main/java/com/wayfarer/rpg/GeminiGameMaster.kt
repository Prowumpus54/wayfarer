package com.wayfarer.rpg

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GmCheckRequest(
    val name: String,
    val dc: Int,
    val reason: String,
    val modifierAdjustment: Int = 0,
    val modifierLabel: String = ""
)

data class GmDiceModifier(
    val label: String,
    val value: Int,
    val appliesTo: String = "next roll",
    val consumeOnRoll: Boolean = true
)

data class GmTurn(
    val narration: String,
    val check: GmCheckRequest? = null,
    val modifiers: List<GmDiceModifier> = emptyList(),
    val xpAward: Int = 0,
    val raw: String = "",
    val modelName: String = ""
)

data class GmContext(
    val campaignTitle: String,
    val location: String,
    val locationDescription: String,
    val gmNotes: String,
    val destinations: List<String>,
    val npcs: List<String>,
    val encounters: List<String>,
    val character: CharacterState,
    val party: List<PartyMember>,
    val action: String,
    val playerRoll: String? = null
)

enum class GmModelChoice(
    val displayName: String,
    val shortName: String
) {
    AUTO("Auto (recommended)", "Auto"),
    LOCAL("Local Qwen 3.5 9B", "Local"),
    LOCAL_FAST("Local Qwen 3.5 4B", "Local Fast"),
    FLASH("Gemini 3.8 Flash", "3.8 Flash"),
    FLASH_LITE("Gemini 3.5 Flash Lite", "3.5 Flash Lite")
}

class GeminiGameMaster(
    private val modelChoice: GmModelChoice = GmModelChoice.AUTO
) {
    private val modelNames: List<String>
        get() = when (modelChoice) {
            GmModelChoice.AUTO -> listOf(
                "gemini-3.8-flash",
                "gemini-3.5-flash-lite"
            )
            GmModelChoice.LOCAL, GmModelChoice.LOCAL_FAST -> emptyList()
            GmModelChoice.FLASH -> listOf("gemini-3.8-flash")
            GmModelChoice.FLASH_LITE -> listOf("gemini-3.5-flash-lite")
        }

    suspend fun adjudicate(
        context: GmContext,
        jevIntent: JevCombatDecision? = null
    ): GmTurn {
        val promptBase = if (
            jevIntent != null &&
            JevCombatPolicy.route(jevIntent).route == JevRoute.ANDROID_RULES
        ) {
            fastCombatPrompt(context, jevIntent)
        } else {
            basePrompt(context)
        }

        val prompt = promptBase + """

PLAYER ACTION:
${context.action}

${context.playerRoll?.let { "PLAYER'S ALREADY ROLLED DICE: $it. Use this result for the declared action if relevant. Never roll again or request the same check. The listed total already includes its modifier." } ?: "No player dice roll is pending."}

Return one JSON object only:
{
  "narration": "What happens immediately before any required roll.",
  "check": null,
  "modifiers": [],
  "xpAward": 0
}

If uncertainty requires a roll, replace check with:
{
  "name": "one allowed check name",
  "dc": 15,
  "reason": "brief reason",
  "modifierAdjustment": 0,
  "modifierLabel": ""
}

You may also push temporary situational modifiers to the player's manual dice
roller using "modifiers":
[
  {
    "label": "Cover",
    "value": -2,
    "appliesTo": "next roll",
    "consumeOnRoll": true
  }
]

IMPORTANT: modifierAdjustment and pushed modifiers are situational modifiers only.
Never include the character's ability modifier, proficiency bonus, item bonus,
or any bonus already represented by the character sheet; Android adds those.

XP: set xpAward to 0 unless this turn completes a meaningful challenge,
encounter, discovery, objective, or important social obstacle. Typical awards
are 10-120 XP. Never award XP merely for making a roll or repeating an action.
""".trimIndent()

        val generated = generateWithFallback(prompt)
        return parseTurn(generated.first, generated.second)
    }

    suspend fun resolve(
        context: GmContext,
        request: GmCheckRequest,
        result: CheckResult
    ): GmTurn {
        val prompt = basePrompt(context) + """

The app resolved the requested ${request.name} check locally.
Die: ${result.die}
Modifier: ${result.modifier}
Total: ${result.total}
DC: ${result.dc}
Degree: ${degreeLabel(result.degree)}

Narrate the consequences. Do not reroll, change the DC,
or contradict the degree of success.

If this resolved outcome completes a meaningful challenge, encounter,
discovery, objective, or important social obstacle, you may award 10-120 XP.
Otherwise xpAward must be 0. Never award XP merely for making the roll.

Return JSON only:
{"narration":"outcome narration","check":null,"modifiers":[],"xpAward":0}
""".trimIndent()

        val generated = generateWithFallback(prompt)
        return parseTurn(generated.first, generated.second)
    }

    private fun fastCombatPrompt(
        context: GmContext,
        intent: JevCombatDecision
    ): String {
        val character = context.character
        val route = JevCombatPolicy.route(intent)
        return """
You are Wayfarer's immediate combat game master.
Be concise and resolve only the declared action.

Android is authoritative for all dice and rules calculations.
Jev already classified the intent:
ACTION: ${intent.actionType.name}
TARGET: ${intent.targetType.name}
MOVEMENT INCLUDED: ${intent.includesMovement}
ROUTE: ${route.route.name}
ACTION CONFIDENCE: ${"%.2f".format(intent.actionConfidence)}

CAMPAIGN: ${context.campaignTitle}
LOCATION: ${context.location}
ACTIVE CHARACTER: ${character.characterName}, ${character.className} ${character.level}
HP: ${character.currentHp}/${character.maxHp}
WEAPONS: ${character.meleeWeapon}; ${character.rangedWeapon}

RELEVANT ENCOUNTERS / HAZARDS:
${context.encounters.joinToString("\n")}

GM NOTES:
${context.gmNotes}

Do not reveal hidden information unless the action and rules justify it.
Do not reinterpret a bounded action into a different action.
${context.playerRoll?.let { "PLAYER'S ALREADY ROLLED DICE: $it. Use this result when relevant; do not roll again or request the same check. Its total already includes the modifier." } ?: ""}
Request only one check when one is actually needed.
""".trimIndent()
    }

    private fun basePrompt(context: GmContext): String {
        val partyText = context.party.joinToString("; ") {
            "${it.name}, ${it.className} ${it.level}, HP ${it.hp}/${it.maxHp}"
        }
        val character = context.character

        return """
You are Wayfarer's immediate tabletop game master.
Be concise, atmospheric, fair, and responsive to player intent.

CRITICAL RULE:
You never roll dice. You may REQUEST a check. Android is the
authority for all dice and will send the result back to you.

Allowed checks:
Perception, Acrobatics, Arcana, Athletics, Crafting, Deception,
Diplomacy, Intimidation, Medicine, Nature, Occultism, Performance,
Religion, Society, Stealth, Survival, Thievery, Fortitude, Reflex, Will.

Only request a check when failure is meaningful. Choose a reasonable
DC for the actual situation. Never invent a second roll after Android
has resolved one.

CAMPAIGN: ${context.campaignTitle}
CURRENT LOCATION: ${context.location}
PLAYER-VISIBLE LOCATION DESCRIPTION:
${context.locationDescription}

GM-ONLY MODULE NOTES:
${context.gmNotes}

AVAILABLE DESTINATIONS:
${context.destinations.joinToString("; ")}

NPCS PRESENT:
${context.npcs.joinToString("; ")}

ENCOUNTERS / HAZARDS IN THIS LOCATION:
${context.encounters.joinToString("\n")}

Never reveal GM-only notes, hidden traps, undiscovered treasure, secret doors,
or encounter information until player actions or rules justify discovery.

PARTY: $partyText
ACTIVE CHARACTER: ${character.characterName}
CLASS/LEVEL: ${character.className} ${character.level}
HP: ${character.currentHp}/${character.maxHp}
WEAPONS: ${character.meleeWeapon}; ${character.rangedWeapon}
""".trimIndent()
    }

    private suspend fun generateWithFallback(prompt: String): Pair<String, String> {
        if (modelChoice == GmModelChoice.LOCAL) return generateLocal(prompt, "gm")
        if (modelChoice == GmModelChoice.LOCAL_FAST) return generateLocal(prompt, "fast")
        if (modelChoice == GmModelChoice.AUTO && BuildConfig.LOCAL_LLM_URL.isNotBlank()) {
            try {
                return generateLocal(prompt, "gm")
            } catch (error: Exception) {
                Log.e("WayfarerGM", "local GM failed; falling back to Gemini: ${error.message}", error)
            }
        }
        var lastError: Exception? = null

        for (modelName in modelNames) {
            repeat(2) { attempt ->
                try {
                    val model = Firebase
                        .ai(backend = GenerativeBackend.googleAI())
                        .generativeModel(modelName)
                    val text = model.generateContent(prompt).text.orEmpty()
                    return text to modelName
                } catch (error: Exception) {
                    lastError = error
                    Log.e(
                        "WayfarerGM",
                        "generate failed model=$modelName attempt=${attempt + 1} type=${error::class.java.name} message=${error.message}",
                        error
                    )
                    if (!isTransient(error)) throw error
                    if (attempt == 0) delay(900)
                }
            }
        }

        throw lastError ?: IllegalStateException("No Gemini model responded.")
    }

    private suspend fun generateLocal(prompt: String, profile: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val base = BuildConfig.LOCAL_LLM_URL.trimEnd('/')
        if (base.isBlank()) throw IOException("Local LLM URL is not configured")
        val connection = (URL("$base/v1/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 180000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${BuildConfig.LOCAL_LLM_TOKEN}")
        }
        val body = JSONObject().put("profile", profile).put("prompt", prompt).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IOException("Local LLM HTTP $code: ${raw.take(240)}")
        val json = JSONObject(raw)
        val text = json.optString("text")
        if (text.isBlank()) throw IOException("Local LLM returned an empty response")
        text to ("local:" + json.optString("model", profile))
    }

    private fun isTransient(error: Exception): Boolean {
        val message = (error.message ?: error.toString()).lowercase()
        return listOf(
            "high demand",
            "temporar",
            "unavailable",
            "resource exhausted",
            "resource_exhausted",
            "429",
            "503",
            "overloaded",
            "try again"
        ).any { message.contains(it) }
    }

    private fun parseTurn(raw: String, modelName: String): GmTurn {
        val clean = raw.trim()
            .removePrefix("json:")
            .trim()

        return runCatching {
            val start = clean.indexOf('{')
            val end = clean.lastIndexOf('}')
            val jsonText = if (start >= 0 && end > start) {
                clean.substring(start, end + 1)
            } else clean
            val json = JSONObject(jsonText)
            val narration = json.optString("narration").ifBlank { clean }
            val checkObject = json.optJSONObject("check")
            val check = if (checkObject == null) {
                null
            } else {
                GmCheckRequest(
                    name = checkObject.optString("name"),
                    dc = checkObject.optInt("dc", 15).coerceIn(5, 50),
                    reason = checkObject.optString("reason"),
                    modifierAdjustment = checkObject
                        .optInt("modifierAdjustment", 0)
                        .coerceIn(-20, 20),
                    modifierLabel = checkObject.optString("modifierLabel")
                ).takeIf { it.name.isNotBlank() }
            }

            val modifiersArray = json.optJSONArray("modifiers")
            val modifiers = buildList {
                if (modifiersArray != null) {
                    for (index in 0 until modifiersArray.length()) {
                        val item = modifiersArray.optJSONObject(index) ?: continue
                        val label = item.optString("label").trim()
                        if (label.isBlank()) continue
                        add(
                            GmDiceModifier(
                                label = label,
                                value = item.optInt("value", 0).coerceIn(-20, 20),
                                appliesTo = item.optString("appliesTo", "next roll"),
                                consumeOnRoll = item.optBoolean("consumeOnRoll", true)
                            )
                        )
                    }
                }
            }

            GmTurn(
                narration = narration,
                check = check,
                modifiers = modifiers,
                xpAward = json.optInt("xpAward", 0).coerceIn(0, 250),
                raw = raw,
                modelName = modelName
            )
        }.getOrElse {
            GmTurn(
                narration = clean.ifBlank {
                    "The Game Master did not return a response."
                },
                raw = raw,
                modelName = modelName
            )
        }
    }

    private fun degreeLabel(degree: Degree): String = when (degree) {
        Degree.CRITICAL_SUCCESS -> "Critical Success"
        Degree.SUCCESS -> "Success"
        Degree.FAILURE -> "Failure"
        Degree.CRITICAL_FAILURE -> "Critical Failure"
    }
}

fun CharacterState.modifierForCheck(name: String): Int? {
    val normalized = name.trim()

    skillDefinitions.firstOrNull {
        it.name.equals(normalized, ignoreCase = true)
    }?.let {
        return skillBonus(it)
    }

    return when {
        normalized.equals("Perception", true) -> perception()
        normalized.equals("Fortitude", true) ->
            saveBonus(Ability.CON, fortitudeProf)
        normalized.equals("Reflex", true) ->
            saveBonus(Ability.DEX, reflexProf)
        normalized.equals("Will", true) ->
            saveBonus(Ability.WIS, willProf)
        else -> null
    }
}
