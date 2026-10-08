package com.wayfarer.rpg

/** Uses a fresh campaign location; stale scenes and chat cannot supply location. */
object ContextAssembler {
    fun assemble(campaignTitle: String, locationId: String, scene: ModuleSceneContext?,
        character: CharacterState, party: List<PartyMember>, runtime: CampaignRuntimeState,
        events: List<GameEvent>, action: String, playerRoll: String? = null,
        archivedSummary: String = "", movementResult: String = ""): GmContext {
        val current = scene?.takeIf { it.location.id == locationId }
        val history = HistoryCompactor.select(events, archivedSummary)
        fun bounded(text: String, limit: Int = 1800) = ContextText.clean(text).take(limit)
        return GmContext(
            campaignTitle = bounded(campaignTitle),
            location = current?.location?.name ?: "Unresolved saved location [$locationId]",
            locationDescription = bounded(current?.location?.playerDescription.orEmpty()),
            gmNotes = bounded(current?.location?.gmNotes.orEmpty()),
            destinations = current?.destinations.orEmpty().map { bounded(it.name, 120) },
            npcs = current?.npcs.orEmpty().take(12).map { bounded("${it.name} (${it.role})", 200) },
            encounters = current?.encounters.orEmpty().take(8).map { encounter ->
                val creatures = current?.encounterCreatures.orEmpty()
                    .filter { it.encounterId == encounter.id }.take(8)
                    .joinToString("; ") { "${it.quantity} x ${it.creatureRuleRef}" }
                bounded("${encounter.name} [${encounter.difficulty}]: ${encounter.triggerText}\n${encounter.gmNotes}\nCreatures: $creatures", 700)
            },
            treasure = current?.treasure.orEmpty().take(8).map {
                bounded("${it.name} ×${it.quantity} [${it.ruleRef}]" +
                    (if (it.hidden) " [hidden]" else "") + ": ${it.gmNotes}", 350)
            },
            character = character, party = party, recentHistory = history.recent,
            runtimeState = GameStateEngine.contextLines(runtime) +
                listOf("Authoritative campaign location ID: $locationId") +
                listOfNotNull(movementResult.takeIf { it.isNotBlank() }),
            action = action, playerRoll = playerRoll, historySummary = history.summary
        )
    }
}
