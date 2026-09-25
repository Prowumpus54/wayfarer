package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val sheetTabs = listOf("Core", "Feats & Inventory", "Character & Actions", "Spells")

@Composable
fun CharacterSheetScreen(
    modifier: Modifier = Modifier,
    party: List<PartyMember>,
    selectedMember: Int,
    onSelectMember: (Int) -> Unit,
    character: CharacterState,
    onCharacterChange: (CharacterState) -> Unit
) {
    var tab by remember { mutableStateOf("Core") }
    var showLevelUp by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val rules = remember { RulesRepository(context.applicationContext) }
    DisposableEffect(rules) { onDispose { rules.close() } }

    Column(modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Character Sheet",
                    style = MaterialTheme.typography.titleLarge,
                    color = Text,
                    modifier = Modifier.weight(1f)
                )
                Text("Lv " + character.level, color = Gold)
                Spacer(Modifier.width(8.dp))
                if (character.level < 20) {
                    Button(
                        onClick = { showLevelUp = true },
                        enabled = character.xp >= 1000,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (character.xp >= 1000) "Level Up" else character.xp.toString() + "/1000 XP",
                            color = Text,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            PartyRail(party, selectedMember, onSelectMember)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sheetTabs.forEach { name ->
                    SheetTab(name, tab == name) { tab = name }
                }
            }
        }

        CharacterAdviceBar(character, rules, onSaveAction = { action -> onCharacterChange(character.copy(actionsAndActivities = (character.actionsAndActivities.lines() + action).filter { it.isNotBlank() }.distinct().joinToString("\n"))) })
        when (tab) {
            "Core" -> CoreSheet(character, onCharacterChange, rules)
            "Feats & Inventory" -> FeatsInventorySheet(character, onCharacterChange, rules)
            "Character & Actions" -> CharacterActionsSheet(character, onCharacterChange)
            else -> SpellsSheet(character, onCharacterChange, rules)
        }
    }

    if (showLevelUp) {
        LevelUpDialog(
            character = character,
            rules = rules,
            onDismiss = { showLevelUp = false },
            onConfirm = { updated ->
                onCharacterChange(updated)
                showLevelUp = false
                tab = "Feats & Inventory"
            }
        )
    }
}

@Composable
private fun SheetTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) Text else Muted,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) GreenDark else Surface)
            .border(1.dp, if (selected) Green else GoldDark, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun SmartDropdown(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Text("▾", color = Green) },
            modifier = Modifier.fillMaxWidth(),
            colors = sheetFieldColors()
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        if (options.size <= 80) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    if (expanded && options.size > 80) {
        val matches = remember(options, query) {
            if (query.isBlank()) options else options.filter {
                it.contains(query, ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("Choose " + label, color = Text) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search " + options.size + " options") },
                        singleLine = true,
                        colors = sheetFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())
                    ) {
                        matches.take(80).forEach { option ->
                            Text(
                                option,
                                color = Text,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        onChange(option)
                                        expanded = false
                                        query = ""
                                    }
                                    .padding(vertical = 9.dp, horizontal = 4.dp)
                            )
                        }
                        if (matches.size > 80) {
                            Text(
                                "Showing first 80 matches — refine your search.",
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = false }) {
                    Text("Close", color = Muted)
                }
            },
            containerColor = Surface
        )
    }
}

@Composable
private fun RulePicker(
    label: String,
    value: String,
    options: List<RuleEntrySummary>,
    modifier: Modifier = Modifier,
    selectedInfo: RuleEntrySummary? = null,
    onChange: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selected = selectedInfo
        ?: options.firstOrNull { it.name.equals(value, true) }

    Column(modifier) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (value.isBlank()) "—" else value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { Text("ⓘ ▾", color = Green) },
                modifier = Modifier.fillMaxWidth(),
                colors = sheetFieldColors()
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable { open = true }
            )
        }
        if (selected != null) {
            RuleInfoSummary(selected) { open = true }
        }
    }

    if (open) {
        val matches = remember(options, query) {
            if (query.isBlank()) options else options.filter {
                it.name.contains(query, true) ||
                    it.description.contains(query, true) ||
                    it.traits.any { trait -> trait.contains(query, true) }
            }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Choose " + label, color = Text) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search " + options.size + " options") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = sheetFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier.heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "—  None / not selected",
                            color = Muted,
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    onChange("—")
                                    open = false
                                    query = ""
                                }
                                .padding(vertical = 10.dp)
                        )
                        matches.take(80).forEach { info ->
                            RuleChoiceRow(info) {
                                onChange(info.name)
                                open = false
                                query = ""
                            }
                        }
                        if (matches.size > 80) {
                            Text(
                                "Showing first 80 matches. Refine the search to narrow the list.",
                                color = Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text("Close", color = Muted)
                }
            },
            containerColor = Surface
        )
    }
}

@Composable
private fun RuleChoiceRow(
    info: RuleEntrySummary,
    onClick: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp)
    ) {
        Text(info.name, color = Text, fontWeight = FontWeight.Bold)
        val meta = buildList {
            if (info.level > 0) add("Level " + info.level)
            info.rarity?.takeIf { it.isNotBlank() }?.let { add(it.replaceFirstChar(Char::uppercase)) }
            if (info.traits.isNotEmpty()) add(info.traits.take(4).joinToString(", "))
        }.joinToString(" • ")
        if (meta.isNotBlank()) Text(meta, color = Green, fontSize = 11.sp)
        if (info.description.isNotBlank()) {
            Text(shortRuleText(info.description), color = Muted, fontSize = 12.sp)
        }
        HorizontalDivider(color = GoldDark.copy(alpha = .35f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun RuleInfoSummary(
    info: RuleEntrySummary,
    onDetails: (() -> Unit)? = null
) {
    Spacer(Modifier.height(5.dp))
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Card.copy(alpha = .55f))
            .padding(8.dp)
    ) {
        val meta = buildList {
            if (info.level > 0) add("Level " + info.level)
            if (info.traits.isNotEmpty()) add(info.traits.take(5).joinToString(", "))
        }.joinToString(" • ")
        if (meta.isNotBlank()) Text(meta, color = Green, fontSize = 10.sp)
        if (info.description.isNotBlank()) {
            Text(shortRuleText(info.description), color = Muted, fontSize = 11.sp)
        }
        if (onDetails != null) {
            Text(
                "More details",
                color = Gold,
                fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onDetails)
                    .padding(top = 4.dp)
            )
        }
    }
}

private fun shortRuleText(text: String, max: Int = 260): String {
    val clean = text.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
    return if (clean.length <= max) clean else clean.take(max).trimEnd() + "…"
}

@Composable
private fun SmartText(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
        ),
        colors = sheetFieldColors()
    )
}

@Composable
private fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Text,
    unfocusedTextColor = Text,
    focusedBorderColor = Green,
    unfocusedBorderColor = GoldDark,
    focusedLabelColor = Green,
    unfocusedLabelColor = Muted,
    cursorColor = Green
)

@Composable
private fun ProficiencyPicker(
    label: String,
    value: Proficiency,
    modifier: Modifier = Modifier,
    onChange: (Proficiency) -> Unit
) {
    SmartDropdown(
        label = label,
        value = value.code,
        options = Proficiency.entries.map { it.code },
        modifier = modifier
    ) { code ->
        onChange(Proficiency.entries.first { it.code == code })
    }
}

@Composable
private fun CoreSheet(
    character: CharacterState,
    onChange: (CharacterState) -> Unit,
    rules: RulesRepository
) {
    val ancestryOptions = remember { rules.names("ancestry") }
    val heritageOptions = remember { rules.names("heritage") }
    val backgroundOptions = remember { rules.names("background") }
    val classOptions = remember { rules.names("class") }
    val deityOptions = remember { rules.names("deity") }
    val weaponRuleOptions = remember {
        weaponCatalog.map { weapon ->
            rules.findByName("equipment", weapon.name) ?: RuleEntrySummary(
                uid = "local:" + weapon.name,
                name = weapon.name,
                level = 0,
                category = "weapon",
                rarity = "common",
                traits = weapon.traits.split(",").map { it.trim() },
                traditions = emptyList(),
                description = weapon.damageDice + " " + weapon.damageType +
                    ". " + weapon.category + " weapon. " + weapon.traits,
                sourceTitle = "Wayfarer local rules",
                remaster = true
            )
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Identity")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartText("Character Name", character.characterName, Modifier.weight(1f)) {
                        onChange(character.copy(characterName = it))
                    }
                    SmartText("Player Name", character.playerName, Modifier.weight(1f)) {
                        onChange(character.copy(playerName = it))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcStat("XP", character.xp, Modifier.weight(1f))
                    CalcStat("Level", character.level, Modifier.weight(1f))
                    SmartText("Hero Points", character.heroPoints.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(heroPoints = (it.toIntOrNull() ?: character.heroPoints).coerceIn(0, 3)))
                    }
                }
                Text(
                    "Level advancement unlocks at 1000 XP. XP is awarded through play.",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Ancestry, Background & Class")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartDropdown(
                        "Ancestry", character.ancestry,
                        ancestryOptions,
                        Modifier.weight(1f)
                    ) { onChange(character.copy(ancestry = it)) }
                    SmartDropdown(
                        "Heritage", character.heritage,
                        heritageOptions,
                        Modifier.weight(1f)
                    ) { onChange(character.copy(heritage = it)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartDropdown(
                        "Background", character.background,
                        backgroundOptions,
                        Modifier.weight(1f)
                    ) { onChange(character.copy(background = it)) }
                    SmartDropdown(
                        "Class", character.className,
                        classOptions,
                        Modifier.weight(1f)
                    ) { onChange(character.copy(className = it)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartDropdown(
                        "Size", character.size,
                        listOf("Tiny", "Small", "Medium", "Large", "Huge"),
                        Modifier.weight(1f)
                    ) { onChange(character.copy(size = it)) }
                    SmartText("Alignment", character.alignment, Modifier.weight(1f)) {
                        onChange(character.copy(alignment = it))
                    }
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Traits", character.traits, Modifier.fillMaxWidth()) {
                    onChange(character.copy(traits = it))
                }
                Spacer(Modifier.height(8.dp))
                SmartDropdown(
                    "Deity",
                    character.deity,
                    listOf("None") + deityOptions,
                    Modifier.fillMaxWidth()
                ) { onChange(character.copy(deity = it)) }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Ability Scores")
                Spacer(Modifier.height(8.dp))
                val rows = Ability.entries.chunked(3)
                rows.forEachIndexed { rowIndex, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { ability ->
                            AbilityScoreField(
                                ability = ability,
                                score = character.abilities[ability] ?: 10,
                                modifier = Modifier.weight(1f)
                            ) { newScore ->
                                onChange(character.copy(abilities = character.abilities + (ability to newScore)))
                            }
                        }
                    }
                    if (rowIndex < rows.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Hit Points, Armor Class & Perception")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartText("Max HP", character.maxHp.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(maxHp = it.toIntOrNull() ?: character.maxHp))
                    }
                    SmartText("Current HP", character.currentHp.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(currentHp = it.toIntOrNull() ?: character.currentHp))
                    }
                    SmartText("Temp HP", character.tempHp.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(tempHp = it.toIntOrNull() ?: character.tempHp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcStat("AC", character.ac(), Modifier.weight(1f))
                    CalcStat("Perception", character.perception(), Modifier.weight(1f))
                    CalcStat("Class DC", character.classDc(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProficiencyPicker("Perception", character.perceptionProf, Modifier.weight(1f)) {
                        onChange(character.copy(perceptionProf = it))
                    }
                    SmartText("Dying", character.dying.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(dying = it.toIntOrNull() ?: character.dying))
                    }
                    SmartText("Wounded", character.wounded.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(wounded = it.toIntOrNull() ?: character.wounded))
                    }
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Saving Throws")
                Spacer(Modifier.height(8.dp))
                SaveRow("Fortitude", Ability.CON, character.fortitudeProf, character) {
                    onChange(character.copy(fortitudeProf = it))
                }
                Spacer(Modifier.height(8.dp))
                SaveRow("Reflex", Ability.DEX, character.reflexProf, character) {
                    onChange(character.copy(reflexProf = it))
                }
                Spacer(Modifier.height(8.dp))
                SaveRow("Will", Ability.WIS, character.willProf, character) {
                    onChange(character.copy(willProf = it))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Skills")
                Spacer(Modifier.height(8.dp))
                skillDefinitions.forEachIndexed { index, skill ->
                    SkillRow(
                        skill = skill,
                        proficiency = character.skillProfs[skill.name] ?: Proficiency.UNTRAINED,
                        bonus = character.skillBonus(skill)
                    ) { proficiency ->
                        onChange(
                            character.copy(
                                skillProfs = character.skillProfs + (skill.name to proficiency)
                            )
                        )
                    }
                    if (index < skillDefinitions.lastIndex) Spacer(Modifier.height(6.dp))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Armor Class & Armor Proficiencies")
                Spacer(Modifier.height(8.dp))
                SmartDropdown(
                    "Equipped Armor",
                    character.armorName,
                    armorCatalog.map { it.name },
                    Modifier.fillMaxWidth()
                ) { onChange(character.copy(armorName = it)) }
                val armor = character.armor()
                val armorInfo = remember(armor.name) {
                    rules.findByName("equipment", armor.name)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Armor +${armor.itemBonus}  •  Dex cap ${armor.dexCap}  •  Check ${armor.checkPenalty}  •  Speed ${armor.speedPenalty} ft  •  Bulk ${armor.bulk}",
                    color = Muted,
                    fontSize = 12.sp
                )
                if (armorInfo != null) RuleInfoSummary(armorInfo)
                Spacer(Modifier.height(10.dp))
                character.armorProf.keys.forEach { category ->
                    val value = character.armorProf[category] ?: Proficiency.UNTRAINED
                    ProficiencyPicker(category, value, Modifier.fillMaxWidth()) { newValue ->
                        onChange(character.copy(armorProf = character.armorProf + (category to newValue)))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Weapon Proficiencies")
                Spacer(Modifier.height(8.dp))
                character.weaponProf.keys.forEach { category ->
                    val value = character.weaponProf[category] ?: Proficiency.UNTRAINED
                    ProficiencyPicker(category, value, Modifier.fillMaxWidth()) { newValue ->
                        onChange(character.copy(weaponProf = character.weaponProf + (category to newValue)))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Melee Strikes")
                Spacer(Modifier.height(8.dp))
                RulePicker(
                    "Weapon",
                    character.meleeWeapon,
                    weaponRuleOptions,
                    Modifier.fillMaxWidth()
                ) { if (it != "—") onChange(character.copy(meleeWeapon = it)) }
                val weapon = weaponCatalog.firstOrNull { it.name == character.meleeWeapon } ?: weaponCatalog.first()
                val weaponInfo = remember(weapon.name) {
                    rules.findByName("equipment", weapon.name)
                }
                Spacer(Modifier.height(8.dp))
                StrikeSummary(weapon, character)
                if (weaponInfo != null) RuleInfoSummary(weaponInfo)
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Ranged Strikes")
                Spacer(Modifier.height(8.dp))
                RulePicker(
                    "Weapon",
                    character.rangedWeapon,
                    weaponRuleOptions,
                    Modifier.fillMaxWidth()
                ) { if (it != "—") onChange(character.copy(rangedWeapon = it)) }
                val weapon = weaponCatalog.firstOrNull { it.name == character.rangedWeapon } ?: weaponCatalog.first()
                val weaponInfo = remember(weapon.name) {
                    rules.findByName("equipment", weapon.name)
                }
                Spacer(Modifier.height(8.dp))
                StrikeSummary(weapon, character)
                if (weaponInfo != null) RuleInfoSummary(weaponInfo)
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Speed, Senses, Languages & Notes")
                Spacer(Modifier.height(8.dp))
                SmartText("Speed (feet)", character.speed.toString(), Modifier.fillMaxWidth(), true) {
                    onChange(character.copy(speed = it.toIntOrNull() ?: character.speed))
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Movement Types & Notes", character.movementNotes, Modifier.fillMaxWidth()) {
                    onChange(character.copy(movementNotes = it))
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Senses", character.senses, Modifier.fillMaxWidth()) {
                    onChange(character.copy(senses = it))
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Languages", character.languages, Modifier.fillMaxWidth()) {
                    onChange(character.copy(languages = it))
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Resistances & Immunities", character.resistances, Modifier.fillMaxWidth()) {
                    onChange(character.copy(resistances = it))
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Conditions", character.conditions, Modifier.fillMaxWidth()) {
                    onChange(character.copy(conditions = it))
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Notes", character.notes, Modifier.fillMaxWidth()) {
                    onChange(character.copy(notes = it))
                }
            }
        }
    }
}

@Composable
private fun AbilityScoreField(
    ability: Ability,
    score: Int,
    modifier: Modifier = Modifier,
    onScoreChange: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(ability.name, color = Gold, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = score.toString(),
            onValueChange = { onScoreChange((it.toIntOrNull() ?: score).coerceIn(1, 30)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = sheetFieldColors()
        )
        val bonus = (score - 10) / 2
        Text(if (bonus >= 0) "+" + bonus else bonus.toString(), color = Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalcStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Muted, fontSize = 11.sp)
        val shown = if (value >= 0 && label != "AC" && label != "Class DC") "+" + value else value.toString()
        Text(shown, color = Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SaveRow(
    label: String,
    ability: Ability,
    proficiency: Proficiency,
    character: CharacterState,
    onChange: (Proficiency) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Text, fontWeight = FontWeight.Bold)
            Text(ability.name + " " + character.abilityModifier(ability) + " + " + proficiency.code, color = Muted, fontSize = 11.sp)
        }

        val total = character.saveBonus(ability, proficiency)
        Text(
            if (total >= 0) "+" + total else total.toString(),
            color = Green,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        ProficiencyPicker(label, proficiency, Modifier.width(92.dp), onChange)
    }
}

@Composable
private fun SkillRow(
    skill: SkillDefinition,
    proficiency: Proficiency,
    bonus: Int,
    onChange: (Proficiency) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(skill.name, color = Text, fontWeight = FontWeight.SemiBold)
            Text(skill.ability.name, color = Muted, fontSize = 10.sp)
        }
        Text(
            if (bonus >= 0) "+" + bonus else bonus.toString(),
            color = Green,
            fontWeight = FontWeight.Bold,

            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        ProficiencyPicker(skill.name, proficiency, Modifier.width(92.dp), onChange)
    }
}

@Composable
private fun StrikeSummary(weapon: WeaponDefinition, character: CharacterState) {
    val attack = character.attackBonus(weapon)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalcStat("Attack", attack, Modifier.weight(1f))
        Column(
            Modifier
                .weight(2f)
                .clip(RoundedCornerShape(10.dp))
                .background(Card)
                .border(1.dp, GoldDark, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Text(weapon.damageDice + " " + weapon.damageType, color = Text, fontWeight = FontWeight.Bold)
            Text(weapon.category + " • " + weapon.traits, color = Muted, fontSize = 11.sp)
            Text("Bulk " + weapon.bulk, color = Gold, fontSize = 11.sp)
        }
    }
}

private fun List<String>.withIndexValue(index: Int, value: String): List<String> {
    val out = toMutableList()
    while (out.size <= index) out.add("")
    out[index] = value
    return out
}

private val featChoices = listOf(
    "—", "Natural Ambition", "General Training", "Hunted Shot",
    "Twin Takedown", "Assurance (Survival)", "Fleet", "Toughness", "Custom"
)

@Composable
private fun FeatsInventorySheet(
    character: CharacterState,
    onChange: (CharacterState) -> Unit,
    rules: RulesRepository
) {
    val ancestryFeats = remember(character.ancestry, character.level) {
        rules.featEntries("ancestry", character.level, ancestry = character.ancestry)
    }
    val classFeats = remember(character.className, character.level) {
        rules.featEntries("class", character.level, className = character.className)
    }
    val skillFeats = remember(character.level) {
        rules.featEntries("skill", character.level)
    }
    val generalFeats = remember(character.level) {
        rules.featEntries("general", character.level)
    }
    var showInventoryAi by remember { mutableStateOf(false) }
    var inventoryGoal by remember {
        mutableStateOf("Make my pack practical and easy to manage.")
    }
    var inventoryPlan by remember { mutableStateOf<InventoryPlan?>(null) }
    var inventoryBusy by remember { mutableStateOf(false) }
    var inventoryError by remember { mutableStateOf("") }
    val inventoryAssistant = remember { InventoryAssistant() }
    val inventoryScope = rememberCoroutineScope()

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FeatSection(
                title = "Ancestry Feats and Abilities",
                levels = listOf("Heritage", "1st", "5th", "9th", "13th", "17th"),
                options = ancestryFeats,
                values = character.ancestryFeats,
                rules = rules
            ) { index, value ->
                onChange(character.copy(ancestryFeats = character.ancestryFeats.withIndexValue(index, value)))
            }
        }

        item {
            FeatSection(
                title = "Class Feats and Abilities",
                levels = listOf("1st Feature", "1st Feat", "2nd", "4th", "6th", "8th", "10th", "12th", "14th", "16th", "18th", "20th"),
                options = classFeats,
                values = character.classFeats,
                rules = rules
            ) { index, value ->
                onChange(character.copy(classFeats = character.classFeats.withIndexValue(index, value)))
            }
        }
        item {
            FeatSection(
                title = "Skill Feats",
                levels = listOf("2nd", "4th", "6th", "8th", "10th", "12th", "14th", "16th", "18th", "20th"),
                options = skillFeats,
                values = character.skillFeats,
                rules = rules
            ) { index, value ->
                onChange(character.copy(skillFeats = character.skillFeats.withIndexValue(index, value)))
            }
        }

        item {
            FeatSection(
                title = "General Feats",
                levels = listOf("3rd", "7th", "11th", "15th", "19th"),
                options = generalFeats,
                values = character.generalFeats,
                rules = rules
            ) { index, value ->
                onChange(character.copy(generalFeats = character.generalFeats.withIndexValue(index, value)))
            }
        }

        item {
            FeatSection(
                title = "Bonus Feats",
                levels = listOf("1", "2", "3", "4"),
                options = (ancestryFeats + classFeats + skillFeats + generalFeats)
                    .distinctBy { it.name },
                values = character.bonusFeats,
                rules = rules
            ) { index, value ->
                onChange(character.copy(bonusFeats = character.bonusFeats.withIndexValue(index, value)))
            }
        }
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Inventory")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showInventoryAi = true }) {
                        Text("✨ AI Pack", color = Green)
                    }
                }
                Spacer(Modifier.height(8.dp))
                character.inventory.forEach { item ->
                    InventorySheetRow(item)
                    Spacer(Modifier.height(6.dp))
                }
                val totalBulk = character.inventory.sumOf { it.weight * it.quantity }
                val strMod = character.abilityModifier(Ability.STR)
                Spacer(Modifier.height(6.dp))
                Text("Bulk " + totalBulk + "  •  Encumbered " + (5 + strMod) + "  •  Maximum " + (10 + strMod), color = Gold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyField("CP", character.currencyCp, Modifier.weight(1f)) { onChange(character.copy(currencyCp = it)) }
                    CurrencyField("SP", character.currencySp, Modifier.weight(1f)) { onChange(character.copy(currencySp = it)) }
                    CurrencyField("GP", character.currencyGp, Modifier.weight(1f)) { onChange(character.copy(currencyGp = it)) }
                    CurrencyField("PP", character.currencyPp, Modifier.weight(1f)) { onChange(character.copy(currencyPp = it)) }
                }
            }
        }
    }

    if (showInventoryAi) {
        InventoryAiDialog(
            goal = inventoryGoal,
            onGoalChange = { inventoryGoal = it },
            plan = inventoryPlan,
            busy = inventoryBusy,
            error = inventoryError,
            onGenerate = {
                if (inventoryBusy) return@InventoryAiDialog
                inventoryBusy = true
                inventoryError = ""
                inventoryPlan = null
                inventoryScope.launch {
                    try {
                        inventoryPlan = inventoryAssistant.manage(
                            character,
                            inventoryGoal
                        )
                    } catch (t: Throwable) {
                        inventoryError = t.message
                            ?: "Inventory AI could not respond."
                    } finally {
                        inventoryBusy = false
                    }
                }
            },
            onApply = { plan ->
                onChange(character.copy(inventory = plan.items))
                showInventoryAi = false
                inventoryPlan = null
            },
            onDismiss = {
                showInventoryAi = false
                inventoryPlan = null
                inventoryError = ""
            }
        )
    }
}

@Composable
private fun InventoryAiDialog(
    goal: String,
    onGoalChange: (String) -> Unit,
    plan: InventoryPlan?,
    busy: Boolean,
    error: String,
    onGenerate: () -> Unit,
    onApply: (InventoryPlan) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("AI Pack Assistant", color = Text)
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Tell Wayfarer what you want. It will propose a new pack before changing anything.",
                    color = Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = goal,
                    onValueChange = onGoalChange,
                    label = { Text("What should the AI do?") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sheetFieldColors()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onGenerate,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (busy) "Thinking…" else "✨ Build Pack Plan",
                        color = Text
                    )
                }

                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Green,
                        trackColor = CardAlt
                    )
                }

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Danger)
                }

                if (plan != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        plan.summary,
                        color = Gold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    plan.items.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, color = Text)
                                Text(
                                    "x" + item.quantity +
                                        if (item.mechanics.isBlank()) {
                                            ""
                                        } else {
                                            " • " + item.mechanics
                                        },
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
            if (plan != null) {
                TextButton(onClick = { onApply(plan) }) {
                    Text("Apply Pack", color = Green)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Muted)
            }
        },
        containerColor = Surface
    )
}

@Composable
private fun FeatSection(
    title: String,
    levels: List<String>,
    options: List<RuleEntrySummary>,
    values: List<String>,
    rules: RulesRepository,
    onChange: (Int, String) -> Unit
) {
    FramedCard(Modifier.fillMaxWidth()) {
        SectionTitle(title)
        Spacer(Modifier.height(8.dp))
        levels.forEachIndexed { index, level ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(level, color = Gold, fontSize = 11.sp, modifier = Modifier.width(82.dp))
                val selectedValue = values.getOrElse(index) { "—" }.ifBlank { "—" }
                val selectedInfo = remember(selectedValue) {
                    if (selectedValue == "—") null else rules.findAnyByName(
                        selectedValue,
                        listOf("feat", "class_feature", "ancestry_feature")
                    )
                }
                RulePicker(
                    label = "Feat / Ability",
                    value = selectedValue,
                    options = options,
                    modifier = Modifier.weight(1f),
                    selectedInfo = selectedInfo
                ) { onChange(index, it) }
            }
            if (index < levels.lastIndex) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun InventorySheetRow(item: InventoryItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Card)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.icon, fontSize = 26.sp, modifier = Modifier.width(38.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = Text, fontWeight = FontWeight.Bold)
            Text(item.description, color = Muted, fontSize = 11.sp)
            if (item.mechanics.isNotBlank()) Text(item.mechanics, color = Green, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("x" + item.quantity, color = Text)
            Text("Bulk " + item.weight, color = Gold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CurrencyField(label: String, value: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange((it.toIntOrNull() ?: value).coerceAtLeast(0)) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = sheetFieldColors()
    )
}

@Composable
private fun CharacterActionsSheet(
    character: CharacterState,
    onChange: (CharacterState) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Character Sketch")
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Card)
                        .border(1.dp, GoldDark, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Portrait / sketch slot", color = Muted)
                }
            }
        }
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Identity & Appearance")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartText("Ethnicity", character.ethnicity, Modifier.weight(1f)) { onChange(character.copy(ethnicity = it)) }
                    SmartText("Nationality", character.nationality, Modifier.weight(1f)) { onChange(character.copy(nationality = it)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartText("Birthplace", character.birthplace, Modifier.weight(1f)) { onChange(character.copy(birthplace = it)) }
                    SmartText("Age", character.age, Modifier.weight(1f)) { onChange(character.copy(age = it)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartText("Gender & Pronouns", character.genderPronouns, Modifier.weight(2f)) { onChange(character.copy(genderPronouns = it)) }
                    SmartText("HT", character.height, Modifier.weight(1f)) { onChange(character.copy(height = it)) }
                    SmartText("WT", character.weight, Modifier.weight(1f)) { onChange(character.copy(weight = it)) }
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Appearance", character.appearance, Modifier.fillMaxWidth()) { onChange(character.copy(appearance = it)) }
            }
        }
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Personality")
                Spacer(Modifier.height(8.dp))
                SmartText("Attitude", character.attitude, Modifier.fillMaxWidth()) { onChange(character.copy(attitude = it)) }
                Spacer(Modifier.height(8.dp))
                SmartText("Beliefs", character.beliefs, Modifier.fillMaxWidth()) { onChange(character.copy(beliefs = it)) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartText("Likes", character.likes, Modifier.weight(1f)) { onChange(character.copy(likes = it)) }
                    SmartText("Dislikes", character.dislikes, Modifier.weight(1f)) { onChange(character.copy(dislikes = it)) }
                }
                Spacer(Modifier.height(8.dp))
                SmartText("Catchphrases", character.catchphrases, Modifier.fillMaxWidth()) { onChange(character.copy(catchphrases = it)) }
            }
        }
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Notes, Allies, Enemies & Organizations")
                Spacer(Modifier.height(8.dp))
                SmartText("Notes", character.notes, Modifier.fillMaxWidth()) { onChange(character.copy(notes = it)) }
                Spacer(Modifier.height(8.dp))
                SmartText("Allies", character.allies, Modifier.fillMaxWidth()) { onChange(character.copy(allies = it)) }
                Spacer(Modifier.height(8.dp))
                SmartText("Enemies", character.enemies, Modifier.fillMaxWidth()) { onChange(character.copy(enemies = it)) }
                Spacer(Modifier.height(8.dp))
                SmartText("Organizations", character.organizations, Modifier.fillMaxWidth()) { onChange(character.copy(organizations = it)) }
            }
        }
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Actions and Activities")
                Spacer(Modifier.height(8.dp))
                SmartDropdown(
                    "Default Action Cost", "1 Action",
                    listOf("1 Action", "2 Actions", "3 Actions", "Free Action", "Reaction"),
                    Modifier.fillMaxWidth()
                ) { }
                Spacer(Modifier.height(8.dp))
                SmartText("Name / Traits / Description", character.actionsAndActivities, Modifier.fillMaxWidth()) {
                    onChange(character.copy(actionsAndActivities = it))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Free Actions and Reactions")
                Spacer(Modifier.height(8.dp))
                SmartText("Trigger / Description", character.freeActionsAndReactions, Modifier.fillMaxWidth()) {
                    onChange(character.copy(freeActionsAndReactions = it))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Campaign Notes")
                Spacer(Modifier.height(8.dp))
                SmartText("Campaign Notes", character.campaignNotes, Modifier.fillMaxWidth()) {
                    onChange(character.copy(campaignNotes = it))
                }
            }
        }
    }
}

@Composable
private fun SpellsSheet(
    character: CharacterState,
    onChange: (CharacterState) -> Unit,
    rules: RulesRepository
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Spellcasting")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalcStat("Spell Attack", character.spellAttack(), Modifier.weight(1f))
                    CalcStat("Spell DC", character.spellDc(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartDropdown(
                        "Magic Tradition",
                        character.magicTradition,
                        listOf("Arcane", "Occult", "Primal", "Divine"),
                        Modifier.weight(1f)
                    ) { onChange(character.copy(magicTradition = it)) }
                    SmartDropdown(
                        "Casting",
                        character.castingType,
                        listOf("Prepared", "Spontaneous"),
                        Modifier.weight(1f)
                    ) { onChange(character.copy(castingType = it)) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartDropdown(
                        "Key Ability",
                        character.spellcastingAbility.name,
                        Ability.entries.map { it.name },
                        Modifier.weight(1f)
                    ) { name -> onChange(character.copy(spellcastingAbility = Ability.valueOf(name))) }
                    ProficiencyPicker("Spell Prof", character.spellProf, Modifier.weight(1f)) {
                        onChange(character.copy(spellProf = it))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmartText("Focus Current", character.focusCurrent.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(focusCurrent = it.toIntOrNull() ?: character.focusCurrent))
                    }
                    SmartText("Focus Maximum", character.focusMax.toString(), Modifier.weight(1f), true) {
                        onChange(character.copy(focusMax = it.toIntOrNull() ?: character.focusMax))
                    }
                }
            }
        }
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Spell Slots Per Day")
                Spacer(Modifier.height(8.dp))
                (1..10).chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { level ->
                            val count = character.spellSlots[level] ?: 0
                            SmartText(
                                "Lv " + level,
                                count.toString(),
                                Modifier.weight(1f),
                                true
                            ) { text ->
                                val value = (text.toIntOrNull() ?: count).coerceAtLeast(0)
                                onChange(character.copy(spellSlots = character.spellSlots + (level to value)))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        item { SpellListSection("Cantrips", 0, character, rules, onChange) }
        item { SpellListSection("Innate Spells", -1, character, rules, onChange) }
        item { SpellListSection("Focus Spells", -2, character, rules, onChange) }

        (1..10).forEach { level ->
            item {
                SpellListSection("Level " + level + " Spells", level, character, rules, onChange)
            }
        }
    }
}

@Composable
private fun SpellListSection(
    title: String,
    level: Int,
    character: CharacterState,
    rules: RulesRepository,
    onChange: (CharacterState) -> Unit
) {
    val current = character.spells[level].orEmpty()
    val tradition = character.magicTradition.lowercase()
    val options = remember(level, tradition) {
        when (level) {
            0 -> rules.search(
                kind = "spell",
                query = "",
                requiredTrait = "cantrip",
                requiredTradition = tradition,
                limit = 5000
            )
            -2 -> rules.search(
                kind = "spell",
                query = "",
                requiredTrait = "focus",
                limit = 5000
            )
            -1 -> rules.search(
                kind = "spell",
                query = "",
                limit = 5000
            )
            else -> rules.search(
                kind = "spell",
                query = "",
                exactLevel = level,
                requiredTradition = tradition,
                limit = 5000
            )
        }.distinctBy { it.name }
            .filterNot { candidate ->
                current.any { it.equals(candidate.name, true) }
            }
            .sortedBy { it.name }
    }

    FramedCard(Modifier.fillMaxWidth()) {
        SectionTitle(title, current.size.toString() + " selected")
        Spacer(Modifier.height(8.dp))
        RulePicker(
            label = "Add Spell",
            value = "—",
            options = options,
            modifier = Modifier.fillMaxWidth()
        ) { spell ->
            if (spell != "—") {
                onChange(
                    character.copy(
                        spells = character.spells +
                            (level to (current + spell).distinct())
                    )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (current.isEmpty()) {
            Text("No spells selected.", color = Muted, fontSize = 12.sp)
        } else {
            current.forEach { spell ->
                val info = remember(spell) { rules.findByName("spell", spell) }
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            spell,
                            color = Text,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Remove",
                            color = Danger,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                onChange(
                                    character.copy(
                                        spells = character.spells +
                                            (level to current.filterNot { it == spell })
                                    )
                                )
                            }
                        )
                    }
                    if (info != null) RuleInfoSummary(info)
                }
            }
        }
    }
}


@Composable
private fun LevelUpDialog(
    character: CharacterState,
    rules: RulesRepository,
    onDismiss: () -> Unit,
    onConfirm: (CharacterState) -> Unit
) {
    val nextLevel = (character.level + 1).coerceAtMost(20)
    val progression = remember(character.className) {
        rules.classProgression(character.className)
    }
    val needsClassFeat = progression?.classFeatLevels?.contains(nextLevel) == true
    val needsSkillFeat = progression?.skillFeatLevels?.contains(nextLevel) == true
    val needsGeneralFeat = progression?.generalFeatLevels?.contains(nextLevel) == true
    val needsAncestryFeat = progression?.ancestryFeatLevels?.contains(nextLevel) == true
    val needsSkillIncrease = progression?.skillIncreaseLevels?.contains(nextLevel) == true
    val autoFeatures = progression?.features?.filter { it.level == nextLevel }.orEmpty()

    var classChoice by remember(nextLevel) { mutableStateOf("") }
    var skillChoice by remember(nextLevel) { mutableStateOf("") }
    var generalChoice by remember(nextLevel) { mutableStateOf("") }
    var ancestryChoice by remember(nextLevel) { mutableStateOf("") }
    var skillIncrease by remember(nextLevel) { mutableStateOf("") }

    val classOptions = remember(character.className, nextLevel) {
        rules.featEntries(
            "class",
            nextLevel,
            className = character.className
        )
    }
    val skillOptions = remember(nextLevel) {
        rules.featEntries("skill", nextLevel)
    }
    val generalOptions = remember(nextLevel) {
        (
            rules.featEntries("general", nextLevel) +
                rules.featEntries("skill", nextLevel)
        ).distinctBy { it.name }.sortedBy { it.name }
    }
    val ancestryOptions = remember(character.ancestry, nextLevel) {
        rules.featEntries(
            "ancestry",
            nextLevel,
            ancestry = character.ancestry
        )
    }

    val ready =
        (!needsClassFeat || classChoice.isNotBlank()) &&
        (!needsSkillFeat || skillChoice.isNotBlank()) &&
        (!needsGeneralFeat || generalChoice.isNotBlank()) &&
        (!needsAncestryFeat || ancestryChoice.isNotBlank()) &&
        (!needsSkillIncrease || skillIncrease.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Level Up", color = Text)
                Text(
                    character.className + " " + character.level + " → " + nextLevel,
                    color = Gold,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CharacterAdviceBar(character, rules, levelUp = true)
                Text("Automatic", color = Green, fontWeight = FontWeight.Bold)
                val hpGain = (progression?.hpPerLevel ?: 0) +
                    character.abilityModifier(Ability.CON)
                Text(
                    "HP +" + hpGain + " • level-based proficiencies recalculate automatically",
                    color = Muted,
                    fontSize = 12.sp
                )
                autoFeatures.forEach { feature ->
                    Text("✓ " + feature.name, color = Text, fontSize = 13.sp)
                }

                if (needsClassFeat) {
                    RulePicker(
                        "Class Feat",
                        classChoice.ifBlank { "—" },
                        classOptions,
                        Modifier.fillMaxWidth()
                    ) { classChoice = if (it == "—") "" else it }
                }
                if (needsSkillFeat) {
                    RulePicker(
                        "Skill Feat",
                        skillChoice.ifBlank { "—" },
                        skillOptions,
                        Modifier.fillMaxWidth()
                    ) { skillChoice = if (it == "—") "" else it }
                }
                if (needsGeneralFeat) {
                    RulePicker(
                        "General Feat",
                        generalChoice.ifBlank { "—" },
                        generalOptions,
                        Modifier.fillMaxWidth()
                    ) { generalChoice = if (it == "—") "" else it }
                }
                if (needsAncestryFeat) {
                    RulePicker(
                        "Ancestry Feat",
                        ancestryChoice.ifBlank { "—" },
                        ancestryOptions,
                        Modifier.fillMaxWidth()
                    ) { ancestryChoice = if (it == "—") "" else it }
                }

                if (needsSkillIncrease) {
                    SmartDropdown(
                        "Skill Increase",
                        skillIncrease.ifBlank { "—" },
                        listOf("—") + skillDefinitions.map { it.name },
                        Modifier.fillMaxWidth()
                    ) { skillIncrease = if (it == "—") "" else it }
                }
                if (!needsClassFeat && !needsSkillFeat &&
                    !needsGeneralFeat && !needsAncestryFeat &&
                    !needsSkillIncrease && autoFeatures.isEmpty()
                ) {
                    Text(
                        "No selections are required at this level.",
                        color = Muted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = ready,
                onClick = {
                    val hpGain = ((progression?.hpPerLevel ?: 0) +
                        character.abilityModifier(Ability.CON)).coerceAtLeast(1)
                    var updated = character.copy(
                        level = nextLevel,
                        xp = (character.xp - 1000).coerceAtLeast(0),
                        maxHp = character.maxHp + hpGain,
                        currentHp = character.currentHp + hpGain
                    )

                    val classAdds = autoFeatures.map { it.name } +
                        listOf(classChoice).filter { it.isNotBlank() }
                    if (classAdds.isNotEmpty()) {
                        updated = updated.copy(
                            classFeats = updated.classFeats + classAdds
                        )
                    }
                    if (skillChoice.isNotBlank()) {
                        updated = updated.copy(
                            skillFeats = updated.skillFeats + skillChoice
                        )
                    }
                    if (generalChoice.isNotBlank()) {
                        updated = updated.copy(
                            generalFeats = updated.generalFeats + generalChoice
                        )
                    }
                    if (ancestryChoice.isNotBlank()) {
                        updated = updated.copy(
                            ancestryFeats = updated.ancestryFeats + ancestryChoice
                        )
                    }
                    if (skillIncrease.isNotBlank()) {
                        val oldRank = updated.skillProfs[skillIncrease]
                            ?: Proficiency.UNTRAINED
                        updated = updated.copy(
                            skillProfs = updated.skillProfs +
                                (skillIncrease to oldRank.promoted())
                        )
                    }
                    onConfirm(updated)
                },

                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
            ) {
                Text("Complete Level Up")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Muted)
            }
        },
        containerColor = Surface
    )
}
