package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class GlossaryCategory(
    val key: String,
    val label: String
)

private val glossaryCategories = listOf(
    GlossaryCategory("all", "All"),
    GlossaryCategory("action", "Actions"),
    GlossaryCategory("condition", "Conditions"),
    GlossaryCategory("class", "Classes"),
    GlossaryCategory("ancestry", "Ancestries"),
    GlossaryCategory("heritage", "Heritages"),
    GlossaryCategory("background", "Backgrounds"),
    GlossaryCategory("feat", "Feats"),
    GlossaryCategory("spell", "Spells"),
    GlossaryCategory("equipment", "Equipment"),
    GlossaryCategory("creature", "Creatures"),
    GlossaryCategory("deity", "Deities"),
    GlossaryCategory("hazard", "Hazards"),
    GlossaryCategory("class_feature", "Class Features"),
    GlossaryCategory("ancestry_feature", "Ancestry Features"),
    GlossaryCategory("equipment_effect", "Equipment Effects"),
    GlossaryCategory("feat_effect", "Feat Effects"),
    GlossaryCategory("spell_effect", "Spell Effects"),
    GlossaryCategory("familiar_ability", "Familiar Abilities")
)

@Composable
fun GlossaryScreen(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rules = remember { RulesRepository(context.applicationContext) }
    DisposableEffect(rules) { onDispose { rules.close() } }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    var selected by remember { mutableStateOf<RuleEntrySummary?>(null) }

    val results = remember(query, category) {
        if (category == "all") {
            rules.searchAll(query = query, limit = 300)
        } else {
            rules.search(
                kind = category,
                query = query,
                remasterOnly = false,
                limit = 300
            )
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Bg)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            "GLOSSARY",
            color = Green,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp
        )
        Text(
            "Rules & Reference",
            color = Text,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Search the bundled PF2e reference. PF1e rules are not included.",
            color = Muted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search feats, spells, rules, gear…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Text,
                unfocusedTextColor = Text,
                focusedBorderColor = Green,
                unfocusedBorderColor = GoldDark,
                cursorColor = Green
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            glossaryCategories.forEach { item ->
                FilterChip(
                    selected = category == item.key,
                    onClick = { category = item.key },
                    label = { Text(item.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            results.size.toString() + " results" +
                if (results.size >= 300) " • refine search" else "",
            color = Muted,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(6.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(results.size) { index ->
                val entry = results[index]
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Card)
                        .clickable { selected = entry }
                        .padding(12.dp)
                ) {
                    Text(
                        entry.name,
                        color = Text,
                        fontWeight = FontWeight.Bold
                    )
                    val meta = buildList {
                        entry.category?.takeIf { it.isNotBlank() }?.let(::add)
                        if (entry.level > 0) add("Level " + entry.level)
                        if (entry.traits.isNotEmpty()) {
                            add(entry.traits.take(4).joinToString(", "))
                        }
                    }.joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(meta, color = Green, fontSize = 10.sp)
                    }
                    if (entry.description.isNotBlank()) {
                        Text(
                            glossaryExcerpt(entry.description),
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
    selected?.let { entry ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(entry.name, color = Text) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        buildList {
                            if (entry.level > 0) add("Level " + entry.level)
                            entry.rarity?.takeIf { it.isNotBlank() }?.let(::add)
                            entry.category?.takeIf { it.isNotBlank() }?.let(::add)
                        }.joinToString(" • "),
                        color = Green,
                        fontSize = 11.sp
                    )
                    if (entry.traits.isNotEmpty()) {
                        Text(
                            "Traits: " + entry.traits.joinToString(", "),
                            color = Gold,
                            fontSize = 11.sp
                        )
                    }
                    if (entry.traditions.isNotEmpty()) {
                        Text(
                            "Traditions: " + entry.traditions.joinToString(", "),
                            color = Gold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(entry.description, color = Text, fontSize = 13.sp)
                    entry.sourceTitle?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Source: " + it, color = Muted, fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null }) {
                    Text("Close", color = Green)
                }
            },
            containerColor = Surface
        )
    }
}

private fun glossaryExcerpt(text: String, max: Int = 220): String {
    val clean = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
    return if (clean.length <= max) clean else clean.take(max).trimEnd() + "…"
}
