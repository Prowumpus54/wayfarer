package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    campaignTitle: String,
    campaignDescription: String,
    currentLocation: ModuleLocation?,
    party: List<PartyMember>,
    events: List<GameEvent>,
    onNavigate: (AppScreen) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            FramedCard(
                Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AppScreen.Play) }
            ) {
                Text("✦ ACTIVE CAMPAIGN ✦", color = Green, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    campaignTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Text
                )
                Text(campaignDescription, color = Muted)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(GreenDark.copy(alpha = .85f), CardAlt, GoldDark.copy(alpha = .5f))
                            )
                        )
                ) {
                    Text(
                        if (currentLocation?.kind == "town") "🌲   🏘️   🛤️   🌲"
                        else "🕯️   🏚️   🪨   🌑",
                        fontSize = 34.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        currentLocation?.name ?: "Oakhurst",
                        color = Text,
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Continue  →", Modifier.fillMaxWidth()) { onNavigate(AppScreen.Play) }
            }
        }

        item {
            SectionTitle(
                "Your Party",
                "Lv " + (party.firstOrNull()?.level ?: 1)
            )
            Spacer(Modifier.height(8.dp))
            PartyRail(party, 0) { onNavigate(AppScreen.Party) }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeTile("⚔", "Play", "Continue your story", Modifier.weight(1f)) { onNavigate(AppScreen.Play) }
                HomeTile("📜", "Character", "Stats & feats", Modifier.weight(1f)) { onNavigate(AppScreen.Party) }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeTile("🎒", "Inventory", "Items & gear", Modifier.weight(1f)) { onNavigate(AppScreen.Party) }
                HomeTile("🗺", "Map", "Explore the realm", Modifier.weight(1f)) { onNavigate(AppScreen.Map) }
            }
        }
        item {
            HomeTile("📖", "Log", "Quests, notes & events", Modifier.fillMaxWidth()) { onNavigate(AppScreen.Journal) }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Events", style = MaterialTheme.typography.titleLarge, color = Text, modifier = Modifier.weight(1f))
                    Text(
                        "View all →",
                        color = Green,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onNavigate(AppScreen.Journal) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                events.take(3).forEach { event ->
                    val glyph = when (event.type) {
                        "travel" -> "🪧"
                        "action" -> "⚔"
                        "roll" -> "🎲"
                        else -> "▤"
                    }
                    EventLine(glyph, event.title, event.timeLabel)
                }
            }
        }
    }
}

@Composable
private fun HomeTile(
    glyph: String,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FramedCard(modifier.clickable(onClick = onClick)) {
        Text(glyph, fontSize = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text(title, color = Text, fontSize = 19.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun EventLine(glyph: String, title: String, time: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(glyph, color = Gold, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Text, fontSize = 14.sp)
            Text(time, color = Muted, fontSize = 11.sp)
        }
    }
}
