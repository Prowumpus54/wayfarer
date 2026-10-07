package com.wayfarer.rpg

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    modifier: Modifier = Modifier,
    session: PlaySession,
    scope: kotlinx.coroutines.CoroutineScope,
    party: List<PartyMember>,
    selectedMember: Int,
    onSelectMember: (Int) -> Unit,
    character: CharacterState,
    campaignTitle: String,
    currentLocation: String,
    sceneContext: ModuleSceneContext?,
    runtimeState: CampaignRuntimeState,
    ruleset: String,
    onAction: (String) -> Unit,
    onDiceRoll: (String) -> Unit,
    recentEvents: List<GameEvent>,
    onGmReply: (String) -> Unit,
    onStateEffects: (List<GmEffect>) -> GameStateApplication,
    onXpAward: (Int) -> Unit
) {
    var input by session.input
    var lastAction by session.lastAction
    var check by session.check
    var checkName by session.checkName
    var gmNarration by session.gmNarration
    var gmStatus by session.gmStatus
    var gmBusy by session.gmBusy
    var failedAction by session.failedAction
    var showModelPicker by remember { mutableStateOf(false) }
    var openPanel by remember { mutableStateOf<String?>(null) }
    var jevStatus by remember { mutableStateOf("Jev ready") }
    var lastJevDecision by remember { mutableStateOf<JevCombatDecision?>(null) }

    var selectedDie by session.selectedDie
    var diceCountText by session.diceCountText
    var manualModifierText by session.manualModifierText
    var manualRoll by session.manualRoll
    var pendingRoll by session.pendingRoll
    var pendingCheck by session.pendingCheck
    var pendingContext by session.pendingContext
    var pendingActionEffects by session.pendingActionEffects
    var selectedTargetId by session.selectedTargetId
    var gmDiceModifiers by session.gmDiceModifiers

    val context = LocalContext.current
    val gmPrefs = remember {
        context.getSharedPreferences("wayfarer_gm", android.content.Context.MODE_PRIVATE)
    }
    var selectedModel by remember {
        val saved = gmPrefs.getString("model_choice", GmModelChoice.AUTO.name)
        mutableStateOf(
            runCatching { GmModelChoice.valueOf(saved ?: GmModelChoice.AUTO.name) }
                .getOrDefault(GmModelChoice.AUTO)
        )
    }

    val gameMaster = remember(selectedModel) { GeminiGameMaster(selectedModel) }
    val jevClient = remember { JevCombatClient() }

    fun pushGmModifiers(incoming: List<GmDiceModifier>) {
        if (incoming.isEmpty()) return
        val labels = incoming.map { it.label.lowercase() }.toSet()
        gmDiceModifiers = (
            gmDiceModifiers.filterNot { it.label.lowercase() in labels } +
                incoming
            ).takeLast(8)
    }

    fun rollManualDice() {
        if (gmBusy || pendingCheck != null) return
        val count = diceCountText.toIntOrNull()?.coerceIn(1, 20) ?: 1
        val manualModifier = manualModifierText.toIntOrNull()
            ?.coerceIn(-99, 99) ?: 0
        val gmModifier = gmDiceModifiers.sumOf { it.value }
        val result = DiceEngine.roll(
            sides = selectedDie,
            count = count,
            modifier = manualModifier + gmModifier
        )
        manualRoll = result
        pendingRoll = appendPlayerRoll(pendingRoll, character.characterName + " rolled " + result.expression +
            " (dice " + result.dice.joinToString(", ") + ") = " + result.total)

        val gmText = if (gmDiceModifiers.isEmpty()) {
            ""
        } else {
            " • GM " + gmDiceModifiers.joinToString(", ") {
                it.label + " " + signed(it.value)
            }
        }
        onDiceRoll(
            character.characterName + " rolled " + result.expression +
                " = " + result.total + gmText
        )

        gmDiceModifiers = gmDiceModifiers.filterNot { it.consumeOnRoll }
        gmStatus = "Roll recorded • supplied with your next action"
    }

    fun submitAction(text: String, logAction: Boolean = true) {
        val clean = text.trim()
        if (clean.isEmpty() || gmBusy || pendingCheck != null) return

        val castSpellName = clean
            .takeIf { it.startsWith("I cast ", ignoreCase = true) }
            ?.substring(7)
            ?.trim()
            ?.trimEnd('.')
            ?.takeIf { spell ->
                character.spells.values.flatten().any {
                    it.equals(spell, ignoreCase = true)
                }
            }
        val deferredSpellEffect = castSpellName?.let { spell ->
            val preview = GameStateEngine.consumeSpell(character, spell)
            if (preview == null) {
                gmStatus = "No spell resource remains for " + spell
                return
            }
            if (preview == character) null else GmEffect(
                type = GmEffectType.SPEND_SPELL_SLOT.wireName,
                name = spell
            )
        }

        val action = character.characterName + ": " + clean
        val rollForAction = pendingRoll
        val moduleScene = sceneContext
        val recentHistory = recentEvents
            .filter {
                it.type in setOf(
                    "action", "roll", "gm", "state", "encounter",
                    "loot", "challenge", "resource", "world", "xp"
                )
            }
            .take(24)
            .asReversed()
            .map { it.title + ": " + it.body }
        val context = GmContext(
            campaignTitle = campaignTitle,
            location = currentLocation,
            locationDescription = moduleScene?.location?.playerDescription.orEmpty(),
            gmNotes = moduleScene?.location?.gmNotes.orEmpty(),
            destinations = moduleScene?.destinations
                ?.map { it.name + if (it.travelText.isBlank()) "" else " — " + it.travelText }
                .orEmpty(),
            npcs = moduleScene?.npcs
                ?.map { it.name + if (it.role.isBlank()) "" else " (" + it.role + ")" }
                .orEmpty(),
            encounters = moduleScene?.let { scene ->
                scene.encounters.map { encounter ->
                    val creatures = scene.encounterCreatures
                        .filter { it.encounterId == encounter.id }
                        .joinToString("; ") {
                            it.quantity.toString() + " × " + it.creatureRuleRef +
                                if (it.role.isBlank()) "" else " (" + it.role + ")"
                        }
                    encounter.name + " [" + encounter.difficulty + "]: " +
                        encounter.gmNotes +
                        if (creatures.isBlank()) "" else "\nCreatures: " + creatures
                }
            }.orEmpty(),
            treasure = moduleScene?.treasure
                ?.map {
                    it.name + " ×" + it.quantity +
                        if (it.ruleRef.isBlank()) "" else " [" + it.ruleRef + "]" +
                        if (it.hidden) " [hidden]" else "" +
                        if (it.gmNotes.isBlank()) "" else ": " + it.gmNotes
                }
                .orEmpty(),
            character = character,
            party = party,
            recentHistory = recentHistory,
            runtimeState = GameStateEngine.contextLines(runtimeState),
            action = action,
            playerRoll = rollForAction
        )

        lastAction = action
        if (logAction) onAction(action)
        input = ""
        gmBusy = true
        failedAction = null
        jevStatus = "Jev routing…"
        gmStatus = "Routing action…"

        scope.launch {
            try {
                var jevDecision: JevCombatDecision? = null
                try {
                    val decision = jevClient.classify(
                        JevCombatInput(
                            action = clean,
                            actorName = character.characterName,
                            weapon = character.meleeWeapon,
                            scene = currentLocation + " — " +
                                moduleScene?.location?.playerDescription.orEmpty()
                        )
                    )
                    jevDecision = decision
                    lastJevDecision = decision
                    val route = JevCombatPolicy.route(decision)
                    val routeLabel = if (route.route == JevRoute.ANDROID_RULES) {
                        "rules"
                    } else {
                        "GM"
                    }
                    jevStatus = "Jev " + decision.latencyMs + "ms • " +
                        decision.actionType.name.lowercase() + " • " + routeLabel
                    gmStatus = if (route.route == JevRoute.ANDROID_RULES) {
                        "Fast combat route • Gemini narrating…"
                    } else {
                        "Gemini adjudicating…"
                    }
                } catch (cancel: CancellationException) { throw cancel
                } catch (jevError: Exception) {
                    lastJevDecision = null
                    jevStatus = if (
                        jevError.message.orEmpty().contains("Sign in", true)
                    ) {
                        "Jev available after sign-in"
                    } else {
                        "Jev unavailable • Gemini fallback"
                    }
                    gmStatus = "GM adjudicating…"
                }

                val fastIntent = jevDecision?.takeIf {
                    JevCombatPolicy.route(it).route == JevRoute.ANDROID_RULES
                }

                val localPf1Maneuver =
                    GameRuleset.fromWire(ruleset) == GameRuleset.PF1E &&
                    listOf("grapple", "trip", "bull rush", "shove", "disarm")
                        .any { clean.contains(it, true) }

                if (
                    (
                        fastIntent?.actionType == JevActionType.STRIKE ||
                            localPf1Maneuver
                        ) &&
                    rollForAction == null &&
                    deferredSpellEffect == null
                ) {
                    var stagedCharacter = character
                    var stagedRuntime = runtimeState
                    val enemySummaries = mutableListOf<String>()

                    fun runEnemyTurns() {
                        var guard = 0
                        while (guard++ < 12 && stagedCharacter.currentHp > 0) {
                            val encounter = stagedRuntime.activeEncounter ?: break
                            val currentTurn = encounter.initiativeOrder
                                .getOrNull(encounter.currentTurnIndex)
                                ?: break
                            if (currentTurn.actorType != CombatActorType.CREATURE) break

                            val before = stagedRuntime
                            val application = onStateEffects(
                                listOf(
                                    GmEffect(
                                        type = GmEffectType.CREATURE_STRIKE.wireName,
                                        name = currentTurn.actorId
                                    )
                                )
                            )
                            stagedCharacter = application.character
                            stagedRuntime = application.runtime
                            enemySummaries += application.events
                                .filter {
                                    it.type == "encounter" &&
                                        (it.title == "Enemy attack" ||
                                            it.title == "Enemy action blocked")
                                }
                                .map { it.body }
                            if (stagedRuntime == before) break
                        }
                    }

                    fun resolveBoundedAction(): CombatMechanicalResolution =
                        if (localPf1Maneuver) {
                            CombatRulesEngine.resolvePlayerManeuver(
                                character = stagedCharacter,
                                runtime = stagedRuntime,
                                selectedTargetId = selectedTargetId,
                                actionText = clean,
                                ruleset = ruleset
                            )
                        } else {
                            CombatRulesEngine.resolvePlayerStrike(
                                character = stagedCharacter,
                                runtime = stagedRuntime,
                                selectedTargetId = selectedTargetId,
                                actionText = clean,
                                ruleset = ruleset
                            )
                        }

                    var combat = resolveBoundedAction()

                    if (combat.status == CombatResolutionStatus.NEED_INITIATIVE) {
                        val initiativeApp = onStateEffects(combat.effects)
                        stagedCharacter = initiativeApp.character
                        stagedRuntime = initiativeApp.runtime
                        runEnemyTurns()
                        combat = resolveBoundedAction()
                    } else if (combat.status == CombatResolutionStatus.NOT_PLAYER_TURN) {
                        runEnemyTurns()
                        combat = resolveBoundedAction()
                    }

                    when (combat.status) {
                        CombatResolutionStatus.RESOLVED -> {
                            if (combat.rollText.isNotBlank()) {
                                onDiceRoll(combat.rollText)
                            }
                            val playerApp = onStateEffects(combat.effects)
                            stagedCharacter = playerApp.character
                            stagedRuntime = playerApp.runtime
                            if (selectedTargetId != null) {
                                val stillActive = stagedRuntime.activeEncounter
                                    ?.creatures
                                    ?.any {
                                        it.id == selectedTargetId &&
                                            it.status == CreatureStatus.ACTIVE &&
                                            it.currentHp > 0
                                    } == true
                                if (!stillActive) selectedTargetId = null
                            }
                            runEnemyTurns()

                            val narrationContext = context.copy(
                                character = stagedCharacter,
                                runtimeState = GameStateEngine.contextLines(stagedRuntime)
                            )
                            val mechanicalSummary = buildString {
                                append(combat.summary)
                                if (enemySummaries.isNotEmpty()) {
                                    append("\nSubsequent enemy turns:\n")
                                    append(enemySummaries.joinToString("\n"))
                                }
                            }
                            val narrated = gameMaster.narrateMechanicalResult(
                                narrationContext,
                                mechanicalSummary
                            )
                            gmNarration = narrated.narration
                            gmNarration?.let(onGmReply)
                            gmStatus = "Rules resolved • " +
                                narrated.modelName.removePrefix("gemini-")
                            pendingActionEffects = emptyList()
                            if (pendingRoll == rollForAction) pendingRoll = null
                            return@launch
                        }

                        CombatResolutionStatus.NEED_TARGET,
                        CombatResolutionStatus.NEED_VALIDATED_STATS,
                        CombatResolutionStatus.NOT_PLAYER_TURN -> {
                            gmNarration = combat.summary
                            gmNarration?.let(onGmReply)
                            gmStatus = "Combat needs input"
                            return@launch
                        }

                        CombatResolutionStatus.NEED_INITIATIVE -> {
                            gmNarration = "Initiative could not advance to the player's turn."
                            gmNarration?.let(onGmReply)
                            gmStatus = "Combat stalled"
                            return@launch
                        }

                        CombatResolutionStatus.NO_ACTIVE_ENCOUNTER,
                        CombatResolutionStatus.UNSUPPORTED_RULESET -> Unit
                    }
                }

                val turn = gameMaster.adjudicate(context, fastIntent)
                gmNarration = turn.narration
                pushGmModifiers(turn.modifiers)

                val request = turn.check
                if (request != null) {
                    val baseModifier = character.modifierForCheck(request.name)
                    if (baseModifier == null) {
                        gmStatus = "GM requested unsupported check: " + request.name
                    } else {
                        pendingCheck = request
                        pendingContext = context
                        pendingActionEffects = listOfNotNull(deferredSpellEffect)
                        if (turn.effects.isNotEmpty()) onStateEffects(turn.effects)
                        check = null
                        checkName = request.name
                        gmStatus = "Roll requested • " + request.name + " vs DC " + request.dc
                        openPanel = "dice"
                    }
                } else {
                    val effects = turn.effects + listOfNotNull(deferredSpellEffect)
                    if (effects.isNotEmpty()) onStateEffects(effects)
                    pendingActionEffects = emptyList()
                    gmStatus = gmModelStatus(turn.modelName)
                    if (turn.xpAward > 0) onXpAward(turn.xpAward)
                }
                gmNarration?.let(onGmReply)
                if (pendingRoll == rollForAction) pendingRoll = null
            } catch (cancel: CancellationException) { throw cancel
            } catch (error: Exception) {
                Log.e("LoreWiseGM", "adjudicate failed type=${error::class.java.name} message=${error.message}", error)
                failedAction = clean
                gmStatus = "GM error • " + (error.message ?: error::class.java.simpleName).take(90)
                gmNarration =
                    "The selected GM is temporarily unavailable. LoreWise kept your action " +
                    "so you can retry without losing the recorded intent or dice."
            } finally {
                gmBusy = false
            }
        }
    }

    fun resolveRequestedCheck() {
        val request = pendingCheck ?: return
        val gmContext = pendingContext ?: return
        if (gmBusy) return
        val baseModifier = character.modifierForCheck(request.name) ?: return
        val modifier = baseModifier + request.modifierAdjustment
        val isNewRoll = check == null
        val result = retainCheckOnRetry(check) {
            if (GameRuleset.fromWire(ruleset) == GameRuleset.PF1E) {
                val isSavingThrow = request.name.equals("Fortitude", true) ||
                    request.name.equals("Reflex", true) ||
                    request.name.equals("Will", true)
                DiceEngine.pf1Check(
                    modifier = modifier,
                    dc = request.dc,
                    automaticOnNatural = isSavingThrow
                )
            } else {
                DiceEngine.d20(modifier, request.dc)
            }
        }
        check = result
        checkName = request.name
        if (isNewRoll) onDiceRoll(
            character.characterName + " rolled " + request.name + " " +
                result.die + " + " + modifier + " = " + result.total +
                " vs DC " + result.dc + " (" + result.degree.name + ")"
        )
        gmBusy = true
        gmStatus = "Resolving " + request.name + "…"
        scope.launch {
            try {
                val resolved = gameMaster.resolve(gmContext, request, result)
                gmNarration = resolved.narration
                pushGmModifiers(resolved.modifiers)
                val effects = resolved.effects + pendingActionEffects
                if (effects.isNotEmpty()) onStateEffects(effects)
                gmNarration?.let(onGmReply)
                if (resolved.xpAward > 0) onXpAward(resolved.xpAward)
                gmStatus = gmModelStatus(resolved.modelName)
                pendingCheck = null
                pendingContext = null
                pendingActionEffects = emptyList()
                openPanel = null
            } catch (cancel: CancellationException) { throw cancel
            } catch (error: Exception) {
                Log.e("LoreWiseGM", "resolve failed type=${error::class.java.name} message=${error.message}", error)
                gmStatus = "GM error • " + (error.message ?: error::class.java.simpleName).take(90)
            } finally {
                gmBusy = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 34.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    campaignTitle.uppercase(),
                    color = Green,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp
                )
                Text(
                    currentLocation,
                    color = Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "PLAY",
                color = Gold,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp
            )
        }

        if (pendingCheck != null && openPanel != "dice") {
            TextButton(onClick = { openPanel = "dice" }) {
                Text(if (check == null) "Roll requested: " + pendingCheck!!.name else "Retry sending recorded check", color = Gold)
            }
        }
        if (pendingRoll != null && openPanel != "dice") {
            TextButton(onClick = { if (!gmBusy) submitAction("I use my recorded dice results.") }) {
                Text("Send recorded dice to GM", color = Green)
            }
        }
        if (runtimeState.activeEncounter != null || runtimeState.activeChallenge != null) {
            RuntimeStateCard(
                state = runtimeState,
                selectedTargetId = selectedTargetId,
                onSelectTarget = { selectedTargetId = it }
            )
            Spacer(Modifier.height(6.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }
        FramedCard(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(selectedModel) {
                    detectTapGestures(
                        onLongPress = { showModelPicker = true }
                    )
                }
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🕯️", fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Game Master",
                        color = Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        selectedModel.shortName + " • " + jevStatus,
                        color = if (jevStatus.contains("ms")) Green else Muted,
                        fontSize = 10.sp
                    )
                }
                Text(
                    gmStatus,
                    color = when {
                        gmStatus.startsWith("GM error") -> Danger
                        gmStatus.contains("Gemini") ||
                            gmStatus.contains("Local") ||
                            gmStatus.startsWith("Rules resolved") -> Green
                        else -> Muted
                    },
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val conversation = recentEvents
                    .filter {
                        it.type in setOf(
                            "action", "roll", "gm", "state", "encounter",
                            "loot", "challenge", "resource", "world", "xp"
                        )
                    }
                    .take(30)
                if (conversation.isEmpty()) {
                    item {
                        Text(
                            sceneContext?.location?.playerDescription
                                ?.takeIf { it.isNotBlank() } ?: sceneText(currentLocation),
                            color = Text,
                            fontFamily = FontFamily.Serif,
                            fontSize = 17.sp,
                            lineHeight = 26.sp
                        )
                    }
                }
                conversation.forEach { event ->
                    item(key = event.id) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(event.title.uppercase(),
                                color = if (event.type == "gm") Gold else Green,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(event.body, color = Text,
                                fontFamily = if (event.type == "gm") FontFamily.Serif else FontFamily.Default,
                                fontSize = if (event.type == "gm") 17.sp else 13.sp,
                                lineHeight = if (event.type == "gm") 26.sp else 19.sp)
                        }
                    }
                }
                if (gmStatus == "GM temporarily offline") {
                    item { Text(gmNarration.orEmpty(), color = Muted) }
                }

                if (gmBusy) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Green,
                            trackColor = Bg
                        )
                    }
                }

                check?.let { result ->
                    item { CheckCard(result, checkName) }
                }
                lastAction?.let { action ->
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Bg.copy(alpha = .65f))
                                .padding(10.dp)
                        ) {
                            Text(
                                "LAST ACTION",
                                color = Green,
                                fontSize = 9.sp,
                                letterSpacing = 1.4.sp
                            )
                            Text(
                                action,
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                failedAction?.let { retryText ->
                    item {
                        OutlinedButton(
                            onClick = {
                                submitAction(retryText, logAction = false)
                            },
                            enabled = !gmBusy
                        ) {
                            Text("Retry GM", color = Green)
                        }
                    }
                }
            }

            Text(
                "What do you do?",
                color = Gold,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionButton("🔎 Seek", Modifier.weight(1f)) {
                    submitAction("I carefully search the area.")
                }
                ActionButton("💬 Call", Modifier.weight(1f)) {
                    submitAction("I call out.")
                }
                ActionButton("⚔ Draw", Modifier.weight(1f)) {
                    submitAction("I draw " + character.meleeWeapon + ".")
                }
                ActionButton("🎲 Dice", Modifier.weight(1f)) {
                    openPanel = "dice"
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        PartyRail(
            party = party,
            selected = selectedMember,
            onSelect = onSelectMember
        )

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("What do you do?", color = Muted)
                },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Text,
                    unfocusedTextColor = Text,
                    focusedBorderColor = Green,
                    unfocusedBorderColor = GoldDark,
                    cursorColor = Green
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { submitAction(input) },
                enabled = !gmBusy && pendingCheck == null && input.isNotBlank(),
                modifier = Modifier.size(54.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenDark
                )
            ) {
                Text("➤", color = Text, fontSize = 22.sp)
            }
        }
    }

        if (openPanel != null) {
            Box(Modifier.fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
                .clickable { openPanel = null })
            Box(Modifier.fillMaxHeight().widthIn(max = 400.dp).fillMaxWidth(0.92f).padding(start = 26.dp)) {
                if (openPanel == "dice") {
                    DiceSideDrawer(
                        selectedDie = selectedDie, onSelectDie = { selectedDie = it },
                        countText = diceCountText, onCountChange = { diceCountText = it.filter(Char::isDigit).take(2) },
                        modifierText = manualModifierText, onModifierChange = { manualModifierText = it.take(3) },
                        gmModifiers = gmDiceModifiers,
                        onDismissGmModifier = { pushed -> gmDiceModifiers = gmDiceModifiers.filterNot { it == pushed } },
                        pendingCheck = pendingCheck, character = character, result = manualRoll.takeIf { pendingRoll != null },
                        onRoll = ::rollManualDice, onRollRequested = ::resolveRequestedCheck,
                        onSendManual = {
                            if (pendingRoll != null && !gmBusy) {
                                submitAction("I use my recorded dice results.")
                                openPanel = null
                            }
                        },
                        onClose = { openPanel = null }
                    )
                } else {
                    CharacterPlayPanel(openPanel!!, character, { openPanel = null }, { action ->
                        input = action
                        openPanel = null
                    }, Modifier.fillMaxSize())
                }
            }
        }

        PlayPanelRail(
            selected = openPanel,
            onSelect = { openPanel = it },
            modifier = Modifier.align(Alignment.CenterStart)
        )

    }

    androidx.activity.compose.BackHandler(enabled = openPanel != null) { openPanel = null }

    if (showModelPicker) {
        AlertDialog(
            onDismissRequest = { showModelPicker = false },
            title = { Text("Game Master Model", color = Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Auto uses local Gemma first when configured, then falls back to Gemini.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    GmModelChoice.entries.forEach { choice ->
                        TextButton(
                            onClick = {
                                selectedModel = choice
                                gmPrefs.edit()
                                    .putString("model_choice", choice.name)
                                    .apply()
                                gmStatus = "Ready"
                                failedAction = null
                                showModelPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedModel == choice,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(choice.displayName, color = Text)
                                if (choice == GmModelChoice.AUTO) {
                                    Text(
                                        "Local Gemma → Gemini 3.8 Flash → 3.5 Flash Lite",
                                        color = Muted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPicker = false }) {
                    Text("Close", color = Green)
                }
            },
            containerColor = Surface
        )
    }
}

private fun gmModelStatus(modelName: String): String = when {
    modelName.startsWith("local:") ->
        "Local " + modelName.removePrefix("local:")
    modelName.startsWith("gemini-") ->
        "Gemini " + modelName.removePrefix("gemini-")
    modelName.isBlank() -> "GM ready"
    else -> "GM " + modelName
}

@Composable
private fun SideHandle(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .width(58.dp)
            .height(92.dp)
            .clickable(onClick = onClick),
        color = GreenDark,
        shape = RoundedCornerShape(0.dp, 10.dp, 10.dp, 0.dp),
        tonalElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = Text,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CharacterActionsDrawer(
    character: CharacterState,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onAction: (String) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = 290.dp, max = 360.dp),
        color = Surface,
        tonalElevation = 10.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "ACTIONS",
                        color = Green,
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        character.characterName,
                        color = Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        character.className + " • Level " + character.level,
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
                TextButton(onClick = onClose) {
                    Text("Close", color = Gold)
                }
            }

            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ActionDrawerSection("Combat")
                }

                val melee = weaponCatalog.firstOrNull {
                    it.name == character.meleeWeapon
                }
                if (melee != null) {
                    item {
                        ActionDrawerRow(
                            "⚔ Strike — " + melee.name,
                            "+" + character.attackBonus(melee) +
                                " • " + melee.damageDice + " " + melee.damageType
                        ) {
                            onAction(
                                "I Strike with my " + melee.name + "."
                            )
                        }
                    }
                }

                val ranged = weaponCatalog.firstOrNull {
                    it.name == character.rangedWeapon
                }
                if (ranged != null && ranged.name != melee?.name) {
                    item {
                        ActionDrawerRow(
                            "🏹 Strike — " + ranged.name,
                            "+" + character.attackBonus(ranged) +
                                " • " + ranged.damageDice + " " + ranged.damageType
                        ) {
                            onAction(
                                "I Strike with my " + ranged.name + "."
                            )
                        }
                    }
                }
                if (character.isPf1()) {
                    val meleeWeapon = melee
                    if (
                        meleeWeapon != null &&
                        character.iterativeAttackBonuses(meleeWeapon).size > 1
                    ) {
                        item {
                            ActionDrawerRow(
                                "⚔ Full Attack — " + meleeWeapon.name,
                                character.iterativeAttackBonuses(meleeWeapon)
                                    .joinToString("/") { signed(it) }
                            ) {
                                onAction(
                                    "I make a Full Attack with my " +
                                        meleeWeapon.name + "."
                                )
                            }
                        }
                    }

                    item {
                        ActionDrawerRow(
                            "🛡 Fight Defensively",
                            "PF1 option • AC " + character.ac() +
                                " • tactical modifiers adjudicated by rules/GM"
                        ) {
                            onAction("I fight defensively.")
                        }
                    }

                    item {
                        ActionDrawerRow(
                            "🤼 Grapple",
                            "CMB " + signed(character.cmb()) +
                                " vs target CMD"
                        ) {
                            onAction("I attempt to Grapple my target.")
                        }
                    }

                    item {
                        ActionDrawerRow(
                            "↔ Bull Rush / Trip",
                            "CMB " + signed(character.cmb()) +
                                " vs target CMD"
                        ) {
                            onAction("I attempt to Trip my target.")
                        }
                    }
                } else {
                    item {
                        ActionDrawerRow(
                            "🛡 Defend / Raise Shield",
                            "AC " + character.ac() +
                                " • prepare for incoming attacks"
                        ) {
                            onAction(
                                "I defend myself and Raise a Shield if possible."
                            )
                        }
                    }
                }

                item {
                    ActionDrawerSection("Weapons")
                }

                item {
                    ActionDrawerRow(
                        "Select " + character.meleeWeapon,
                        "Current melee weapon"
                    ) {
                        onAction("I ready my " + character.meleeWeapon + ".")
                    }
                }
                item {
                    ActionDrawerRow(
                        "Select " + character.rangedWeapon,
                        "Current ranged weapon"
                    ) {
                        onAction("I ready my " + character.rangedWeapon + ".")
                    }
                }

                val spells = character.spells
                    .toSortedMap()
                    .flatMap { (level, names) ->
                        names.map { name -> level to name }
                    }

                if (spells.isNotEmpty()) {
                    item {
                        ActionDrawerSection("Spells")
                    }
                    items(spells.size) { index ->
                        val (level, spell) = spells[index]
                        ActionDrawerRow(
                            "✨ " + spell,
                            if (level == 0) {
                                if (character.isPf1()) {
                                    "0-level spell • not expended"
                                } else {
                                    "Cantrip"
                                }
                            } else {
                                "Spell level " + level
                            }
                        ) {
                            onAction("I cast " + spell + ".")
                        }
                    }
                }

                val feats = (
                    character.ancestryFeats +
                        character.classFeats +
                        character.skillFeats +
                        character.generalFeats +
                        character.bonusFeats
                    ).filter { it.isNotBlank() }.distinct()
                if (feats.isNotEmpty()) {
                    item {
                        ActionDrawerSection("Feats")
                    }
                    items(feats.size) { index ->
                        val feat = feats[index]
                        ActionDrawerRow(
                            "◆ " + feat,
                            "From character sheet"
                        ) {
                            onAction("I use my " + feat + " feat.")
                        }
                    }
                }

                val usableItems = character.inventory.filter {
                    it.quantity > 0 &&
                        (
                            it.category.contains("Consum", true) ||
                                it.category.contains("Tool", true) ||
                                it.mechanics.isNotBlank()
                            )
                }

                if (usableItems.isNotEmpty()) {
                    item {
                        ActionDrawerSection("Items")
                    }
                    items(usableItems.size) { index ->
                        val item = usableItems[index]
                        ActionDrawerRow(
                            item.icon + " " + item.name,
                            "x" + item.quantity +
                                if (item.mechanics.isBlank()) {
                                    ""
                                } else {
                                    " • " + item.mechanics
                                }
                        ) {
                            onAction("I use " + item.name + ".")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun ActionDrawerSection(title: String) {
    Text(
        title.uppercase(),
        color = Gold,
        fontSize = 10.sp,
        letterSpacing = 1.3.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun ActionDrawerRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .clickable(onClick = onClick)
            .padding(11.dp)
    ) {
        Text(
            title,
            color = Text,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DiceSideDrawer(
    selectedDie: Int,
    onSelectDie: (Int) -> Unit,
    countText: String,
    onCountChange: (String) -> Unit,
    modifierText: String,
    onModifierChange: (String) -> Unit,
    gmModifiers: List<GmDiceModifier>,
    onDismissGmModifier: (GmDiceModifier) -> Unit,
    pendingCheck: GmCheckRequest?,
    character: CharacterState,
    result: DiceRollResult?,
    onRoll: () -> Unit,
    onRollRequested: () -> Unit,
    onSendManual: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Surface,
        tonalElevation = 10.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DICE",
                    color = Green,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose) {
                    Text("Close", color = Gold)
                }
            }
            pendingCheck?.let { request ->
                val base = character.modifierForCheck(request.name) ?: 0
                val totalModifier = base + request.modifierAdjustment
                FramedCard(Modifier.fillMaxWidth()) {
                    Text("GM REQUESTED CHECK", color = Gold, fontSize = 10.sp)
                    Text(
                        request.name + " " + signed(totalModifier) +
                            " vs DC " + request.dc,
                        color = Text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (request.reason.isNotBlank()) {
                        Text(request.reason, color = Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(
                        "🎲 Roll " + request.name,
                        Modifier.fillMaxWidth(),
                        onRollRequested
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            DiceRollerCard(
                selectedDie = selectedDie,
                onSelectDie = onSelectDie,
                countText = countText,
                onCountChange = onCountChange,
                modifierText = modifierText,
                onModifierChange = onModifierChange,
                gmModifiers = gmModifiers,
                onDismissGmModifier = onDismissGmModifier,
                result = result,
                onRoll = onRoll
            )
            if (result != null && pendingCheck == null) {
                Spacer(Modifier.height(10.dp))
                PrimaryButton(
                    "Send Roll to Game Master",
                    Modifier.fillMaxWidth(),
                    onSendManual
                )
                Text(
                    "Manual rolls are recorded immediately and can be attached to your next GM action.",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun DiceRollerCard(
    selectedDie: Int,
    onSelectDie: (Int) -> Unit,
    countText: String,
    onCountChange: (String) -> Unit,
    modifierText: String,
    onModifierChange: (String) -> Unit,
    gmModifiers: List<GmDiceModifier>,
    onDismissGmModifier: (GmDiceModifier) -> Unit,
    result: DiceRollResult?,
    onRoll: () -> Unit
) {
    val gmTotal = gmModifiers.sumOf { it.value }
    val manualModifier = modifierText.toIntOrNull() ?: 0

    FramedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎲 Dice Roller", color = Text, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "Local RNG",
                color = Green,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("Die", color = Muted, fontSize = 11.sp)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(4, 6, 8, 10).forEach { sides ->
                DieChoice(
                    sides = sides,
                    selected = selectedDie == sides,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectDie(sides) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(12, 20, 100).forEach { sides ->
                DieChoice(
                    sides = sides,
                    selected = selectedDie == sides,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectDie(sides) }
                )
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = countText,
                onValueChange = onCountChange,
                label = { Text("Count") },
                placeholder = { Text("1") },
                singleLine = true,
                modifier = Modifier.weight(.75f),
                colors = diceFieldColors()
            )
            OutlinedTextField(
                value = modifierText,
                onValueChange = onModifierChange,
                label = { Text("Modifier") },
                placeholder = { Text("0") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = diceFieldColors()
            )
        }

        if (gmModifiers.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GM PUSHED MODIFIERS",
                    color = Gold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    signed(gmTotal),
                    color = if (gmTotal >= 0) Green else Danger,
                    fontWeight = FontWeight.Bold
                )
            }

            gmModifiers.forEach { pushed ->
                AssistChip(
                    onClick = { onDismissGmModifier(pushed) },
                    label = {
                        Text(
                            pushed.label + " " + signed(pushed.value) +
                                " • " + pushed.appliesTo
                        )
                    },
                    trailingIcon = { Text("×") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = Text
                    )
                )
            }
            Text(
                "Tap a GM modifier to dismiss it.",
                color = Muted,
                fontSize = 9.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        val totalModifier = manualModifier + gmTotal
        PrimaryButton(
            "Roll " +
                (countText.toIntOrNull()?.coerceIn(1, 20) ?: 1) +
                "d" + selectedDie +
                if (totalModifier == 0) "" else " " + signed(totalModifier),
            Modifier.fillMaxWidth(),
            onRoll
        )

        result?.let { roll ->
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Bg)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        roll.expression,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        roll.dice.joinToString(" + "),
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    roll.total.toString(),
                    color = Text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RowScope.DieChoice(
    sides: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("d" + sides) },
        modifier = modifier
    )
}

@Composable
private fun diceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Text,
    unfocusedTextColor = Text,
    focusedBorderColor = Green,
    unfocusedBorderColor = GoldDark,
    cursorColor = Green
)

private fun signed(value: Int): String =
    if (value >= 0) "+" + value else value.toString()

@Composable
private fun CheckCard(result: CheckResult, checkName: String) {
    val degree = when (result.degree) {
        Degree.CRITICAL_SUCCESS -> "Critical Success"
        Degree.SUCCESS -> "Success"
        Degree.FAILURE -> "Failure"
        Degree.CRITICAL_FAILURE -> "Critical Failure"
    }
    FramedCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎲", fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(checkName + " Check", color = Text, fontWeight = FontWeight.Bold)
                Text(
                    result.die.toString() + " + " + result.modifier + " = " + result.total + " • DC " + result.dc,
                    color = Muted
                )
            }
            Text(
                degree,
                color = if (result.degree == Degree.SUCCESS || result.degree == Degree.CRITICAL_SUCCESS) Green else Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

private fun locationSubtitle(location: String): String = when (location) {
    "Old Mill" -> "Abandoned mill beside the Willowbrook river"
    "Tavern" -> "Warm food, rumor, and questionable decisions"
    "Smithy" -> "Forge, repairs, arms, and armor"
    "Temple" -> "Stone sanctuary overlooking the river"
    "Forest Path" -> "A narrow trail leading north into dense woods"
    else -> "The party's current location"
}

private fun sceneText(location: String): String = when (location) {
    "Old Mill" -> "The old mill stands quiet at the edge of Willowbrook. Weathered timbers creak above the river, and the door hangs slightly open."
    "Tavern" -> "Heat and conversation spill from the tavern. A few locals glance toward your party."
    "Smithy" -> "Hammer blows ring across the yard as sparks scatter across the stone floor."
    "Temple" -> "The temple is cool and quiet. Thin daylight cuts through the high windows."
    "Forest Path" -> "The path narrows beneath the trees. Mud preserves several overlapping tracks heading deeper into the woods."
    else -> "Willowbrook settles around you: narrow streets, timber houses, distant voices, and several roads leading toward unfinished business."
}
