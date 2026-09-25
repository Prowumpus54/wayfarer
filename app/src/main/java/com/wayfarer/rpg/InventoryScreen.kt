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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val demoItems = listOf(
    InventoryItem("Longsword", "Weapons", 1, 3.0, "🗡️", "A reliable blade for any journey."),
    InventoryItem("Leather Armor", "Armor", 1, 5.0, "🥋", "Light and flexible protection."),
    InventoryItem("Health Potion", "Consumables", 3, 0.5, "🧪", "Restores a moderate amount of health."),
    InventoryItem("Rope", "Misc", 2, 1.0, "🪢", "Sturdy hemp rope."),
    InventoryItem("Torch", "Consumables", 5, 0.5, "🔥", "Lights the way in dark places."),
    InventoryItem("Rations", "Consumables", 4, 0.5, "🥖", "Simple, but keeps you going."),
    InventoryItem("Key Fragment", "Quest", 1, 0.1, "🗝️", "Part of a larger key."),
    InventoryItem("Traveler's Pack", "Misc", 1, 2.0, "🎒", "A sturdy pack with extra space.")
)

@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    party: List<PartyMember>,
    selectedMember: Int,
    onSelectMember: (Int) -> Unit
) {
    var category by remember { mutableStateOf("All") }
    val member = party[selectedMember]
    val visibleItems = if (category == "All") demoItems else demoItems.filter { it.category == category }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("Your Party")
            Spacer(Modifier.height(8.dp))
            PartyRail(party, selectedMember, onSelectMember)
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenDark.copy(alpha = .4f)),
                        contentAlignment = Alignment.Center
                    ) { Text(member.portrait, fontSize = 48.sp) }

                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.name, style = MaterialTheme.typography.headlineMedium, color = Text)
                        Text("Lv ${member.level} • ${member.className}", color = Gold)
                        Spacer(Modifier.height(8.dp))
                        HpBar(member.hp, member.maxHp, Modifier.fillMaxWidth())
                        Text("${member.hp}/${member.maxHp} HP", color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatMini("STR", "12")
                    StatMini("DEX", "17")
                    StatMini("CON", "14")
                    StatMini("WIS", "16")
                    StatMini("CHA", "10")
                }
            }
        }

        item {
            SectionTitle("Equipped Items")
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EquippedSlot("🗡️", "Longsword", Modifier.weight(1f))
                EquippedSlot("🥋", "Leather", Modifier.weight(1f))
                EquippedSlot("🧿", "Charm", Modifier.weight(1f))
                EquippedSlot("🧪", "Potion", Modifier.weight(1f))
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Weapons", "Armor", "Consumables", "Quest", "Misc").forEach { name ->
                    CategoryChip(name, category == name, Modifier.weight(1f)) { category = name }
                }
            }
        }

        visibleItems.chunked(2).forEach { pair ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { item -> ItemCard(item, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("↔ Split", Modifier.weight(1f)) { }
                PrimaryButton("Use 🧪", Modifier.weight(1f)) { }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value, color = Text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EquippedSlot(icon: String, name: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(icon, fontSize = 27.sp)
        Text(name, color = Text, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun RowScope.CategoryChip(name: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = name.take(5),
        color = if (selected) Text else Muted,
        fontSize = 9.sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) GreenDark else Surface)
            .border(1.dp, if (selected) Green else GoldDark.copy(alpha = .5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun ItemCard(item: InventoryItem, modifier: Modifier) {
    FramedCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.icon, fontSize = 30.sp)

            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = Text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(item.description, color = Muted, fontSize = 10.sp, maxLines = 2)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text("Qty ${item.quantity}   •   ${item.weight} lb", color = Gold, fontSize = 10.sp)
    }
}
