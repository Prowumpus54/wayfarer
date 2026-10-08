package com.wayfarer.rpg

/** Shared read-only context capture for play and Fourth Wall.
 * Delegates to the same authoritative assembler used by normal GM turns so
 * diagnostics/meta chat cannot resurrect stale scene or 24-event context logic.
 */
fun captureGmContext(
    campaignTitle: String,
    locationId: String,
    moduleScene: ModuleSceneContext?,
    character: CharacterState,
    party: List<PartyMember>,
    recentEvents: List<GameEvent>,
    runtimeState: CampaignRuntimeState,
    archivedSummary: String = "",
    action: String = "",
    rollForAction: String? = null
): GmContext = ContextAssembler.assemble(
    campaignTitle = campaignTitle,
    locationId = locationId,
    scene = moduleScene,
    character = character,
    party = party,
    runtime = runtimeState,
    events = recentEvents,
    action = action,
    playerRoll = rollForAction,
    archivedSummary = archivedSummary
)

fun GmContext.snapshot(): ModelContextSnapshot = ModelContextSnapshot(listOf(
    ContextCategory(
        "authoritative_location",
        "Authoritative location",
        location + "\n" + locationDescription
    ),
    ContextCategory(
        "recent_history",
        "Recent in-world history",
        recentHistory.joinToString("\n")
    ),
    ContextCategory(
        "older_history",
        "Older recorded history",
        historySummary
    ),
    ContextCategory(
        "character_party",
        "Character and party",
        CharacterMechanicsContext.project(character) + "\nParty: " +
            party.joinToString("; ") {
                "${it.name}, ${it.className} ${it.level}, HP ${it.hp}/${it.maxHp}"
            }
    ),
    ContextCategory(
        "module_scene",
        "Module and scene notes",
        "Campaign: $campaignTitle\nDestinations: ${destinations.joinToString("; ")}\nNPCs: ${npcs.joinToString("; ")}"
    ),
    ContextCategory(
        "runtime_state",
        "Authoritative runtime state",
        runtimeState.joinToString("\n")
    ),
    ContextCategory(
        "encounters_challenges",
        "Encounters and challenges",
        encounters.joinToString("\n"),
        true
    ),
    ContextCategory(
        "hidden_gm_context",
        "Hidden GM context",
        gmNotes + "\nTreasure: " + treasure.joinToString("\n"),
        true
    )
))
