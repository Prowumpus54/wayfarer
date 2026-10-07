package com.wayfarer.rpg

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RuntimeStateCard(
    state: CampaignRuntimeState,
    selectedTargetId: String? = null,
    onSelectTarget: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val encounter = state.activeEncounter
    val challenge = state.activeChallenge
    if (encounter == null && challenge == null) return

    FramedCard(modifier.fillMaxWidth()) {
        encounter?.let {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "⚔ " + it.name,
                        color = Text,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        it.status.name + " • Round " + it.round,
                        color = Gold,
                        fontSize = 10.sp
                    )
                    it.initiativeOrder.getOrNull(it.currentTurnIndex)?.let { turn ->
                        Text(
                            "Turn: " + turn.name,
                            color = Green,
                            fontSize = 10.sp
                        )
                    }
                }
                val remaining = it.creatures.count { creature ->
                    creature.status == CreatureStatus.ACTIVE &&
                        creature.currentHp > 0
                }
                Text(
                    remaining.toString() + "/" + it.creatures.size + " active",
                    color = Muted,
                    fontSize = 10.sp
                )
            }

            if (it.creatures.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                it.creatures.take(8).forEach { creature ->
                    val selectable = creature.status == CreatureStatus.ACTIVE &&
                        creature.currentHp > 0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if (selectable) {
                                    Modifier.clickable { onSelectTarget(creature.id) }
                                } else {
                                    Modifier
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (if (selectedTargetId == creature.id) "◎ " else "") +
                                creature.name,
                            color = when {
                                selectedTargetId == creature.id -> Gold
                                creature.status == CreatureStatus.ACTIVE -> Text
                                else -> Muted
                            },
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (selectedTargetId == creature.id) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                        Text(
                            creature.currentHp.toString() + "/" + creature.maxHp + " HP" +
                                if (creature.statsResolved) "" else " • ? stats",
                            color = if (creature.currentHp > 0) Green else Danger,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (it.loot.isNotEmpty() || it.lootCp + it.lootSp + it.lootGp + it.lootPp > 0) {
                Spacer(Modifier.height(5.dp))
                val items = it.loot.joinToString(", ") { item ->
                    item.name + " ×" + item.quantity
                }
                val coins = listOf(
                    it.lootPp.takeIf { value -> value > 0 }?.let { value -> value.toString() + "pp" },
                    it.lootGp.takeIf { value -> value > 0 }?.let { value -> value.toString() + "gp" },
                    it.lootSp.takeIf { value -> value > 0 }?.let { value -> value.toString() + "sp" },
                    it.lootCp.takeIf { value -> value > 0 }?.let { value -> value.toString() + "cp" }
                ).filterNotNull().joinToString(" ")
                Text(
                    "Loot: " + listOf(items, coins).filter { value -> value.isNotBlank() }.joinToString(" • "),
                    color = Gold,
                    fontSize = 10.sp
                )
            }
        }

        challenge?.let {
            if (encounter != null) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "◆ " + it.name,
                    color = Text,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    it.progress.toString() + "/" + it.goal,
                    color = Green,
                    fontSize = 11.sp
                )
            }
            if (it.description.isNotBlank()) {
                Text(it.description, color = Muted, fontSize = 10.sp)
            }
        }
    }
}
