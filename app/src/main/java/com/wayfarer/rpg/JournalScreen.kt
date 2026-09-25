package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    campaignTitle: String,
    quests: List<ModuleQuestSummary>,
    events: List<GameEvent>,
    stateStore: CampaignStateStore,
    onViewMap: () -> Unit
) {
    var tab by remember { mutableStateOf("Quests") }
    var query by remember { mutableStateOf("") }
    var note by remember {
        mutableStateOf(stateStore.loadPlayerNote())
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Quests", "Journal", "Notes", "Events").forEach { label ->
                    JournalTab(label, tab == label, Modifier.weight(1f)) { tab = label }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search " + tab + "…", color = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Text,
                    unfocusedTextColor = Text,
                    focusedBorderColor = Green,
                    unfocusedBorderColor = GoldDark
                )
            )
        }

        when (tab) {
            "Quests" -> {
                if (quests.isEmpty()) {
                    item {
                        Text("No quests are available.", color = Muted)
                    }
                } else {
                    quests.filter {
                        query.isBlank() ||
                            it.title.contains(query, true) ||
                            it.description.contains(query, true)
                    }.forEachIndexed { index, quest ->
                        item {
                            FramedCard(Modifier.fillMaxWidth()) {
                                Text(
                                    if (index == 0) "✦ ADVENTURE HOOK ✦" else "QUEST",
                                    color = Green,
                                    fontSize = 11.sp,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    quest.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Text
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(quest.description, color = Muted)
                                Spacer(Modifier.height(8.dp))
                                PrimaryButton(
                                    "View on Map →",
                                    Modifier.fillMaxWidth(),
                                    onViewMap
                                )
                            }
                        }
                    }
                }
            }

            "Journal" -> {
                item {
                    FramedCard(Modifier.fillMaxWidth()) {
                        SectionTitle("Campaign Journal")
                        Text(
                            campaignTitle + " campaign log. " +
                                events.size + " recorded events.",
                            color = Text
                        )
                    }
                }

                val filtered = events.filter {
                    query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true)
                }.take(8)
                filtered.forEach { event ->
                    item { TimelineItem(iconFor(event.type), event.title, event.body, event.timeLabel) }
                }
            }

            "Notes" -> item {
                FramedCard(Modifier.fillMaxWidth()) {
                    Text("📌 PLAYER NOTES", color = Gold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            stateStore.savePlayerNote(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        placeholder = { Text("Write notes, clues, suspicions…", color = Muted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Text,
                            unfocusedTextColor = Text,
                            focusedBorderColor = Green,
                            unfocusedBorderColor = GoldDark
                        )
                    )
                }
            }

            "Events" -> {
                val filtered = events.filter {
                    query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true)
                }
                if (filtered.isEmpty()) {
                    item { Text("No matching events.", color = Muted) }
                } else {
                    filtered.forEach { event ->
                        item { TimelineItem(iconFor(event.type), event.title, event.body, event.timeLabel) }
                    }
                }
            }
        }
    }
}

private fun iconFor(type: String): String = when (type) {
    "travel" -> "🪧"
    "action" -> "⚔"
    "roll" -> "🎲"
    else -> "▤"
}

@Composable
private fun RowScope.JournalTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Text(
        label,
        color = if (selected) Text else Muted,
        fontSize = 12.sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GreenDark else Surface)
            .border(1.dp, if (selected) Green else GoldDark.copy(alpha = .5f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun QuestStep(done: Boolean, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (done) "●" else "○", color = if (done) Green else Muted, modifier = Modifier.width(24.dp))
        Text(text, color = if (done) Text else Muted)
    }
}

@Composable
private fun TimelineItem(icon: String, title: String, body: String, time: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(icon, fontSize = 27.sp, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Text, fontWeight = FontWeight.Bold)
            Text(body, color = Muted, fontSize = 13.sp)
        }
        Text(time, color = Muted, fontSize = 11.sp)
    }
}
