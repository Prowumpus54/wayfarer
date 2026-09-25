package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WayfarerHeader(
    current: AppScreen,
    onSelect: (AppScreen) -> Unit,
    onProfileClick: () -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .statusBarsPadding()
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(42.dp)) {
            Text(
                "☰",
                color = Text,
                fontSize = 26.sp,
                modifier = Modifier
                    .clickable { menuOpen = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Surface
            ) {
                Text(
                    "Navigate",
                    color = Gold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                AppScreen.entries.forEach { screen ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                screen.glyph + "  " + screen.label,
                                color = if (screen == current) Green else Text
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onSelect(screen)
                        }
                    )
                }
                HorizontalDivider(color = GoldDark)
                Text(
                    "Wayfarer v" + BuildConfig.VERSION_NAME,
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
        Text("✥", color = Gold, fontSize = 30.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Wayfarer", color = Text, fontSize = 30.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            Text("STORIES AWAIT", color = Green, fontSize = 9.sp, letterSpacing = 3.sp)
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(50))
                .background(CardAlt)
                .border(1.dp, GoldDark, RoundedCornerShape(50))
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) { Text("🧝", fontSize = 24.sp) }
    }
}

@Composable
fun WayfarerBottomBar(current: AppScreen, onSelect: (AppScreen) -> Unit) {
    NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
        AppScreen.entries.forEach { screen ->
            val selected = current == screen
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(screen) },
                icon = {
                    Text(
                        screen.glyph,
                        color = if (selected) Green else Muted,
                        fontSize = 23.sp,
                        textAlign = TextAlign.Center
                    )
                },
                label = {
                    Text(screen.label, color = if (selected) Green else Muted, fontSize = 11.sp)
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = GreenDark.copy(alpha = .35f)
                )
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Text, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Green, fontSize = 13.sp)
    }
}

@Composable
fun FramedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CardAlt.copy(alpha = .98f), Surface.copy(alpha = .98f))
                )
            )
            .border(1.dp, GoldDark.copy(alpha = .8f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun HpBar(current: Int, max: Int, modifier: Modifier = Modifier) {
    val fraction = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = { fraction },
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(99.dp)),
        color = Danger,
        trackColor = Bg
    )
}

@Composable
fun PartyRail(
    party: List<PartyMember>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        party.forEachIndexed { index, member ->
            val active = selected == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) GreenDark.copy(alpha = .45f) else Card)
                    .border(if (active) 2.dp else 1.dp, if (active) Green else GoldDark, RoundedCornerShape(10.dp))
                    .clickable { onSelect(index) }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(member.portrait, fontSize = 27.sp)
                Text(member.name, color = Text, fontSize = 12.sp, maxLines = 1)
                HpBar(member.hp, member.maxHp, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldDark),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Text)
    ) {
        Text(text, fontFamily = FontFamily.Serif, fontSize = 14.sp)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GreenDark, contentColor = Text)
    ) {
        Text(text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
    }
}
