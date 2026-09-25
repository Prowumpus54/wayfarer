package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CharacterCreationScreen(
    campaignName: String,
    onCreated: (CharacterState) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pronouns by remember { mutableStateOf("") }
    var concept by remember {
        mutableStateOf("A brave adventurer with a memorable personality")
    }
    var personality by remember { mutableStateOf("") }
    var combatStyle by remember { mutableStateOf("Surprise me") }
    var magic by remember { mutableStateOf("Surprise me") }
    var complexity by remember { mutableStateOf("Simple") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<CharacterState?>(null) }

    val scope = rememberCoroutineScope()
    val architect = remember { CharacterArchitect() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "CREATE YOUR HERO",
                    color = Green,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    campaignName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Text
                )
            }
            TextButton(onClick = onCancel) {
                Text("Campaigns", color = Gold)
            }
        }
        Text(
            "Describe the hero you want to play. Wayfarer will handle the rules, feats, spells and starting equipment.",
            color = Muted
        )

        FramedCard(Modifier.fillMaxWidth()) {
            SectionTitle("Your idea")
            Spacer(Modifier.height(8.dp))
            CreationField("Character name (optional)", name) { name = it }
            Spacer(Modifier.height(8.dp))
            CreationField("Pronouns (optional)", pronouns) { pronouns = it }
            Spacer(Modifier.height(8.dp))
            CreationField(
                "Describe your character",
                concept,
                minLines = 3
            ) { concept = it }
            Spacer(Modifier.height(8.dp))
            CreationField(
                "Personality",
                personality,
                placeholder = "Funny, serious, reckless, kind, mysterious…"
            ) { personality = it }
        }
        FramedCard(Modifier.fillMaxWidth()) {
            SectionTitle("How they play")
            Spacer(Modifier.height(8.dp))
            ChoiceRow(
                "Combat",
                combatStyle,
                listOf(
                    "Surprise me",
                    "Up close",
                    "Ranged",
                    "Sneaky",
                    "Protect others",
                    "Support"
                )
            ) { combatStyle = it }
            Spacer(Modifier.height(10.dp))
            ChoiceRow(
                "Magic",
                magic,
                listOf(
                    "Surprise me",
                    "No magic",
                    "A little magic",
                    "Lots of magic",
                    "Healer"
                )
            ) { magic = it }
            Spacer(Modifier.height(10.dp))
            ChoiceRow(
                "Rules complexity",
                complexity,
                listOf("Simple", "Medium", "Advanced")
            ) { complexity = it }
        }

        if (preview == null) {
            PrimaryButton(
                if (busy) "Building your hero…" else "✨ Build My Character",
                Modifier.fillMaxWidth()
            ) {
                if (concept.isBlank() || busy) return@PrimaryButton
                busy = true
                error = ""
                scope.launch {
                    try {
                        preview = architect.create(
                            CharacterBrief(
                                name = name,
                                pronouns = pronouns,
                                concept = concept,
                                personality = personality,
                                combatStyle = combatStyle,
                                magicPreference = magic,
                                complexity = complexity
                            )
                        )
                    } catch (t: Throwable) {
                        error = t.message
                            ?: "Wayfarer could not build the character."
                    } finally {
                        busy = false
                    }
                }
            }
            TextButton(
                onClick = {
                    onCreated(
                        CharacterState(
                            playerName = "",
                            characterName = name.ifBlank { "New Hero" },
                            level = 1,
                            genderPronouns = pronouns,
                            notes = concept
                        )
                    )
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Build manually from a blank sheet", color = Gold)
            }
        } else {
            HeroPreview(
                character = preview!!,
                onUse = { onCreated(preview!!) },
                onRegenerate = {
                    preview = null
                    error = ""
                }
            )
        }

        if (busy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Green,
                trackColor = CardAlt
            )
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = Danger,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        }
    }
}
@Composable
private fun HeroPreview(
    character: CharacterState,
    onUse: () -> Unit,
    onRegenerate: () -> Unit
) {
    FramedCard(Modifier.fillMaxWidth()) {
        Text(
            character.characterName,
            color = Text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            character.ancestry + " • " +
                character.background + " • " +
                character.className,
            color = Green
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "HP " + character.maxHp +
                "  •  AC " + character.ac() +
                "  •  Speed " + character.speed + " ft",
            color = Gold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Weapons: " + character.meleeWeapon +
                " • " + character.rangedWeapon,
            color = Text
        )
        if (character.classFeats.isNotEmpty()) {
            Text(
                "Feats: " + character.classFeats.joinToString(", "),
                color = Muted
            )
        }
        val spellNames = character.spells.values.flatten()
        if (spellNames.isNotEmpty()) {
            Text(
                "Spells: " + spellNames.take(6).joinToString(", "),
                color = Muted
            )
        }
        if (character.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(character.notes, color = Muted)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Use This Character", Modifier.fillMaxWidth(), onUse)
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = onRegenerate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try another version", color = Gold)
        }
    }
}

@Composable
private fun CreationField(
    label: String,
    value: String,
    placeholder: String = "",
    minLines: Int = 1,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = {
            if (placeholder.isNotBlank()) Text(placeholder)
        },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Text,
            unfocusedTextColor = Text,
            focusedBorderColor = Green,
            unfocusedBorderColor = GoldDark,
            cursorColor = Green
        )
    )
}
@Composable
private fun ChoiceRow(
    label: String,
    selected: String,
    choices: List<String>,
    onSelected: (String) -> Unit
) {
    Text(label, color = Muted, fontSize = 11.sp)
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(
                androidx.compose.foundation.rememberScrollState()
            ),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        choices.forEach { choice ->
            FilterChip(
                selected = selected == choice,
                onClick = { onSelected(choice) },
                label = { Text(choice) }
            )
        }
    }
}
