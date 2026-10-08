package com.wayfarer.rpg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.appcheck.FirebaseAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LoreWiseDiagnostics.initialize(applicationContext)
        LoreWiseTranscripts.initialize(applicationContext)
        LoreWiseDiagnostics.record(
            "app",
            "startup",
            DiagnosticStatus.INFO,
            detail = "MainActivity created"
        )
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                loreWiseAppCheckProviderFactory()
            )
        LoreWiseDiagnostics.record(
            "security",
            "app_check_provider",
            DiagnosticStatus.OK,
            detail = if (BuildConfig.DEBUG) "debug" else "play_integrity"
        )

        setContent {
            LoreWiseTheme {
                LoreWiseApp()
            }
        }
    }
}

@Composable
fun LoreWiseApp() {
    val appContext = LocalContext.current.applicationContext
    val moduleId = "sunless_citadel"
    val moduleRepo = remember { AdventureModuleRepository(appContext) }
    val rulesRepo = remember { RulesRepository(appContext) }
    DisposableEffect(rulesRepo) {
        onDispose { rulesRepo.close() }
    }

    val manifest = remember {
        moduleRepo.ensureBundledModuleInstalled(moduleId)
            ?: AdventureModuleManifest(
                id = moduleId,
                title = "The Sunless Citadel",
                version = "0",
                ruleset = "pf1e",
                description = "A buried fortress and the mystery beneath Oakhurst.",
                startingLocation = "oakhurst",
                minLevel = 1,
                maxLevel = 3
            )
    }

    val campaignRegistry = remember { CampaignRegistry(appContext) }
    var campaign by remember {
        mutableStateOf(campaignRegistry.activeCampaign(moduleId, manifest.title))
    }
    LoreWiseTranscripts.activeScope = campaign.id
    var showCampaignHub by remember { mutableStateOf(false) }
    var current by remember(campaign.id) { mutableStateOf(AppScreen.Home) }

    BackHandler(enabled = true) {
        when {
            showCampaignHub -> showCampaignHub = false
            current != AppScreen.Home -> current = AppScreen.Home
            else -> Unit
        }
    }

    val playSession = remember(campaign.id) { PlaySession() }
    val playScope = key(campaign.id) { rememberCoroutineScope() }
    val screenState = key(campaign.id) { rememberSaveableStateHolder() }

    val stateStore = remember(campaign.id) {
        CampaignStateStore(appContext, campaign.id)
    }
    val gameStateStore = remember(campaign.id) {
        GameStateStore(appContext, campaign.id)
    }
    var runtimeState by remember(campaign.id) {
        mutableStateOf(gameStateStore.load())
    }
    val characterStore = remember(campaign.id) {
        CharacterStore(appContext, campaign.id)
    }
    var storedCharacter by remember(campaign.id) {
        mutableStateOf(characterStore.load())
    }

    if (storedCharacter == null && campaign.ownerUid != null) {
        CharacterCreationScreen(
            campaignName = campaign.name,
            onCreated = { created ->
                characterStore.save(created)
                storedCharacter = created
            },
            onCancel = { showCampaignHub = true }
        )
        return
    }

    var selectedMember by remember(campaign.id) { mutableIntStateOf(0) }
    var currentLocationId by remember(campaign.id) {
        mutableStateOf(stateStore.loadLocation(manifest.startingLocation))
    }

    val characters = remember(campaign.id) {
        mutableStateListOf<CharacterState>().apply {
            add(storedCharacter ?: starterCharacters().first())
        }
    }

    val storedEvents = remember(campaign.id) { stateStore.loadEvents() }
    val events = remember(campaign.id) {
        mutableStateListOf<GameEvent>().apply {
            if (storedEvents.isNotEmpty()) {
                addAll(storedEvents)
            } else {
                add(
                    GameEvent(
                        "Campaign ready",
                        "Campaign loaded at saved location: " + currentLocationId + ".",
                        "event"
                    )
                )
            }
        }
    }

    val discovered = remember(campaign.id) {
        mutableStateListOf<String>().apply {
            addAll(stateStore.loadDiscovered(manifest.startingLocation))
        }
    }

    val syncRepo = remember(campaign.id, campaign.ownerUid) {
        if (campaign.ownerUid != null) CampaignSyncRepository(campaign.id)
        else null
    }

    DisposableEffect(syncRepo) {
        syncRepo?.listenEvents(
            onEvents = { incoming ->
                val known = events.map { it.id }.toHashSet()
                incoming.asReversed().forEach { event ->
                    if (known.add(event.id)) events.add(0, event)
                }
                stateStore.saveEvents(events)
                while (events.size > 250) events.removeAt(events.lastIndex)
            }
        )
        onDispose { syncRepo?.close() }
    }

    val sceneContext = remember(campaign.moduleId, currentLocationId) {
        moduleRepo.sceneContext(campaign.moduleId, currentLocationId)
    }

    val currentLocation =
        sceneContext?.location?.name ?: "Unresolved saved location [$currentLocationId]"
    val destinations =
        sceneContext?.destinations.orEmpty()
    val quests = remember(campaign.moduleId) {
        moduleRepo.quests(campaign.moduleId)
    }

    val portraits = listOf("🧝", "🧙", "🧔", "🥷")
    val party = characters.mapIndexed { index, character ->
        PartyMember(
            character.characterName,
            character.className,
            character.level,
            character.currentHp,
            character.maxHp,
            portraits.getOrElse(index) { "🧑" },
            "green"
        )
    }

    fun addEvent(event: GameEvent) {
        if (events.none { it.id == event.id }) {
            events.add(0, event)
        }
        stateStore.saveEvents(events)
        while (events.size > 250) events.removeAt(events.lastIndex)
        syncRepo?.publishEvent(event)
    }

    fun updateSelected(updated: CharacterState) {
        if (selectedMember in characters.indices) {
            characters[selectedMember] = updated
            characterStore.save(updated)
            storedCharacter = updated
        }
    }

    fun travel(destination: ModuleDestination): LocationTransition {
        val origin = currentLocation
        val savedId = stateStore.loadLocation(manifest.startingLocation)
        val result = LocationTransitionResolver.commit(
            destination, moduleRepo.sceneContext(campaign.moduleId, savedId),
            blocked = runtimeState.activeEncounter?.status == EncounterStatus.ACTIVE ||
                runtimeState.activeChallenge?.status == ChallengeStatus.ACTIVE
        ) { validatedId ->
            stateStore.saveLocation(validatedId)
            currentLocationId = validatedId
        }
        val validated = result.destination ?: return result

        if (!discovered.contains(validated.id)) {
            discovered.add(validated.id)
            stateStore.saveDiscovered(discovered.toSet())
        }

        addEvent(
            GameEvent(
                "Traveled to " + validated.name,
                validated.travelText.ifBlank {
                    "The party traveled from " + origin +
                        " to " + validated.name + "."
                },
                "travel"
            )
        )
        return result
    }

    if (showCampaignHub) {
        CampaignHubScreen(
            currentCampaign = campaign,
            onSelectCampaign = { selected ->
                campaignRegistry.upsert(selected)
                campaignRegistry.select(selected.id)
                campaign = selected
                showCampaignHub = false
            },
            onClose = { showCampaignHub = false }
        )
        return
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            LoreWiseHeader(
                current = current,
                onSelect = { current = it },
                onProfileClick = { showCampaignHub = true }
            )
        }
    ) { padding ->

        val contentModifier = Modifier.padding(padding)

        screenState.SaveableStateProvider(current.name) {
        when (current) {
            AppScreen.Home -> HomeScreen(
                modifier = contentModifier,
                campaignTitle = campaign.name,
                campaignDescription = manifest.description,
                currentLocation = sceneContext?.location,
                party = party,
                events = events,
                onNavigate = { current = it }
            )

            AppScreen.Play -> PlayScreen(
                session = playSession,
                scope = playScope,
                modifier = contentModifier,
                party = party,
                selectedMember = selectedMember,
                onSelectMember = { selectedMember = it },
                character = characters[selectedMember],
                campaignTitle = campaign.name,
                currentLocation = currentLocation,
                sceneContext = sceneContext,
                runtimeState = runtimeState,
                ruleset = manifest.ruleset,
                onAction = { action ->
                    addEvent(
                        GameEvent("Player action", action, "action")
                    )
                },
                onDiceRoll = { roll ->
                    addEvent(GameEvent("Dice roll", roll, "roll"))
                },
                recentEvents = events,
                onGmReply = { reply ->
                    addEvent(GameEvent("Game Master", reply, "gm"))
                },
                onStateEffects = { effects ->
                    val active = characters[selectedMember]
                    val application = GameStateEngine.apply(
                        character = active,
                        runtime = runtimeState,
                        effects = effects,
                        location = currentLocation,
                        ruleset = manifest.ruleset,
                        creatureResolver = { query ->
                            if (
                                GameRuleset.fromWire(manifest.ruleset) ==
                                GameRuleset.PF1E
                            ) {
                                Pf1CreatureCatalog.resolve(query)
                            } else {
                                rulesRepo.findCreatureCombatProfile(query)
                            }
                        }
                    )
                    if (application.character != active) {
                        updateSelected(application.character)
                    }
                    if (application.runtime != runtimeState) {
                        runtimeState = application.runtime
                        gameStateStore.save(application.runtime)
                    }
                    application.events.forEach(::addEvent)
                    application
                },
                prepareMovement = { action ->
                    val savedId = stateStore.loadLocation(manifest.startingLocation)
                    val liveScene = moduleRepo.sceneContext(campaign.moduleId, savedId)
                    val proposal = LocationTransitionResolver.propose(action, liveScene)
                    if (proposal.destination != null) {
                        travel(proposal.destination).message
                    } else proposal.message
                },
                authoritativeScene = {
                    val savedId = stateStore.loadLocation(manifest.startingLocation)
                    savedId to moduleRepo.sceneContext(campaign.moduleId, savedId)
                },
                archivedHistory = { stateStore.loadHistorySummary() },
                onXpAward = { amount ->
                    val active = characters[selectedMember]
                    val updated = active.copy(xp = active.xp + amount)
                    updateSelected(updated)
                    addEvent(
                        GameEvent(
                            "XP gained",
                            active.characterName + " earned " + amount + " XP.",
                            "xp"
                        )
                    )
                }
            )

            AppScreen.Map -> MapScreen(
                modifier = contentModifier,
                campaignTitle = campaign.name,
                party = party,
                currentLocation = sceneContext?.location,
                destinations = destinations,
                discoveredLocationIds = discovered.toSet(),
                onTravel = { travel(it) }
            )

            AppScreen.Journal -> JournalScreen(
                modifier = contentModifier,
                campaignTitle = campaign.name,
                quests = quests,
                events = events,
                stateStore = stateStore,
                onViewMap = { current = AppScreen.Map }
            )

            AppScreen.Glossary -> GlossaryScreen(
                modifier = contentModifier
            )

            AppScreen.Party -> CharacterSheetScreen(
                modifier = contentModifier,
                party = party,
                selectedMember = selectedMember,
                onSelectMember = { selectedMember = it },
                character = characters[selectedMember],
                onCharacterChange = ::updateSelected
            )

            AppScreen.Meta -> MetaChatScreen(
                modifier = contentModifier,
                gmContext = captureGmContext(
                    campaignTitle = campaign.name,
                    locationId = currentLocationId,
                    moduleScene = sceneContext,
                    character = characters[selectedMember],
                    party = party,
                    recentEvents = events,
                    runtimeState = runtimeState,
                    archivedSummary = stateStore.loadHistorySummary()
                ),
                scope = playScope
            )

            AppScreen.Transcripts -> TranscriptViewerScreen(modifier = contentModifier)

            AppScreen.Diagnostics -> DiagnosticsScreen(
                modifier = contentModifier
            )
        }
        }
    }
}
