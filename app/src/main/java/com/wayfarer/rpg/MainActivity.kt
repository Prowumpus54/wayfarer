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
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        setContent {
            WayfarerTheme {
                WayfarerApp()
            }
        }
    }
}

@Composable
fun WayfarerApp() {
    val appContext = LocalContext.current.applicationContext
    val moduleId = "sunless_citadel"
    val moduleRepo = remember { AdventureModuleRepository(appContext) }

    val manifest = remember {
        moduleRepo.ensureBundledModuleInstalled(moduleId)
            ?: AdventureModuleManifest(
                id = moduleId,
                title = "The Sunless Citadel",
                version = "0",
                ruleset = "pf2e-adapted",
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
                        "The party is in Oakhurst, near the Old Road and the Sunless Citadel.",
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
                while (events.size > 250) events.removeAt(events.lastIndex)
                stateStore.saveEvents(events)
            }
        )
        onDispose { syncRepo?.close() }
    }

    val sceneContext = remember(campaign.moduleId, currentLocationId) {
        moduleRepo.sceneContext(campaign.moduleId, currentLocationId)
    }

    val currentLocation =
        sceneContext?.location?.name ?: "Oakhurst"
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
        while (events.size > 250) events.removeAt(events.lastIndex)
        stateStore.saveEvents(events)
        syncRepo?.publishEvent(event)
    }

    fun updateSelected(updated: CharacterState) {
        if (selectedMember in characters.indices) {
            characters[selectedMember] = updated
            characterStore.save(updated)
            storedCharacter = updated
        }
    }

    fun travel(destination: ModuleDestination) {
        val origin = currentLocation
        currentLocationId = destination.id
        stateStore.saveLocation(destination.id)

        if (!discovered.contains(destination.id)) {
            discovered.add(destination.id)
            stateStore.saveDiscovered(discovered.toSet())
        }

        addEvent(
            GameEvent(
                "Traveled to " + destination.name,
                destination.travelText.ifBlank {
                    "The party traveled from " + origin +
                        " to " + destination.name + "."
                },
                "travel"
            )
        )
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
            WayfarerHeader(
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
                onTravel = ::travel
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
        }
        }
    }
}
