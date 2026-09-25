package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

data class PlayPanelTab(
    val id: String,
    val glyph: String,
    val label: String,
    val color: Color
)

val playPanelTabs = listOf(
    PlayPanelTab("attacks", "⚔", "Attacks", Green),
    PlayPanelTab("actions", "➤", "Actions", Green),
    PlayPanelTab("skills", "◆", "Skills", Gold),
    PlayPanelTab("items", "🎒", "Inventory", Color(0xFF4FA3A5)),
    PlayPanelTab("spells", "✦", "Spells", Color(0xFF9B7FD1)),
    PlayPanelTab("dice", "🎲", "Dice", Color(0xFFC97848))
)

@Composable
fun PlayPanelRail(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(26.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        playPanelTabs.forEach { tab ->
            val active = selected == tab.id
            Box(
                Modifier
                    .width(26.dp)
                    .semantics { contentDescription = tab.label }
                    .height(44.dp)
                    .clip(RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp))
                    .background(tab.color.copy(alpha = if (active) .95f else .45f))
                    .clickable {
                        onSelect(if (active) null else tab.id)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(tab.glyph, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CharacterPlayPanel(
    mode: String,
    character: CharacterState,
    onClose: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Surface,
        tonalElevation = 8.dp
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            val tab = playPanelTabs.first { it.id == mode }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tab.glyph, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    tab.label,
                    color = tab.color,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClose) {
                    Text("Close", color = Muted)
                }
            }
            PanelContents(mode, character, onAction)
        }
    }
}

@Composable
private fun ColumnScope.PanelContents(
    mode: String,
    character: CharacterState,
    onAction: (String) -> Unit
) {
    when (mode) {
        "actions" -> ActionsList(character, onAction, false)
        "attacks" -> ActionsList(character, onAction, true)
        "skills" -> SkillsList(character, onAction)
        "items" -> ItemsList(character, onAction)
        "spells" -> SpellsList(character, onAction)
    }
}

@Composable
private fun ColumnScope.ActionsList(
    character: CharacterState,
    onAction: (String) -> Unit,
    attacksOnly: Boolean
) {
    val melee = weaponCatalog.firstOrNull { it.name == character.meleeWeapon }
    val ranged = weaponCatalog.firstOrNull { it.name == character.rangedWeapon }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (attacksOnly) {
        melee?.let { weapon ->
            item {
                PlayPanelRow(
                    "⚔ Strike — " + weapon.name,
                    signedPanel(character.attackBonus(weapon)) +
                        " • " + weapon.damageDice + " " + weapon.damageType
                ) { onAction("I Strike with my " + weapon.name + ".") }
            }
        }
        ranged?.takeIf { it.name != melee?.name }?.let { weapon ->
            item {
                PlayPanelRow(
                    "🏹 Strike — " + weapon.name,
                    signedPanel(character.attackBonus(weapon)) +
                        " • " + weapon.damageDice + " " + weapon.damageType
                ) { onAction("I Strike with my " + weapon.name + ".") }
            }
        }
        } else {
        val sheetActions = (character.actionsAndActivities.lines() + character.freeActionsAndReactions.lines()).filter { it.isNotBlank() }.distinct()
        items(sheetActions.size) { index ->
            val action = sheetActions[index]
            PlayPanelRow(action, "From character sheet") { onAction("I use " + action + ".") }
        }
        item {
            PlayPanelRow(
                "🛡 Defend / Raise Shield",
                "AC " + character.ac()
            ) { onAction("I defend myself and Raise a Shield if possible.") }
        }
        item {
            PlayPanelRow("🤼 Grapple", "Athletics " +
                signedPanel(character.skillBonus(skillDefinitions.first { it.name == "Athletics" }))) {
                onAction("I attempt to Grapple my target.")
            }
        }
        item {
            PlayPanelRow("↔ Shove / Trip", "Athletics " +
                signedPanel(character.skillBonus(skillDefinitions.first { it.name == "Athletics" }))) {
                onAction("I attempt to Shove or Trip my target.")
            }
        }
        item {
            PlayPanelRow("◉ Aid", "Help an ally with their next action") {
                onAction("I Aid my ally with their next action.")
            }
        }
        item {
            PlayPanelRow("⧖ Ready", "Prepare an action for a trigger") {
                onAction("I Ready an action.")
            }
        }
        item {
            PlayPanelRow("✋ Interact", "Use or manipulate something nearby") {
                onAction("I Interact with the nearby object or environment.")
            }
        }
        val feats = (character.ancestryFeats + character.classFeats +
            character.skillFeats + character.generalFeats + character.bonusFeats)
            .filter { it.isNotBlank() }.distinct()
        items(feats.size) { index ->
            val feat = feats[index]
            PlayPanelRow("◆ " + feat, "Feat") {
                onAction("I use my " + feat + " feat.")
            }
        }
        }
    }
}

@Composable
private fun ColumnScope.SkillsList(
    character: CharacterState,
    onAction: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            PlayPanelRow(
                "Perception",
                signedPanel(character.perception())
            ) { onAction("I make a Perception check.") }
        }
        items(skillDefinitions.size) { index ->
            val skill = skillDefinitions[index]
            val bonus = character.skillBonus(skill)
            PlayPanelRow(
                skill.name,
                skill.ability.name + " • " + signedPanel(bonus)
            ) {
                onAction("I attempt a " + skill.name + " check.")
            }
        }
    }
}

@Composable
private fun ColumnScope.ItemsList(
    character: CharacterState,
    onAction: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (character.inventory.none { it.quantity > 0 }) {
            item { Text("No usable inventory on this character sheet.", color = Muted) }
        }
        items(character.inventory.size) { index ->
            val item = character.inventory[index]
            if (item.quantity > 0) PlayPanelRow(
                item.icon + " " + item.name,
                "x" + item.quantity +
                    if (item.mechanics.isBlank()) "" else " • " + item.mechanics
            ) {
                onAction("I use or ready " + item.name + ".")
            }
        }
    }
}

@Composable
private fun ColumnScope.SpellsList(
    character: CharacterState,
    onAction: (String) -> Unit
) {
    val spells = character.spells.toSortedMap().flatMap { (rank, names) ->
        names.map { rank to it }
    }
    if (spells.isEmpty()) {
        Text(
            "No spells are currently on this character sheet.",
            color = Muted,
            modifier = Modifier.padding(top = 12.dp)
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(spells.size) { index ->
            val (rank, spell) = spells[index]
            PlayPanelRow(
                "✦ " + spell,
                if (rank == 0) "Cantrip" else "Rank " + rank
            ) {
                onAction("I cast " + spell + ".")
            }
        }
    }
}

@Composable
private fun PlayPanelRow(
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
        Text(title, color = Text, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

private fun signedPanel(value: Int): String =
    if (value >= 0) "+" + value else value.toString()
