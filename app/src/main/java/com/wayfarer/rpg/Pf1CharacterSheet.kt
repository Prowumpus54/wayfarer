package com.wayfarer.rpg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val pf1CoreRaces = listOf(
    "Human", "Dwarf", "Elf", "Gnome", "Half-Elf", "Half-Orc", "Halfling"
)

@Composable
fun Pf1CoreSheet(
    character: CharacterState,
    onChange: (CharacterState) -> Unit
) {
    val profile = pf1ClassProfile(character.className)
    val classSkills = character.pf1ClassSkills()
    val intMod = character.abilityModifier(Ability.INT)
    val classRankBase = character.effectivePf1ClassLevels().entries.sumOf {
        (pf1ClassProfile(it.key)?.skillRanksPerLevel ?: 0) * it.value
    }
    val rankBudget = (
        classRankBase +
            intMod * character.level +
            if (character.ancestry.equals("Human", true)) character.level else 0
        ).coerceAtLeast(character.level)
    val ranksUsed = character.pf1SkillRanks.values.sum()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Pathfinder 1e")
                Text(
                    "PF1 schema v" + character.pf1SchemaVersion +
                        " • " + character.pf1ExperienceTrack + " XP track",
                    color = Green,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(8.dp))
                Pf1TextField(
                    "Character Name",
                    character.characterName
                ) { onChange(character.copy(characterName = it)) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pf1Dropdown(
                        "Race",
                        character.ancestry,
                        pf1CoreRaces,
                        Modifier.weight(1f)
                    ) { onChange(character.copy(ancestry = it, heritage = "")) }
                    Pf1Dropdown(
                        "Class",
                        character.className,
                        pf1ClassProfiles.values.map { it.name }.sorted(),
                        Modifier.weight(1f)
                    ) { selected ->
                        val selectedProfile = pf1ClassProfile(selected)
                        onChange(
                            character.copy(
                                className = selected,
                                pf1ClassLevels = mapOf(selected to character.level),
                                spellcastingAbility =
                                    selectedProfile?.castingAbility
                                        ?: character.spellcastingAbility
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Pf1TextField(
                    "Background / Concept",
                    character.background
                ) { onChange(character.copy(background = it)) }
                Spacer(Modifier.height(8.dp))
                Pf1TextField(
                    "Alignment",
                    character.alignment
                ) { onChange(character.copy(alignment = it)) }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Combat")
                Spacer(Modifier.height(8.dp))
                Pf1StatGrid(
                    listOf(
                        "BAB" to character.baseAttackBonus(),
                        "Init" to character.initiative(),
                        "AC" to character.ac(),
                        "Touch" to character.touchAc(),
                        "Flat" to character.flatFootedAc(),
                        "CMB" to character.cmb(),
                        "CMD" to character.cmd()
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pf1Stat("Fort", character.fortitudeSave(), Modifier.weight(1f))
                    Pf1Stat("Ref", character.reflexSave(), Modifier.weight(1f))
                    Pf1Stat("Will", character.willSave(), Modifier.weight(1f))
                }
                profile?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it.name + " • d" + it.hitDie + " HD • " +
                            when (it.bab) {
                                Pf1BabProgression.FULL -> "full BAB"
                                Pf1BabProgression.THREE_QUARTER -> "3/4 BAB"
                                Pf1BabProgression.HALF -> "1/2 BAB"
                            } +
                            " • " + it.skillRanksPerLevel + "+Int skill ranks/level",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Ability Scores")
                Spacer(Modifier.height(8.dp))
                Ability.entries.chunked(3).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { ability ->
                            Pf1NumberField(
                                ability.name,
                                character.abilities[ability] ?: 10,
                                Modifier.weight(1f),
                                1,
                                40
                            ) { score ->
                                onChange(
                                    character.copy(
                                        abilities = character.abilities +
                                            (ability to score)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Hit Points & Defense")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pf1NumberField(
                        "Current HP",
                        character.currentHp,
                        Modifier.weight(1f),
                        0,
                        9999
                    ) { onChange(character.copy(currentHp = it)) }
                    Pf1NumberField(
                        "Max HP",
                        character.maxHp,
                        Modifier.weight(1f),
                        1,
                        9999
                    ) {
                        onChange(
                            character.copy(
                                maxHp = it,
                                currentHp = character.currentHp.coerceAtMost(it)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Pf1Dropdown(
                    "Armor",
                    character.armorName,
                    armorCatalog.map { it.name },
                    Modifier.fillMaxWidth()
                ) { onChange(character.copy(armorName = it)) }
                val armor = character.armor()
                Text(
                    "Armor +" + armor.itemBonus +
                        " • Max Dex " + armor.dexCap +
                        " • ACP " + armor.checkPenalty +
                        " • Speed " + armor.speedPenalty,
                    color = Muted,
                    fontSize = 11.sp
                )
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Weapons")
                Spacer(Modifier.height(8.dp))
                Pf1Dropdown(
                    "Melee Weapon",
                    character.meleeWeapon,
                    weaponCatalog.map { it.name },
                    Modifier.fillMaxWidth()
                ) { onChange(character.copy(meleeWeapon = it)) }
                val melee = weaponCatalog.firstOrNull {
                    it.name == character.meleeWeapon
                } ?: weaponCatalog.first()
                Text(
                    "Attack " + signedPf1(character.attackBonus(melee)) +
                        " • " + melee.damageDice +
                        signedDamage(character.damageBonus(melee)) +
                        " • crit " + melee.criticalThreatMin + "-20/x" +
                        melee.criticalMultiplier,
                    color = Green,
                    fontSize = 12.sp
                )
                val iteratives = character.iterativeAttackBonuses(melee)
                if (iteratives.size > 1) {
                    Text(
                        "Full attack: " +
                            iteratives.joinToString("/") { signedPf1(it) },
                        color = Gold,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Pf1Dropdown(
                    "Ranged Weapon",
                    character.rangedWeapon,
                    weaponCatalog.filter { it.isRanged }.map { it.name },
                    Modifier.fillMaxWidth()
                ) { onChange(character.copy(rangedWeapon = it)) }
                val ranged = weaponCatalog.firstOrNull {
                    it.name == character.rangedWeapon
                }
                if (ranged != null) {
                    Text(
                        "Attack " + signedPf1(character.attackBonus(ranged)) +
                            " • " + ranged.damageDice +
                            signedDamage(character.damageBonus(ranged)) +
                            " • crit " + ranged.criticalThreatMin + "-20/x" +
                            ranged.criticalMultiplier +
                            " • range " + ranged.rangeIncrementFt + " ft",
                        color = Green,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle(
                    "Skills",
                    ranksUsed.toString() + "/" + rankBudget + " ranks"
                )
                Spacer(Modifier.height(8.dp))
                skillDefinitions.forEach { skill ->
                    val ranks = character.pf1SkillRanks[skill.name] ?: 0
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                skill.name +
                                    if (skill.name in classSkills) " ★" else "",
                                color = Text,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                skill.ability.name +
                                    if (skill.trainedOnly) " • trained only" else "",
                                color = Muted,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            signedPf1(character.skillBonus(skill)),
                            color = Green,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Pf1NumberField(
                            "Ranks",
                            ranks,
                            Modifier.width(86.dp),
                            0,
                            character.level
                        ) { value ->
                            val current = character.pf1SkillRanks.toMutableMap()
                            if (value <= 0) current.remove(skill.name)
                            else current[skill.name] = value
                            onChange(character.copy(pf1SkillRanks = current))
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Movement & Notes")
                Pf1NumberField(
                    "Speed (ft)",
                    character.speed,
                    Modifier.fillMaxWidth(),
                    0,
                    500
                ) { onChange(character.copy(speed = it)) }
                Spacer(Modifier.height(8.dp))
                Pf1TextField("Senses", character.senses) {
                    onChange(character.copy(senses = it))
                }
                Spacer(Modifier.height(8.dp))
                Pf1TextField("Languages", character.languages) {
                    onChange(character.copy(languages = it))
                }
                Spacer(Modifier.height(8.dp))
                Pf1TextField("Conditions", character.conditions) {
                    onChange(character.copy(conditions = it))
                }
            }
        }
    }
}

@Composable
fun Pf1FeatsInventorySheet(
    character: CharacterState,
    onChange: (CharacterState) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("PF1 Feats")
                Text(
                    "Only stored feats are shown here. LoreWise will not treat a feat as " +
                        "rules-valid until its prerequisites are represented by the PF1 validator.",
                    color = Muted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(8.dp))
                Pf1ListEditor(
                    "General / Character Feats",
                    character.generalFeats
                ) { onChange(character.copy(generalFeats = it)) }
                Spacer(Modifier.height(8.dp))
                Pf1ListEditor(
                    "Class / Combat Style Feats",
                    character.classFeats
                ) { onChange(character.copy(classFeats = it)) }
                Spacer(Modifier.height(8.dp))
                Pf1ListEditor(
                    "Bonus Feats",
                    character.bonusFeats
                ) { onChange(character.copy(bonusFeats = it)) }
            }
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("Inventory")
                if (character.inventory.isEmpty()) {
                    Text("No carried items.", color = Muted)
                } else {
                    character.inventory.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, color = Text)
                                Text(
                                    item.category +
                                        if (item.mechanics.isBlank()) "" else
                                            " • " + item.mechanics,
                                    color = Muted,
                                    fontSize = 10.sp
                                )
                            }
                            Text("×" + item.quantity, color = Gold)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Text(
                    "Detailed item add/remove controls remain available in Inventory.",
                    color = Muted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun Pf1SpellsSheet(
    character: CharacterState,
    onChange: (CharacterState) -> Unit
) {
    val maxLevel = maxOf(
        character.spellSlots.keys.maxOrNull() ?: 0,
        character.spells.keys.maxOrNull() ?: 0
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle("PF1 Spellcasting")
                Text(
                    "Caster level " + character.casterLevel() +
                        " • " + character.spellcastingAbility.name +
                        " • " + character.castingType,
                    color = Green
                )
                if (character.casterLevel() == 0) {
                    Text(
                        character.className +
                            " does not cast spells at this class level.",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        items((0..maxLevel.coerceAtLeast(0)).toList()) { spellLevel ->
            FramedCard(Modifier.fillMaxWidth()) {
                SectionTitle(
                    if (spellLevel == 0) "0-Level" else "Level $spellLevel",
                    "DC " + character.spellDc(spellLevel)
                )
                val capacity = character.spellSlots[spellLevel] ?: 0
                val used = character.spellSlotsUsed[spellLevel] ?: 0
                if (spellLevel > 0) {
                    Text(
                        "Slots " + (capacity - used).coerceAtLeast(0) +
                            "/" + capacity,
                        color = Gold,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        "0-level spells are not expended when cast.",
                        color = Muted,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Pf1ListEditor(
                    if (character.castingType.equals("Prepared", true)) {
                        "Prepared / Available Spells"
                    } else {
                        "Spells Known"
                    },
                    character.spells[spellLevel].orEmpty()
                ) { names ->
                    onChange(
                        character.copy(
                            spells = character.spells +
                                (spellLevel to names)
                        )
                    )
                }
            }
        }

        if (maxLevel == 0 && character.casterLevel() == 0) {
            item {
                Text(
                    "Spell slots will appear when this class reaches its PF1 casting level.",
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
fun Pf1LevelUpDialog(
    character: CharacterState,
    onDismiss: () -> Unit,
    onConfirm: (CharacterState) -> Unit
) {
    val nextLevel = (character.level + 1).coerceAtMost(20)
    val profile = pf1ClassProfile(character.className)
    val suggestedHp = (
        ((profile?.hitDie ?: 8) / 2 + 1) +
            character.abilityModifier(Ability.CON)
        ).coerceAtLeast(1)
    var hpGain by remember(nextLevel) {
        mutableStateOf(suggestedHp.toString())
    }
    var feat by remember(nextLevel) { mutableStateOf("") }
    var abilityChoice by remember(nextLevel) {
        mutableStateOf(Ability.STR.name)
    }
    val featLevel = nextLevel % 2 == 1
    val abilityLevel = nextLevel % 4 == 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                character.className + " " + character.level +
                    " → " + nextLevel,
                color = Text
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "PF1 level-up",
                    color = Green,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "BAB and base saves recalculate from class level automatically.",
                    color = Muted,
                    fontSize = 11.sp
                )
                Pf1NumberField(
                    "HP gained",
                    hpGain.toIntOrNull() ?: suggestedHp,
                    Modifier.fillMaxWidth(),
                    1,
                    profile?.hitDie?.plus(
                        character.abilityModifier(Ability.CON)
                    )?.coerceAtLeast(1) ?: 20
                ) { hpGain = it.toString() }
                Text(
                    "Default is the average-hit-die suggestion; edit it if your " +
                        "campaign rolls or uses another HP rule.",
                    color = Muted,
                    fontSize = 10.sp
                )
                if (featLevel) {
                    Pf1TextField("New feat", feat) { feat = it }
                }
                if (abilityLevel) {
                    Pf1Dropdown(
                        "Ability +1",
                        abilityChoice,
                        Ability.entries.map { it.name },
                        Modifier.fillMaxWidth()
                    ) { abilityChoice = it }
                }
                if (
                    character.className.equals("Ranger", true) &&
                    nextLevel == 4
                ) {
                    Text(
                        "Ranger 4 unlocks PF1 spellcasting and Hunter's Bond.",
                        color = Gold,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val gain = (hpGain.toIntOrNull() ?: suggestedHp)
                        .coerceAtLeast(1)
                    val levels = character.effectivePf1ClassLevels()
                        .toMutableMap()
                    levels[character.className] =
                        (levels[character.className] ?: character.level) + 1

                    var abilities = character.abilities
                    if (abilityLevel) {
                        val ability = Ability.valueOf(abilityChoice)
                        abilities = abilities + (
                            ability to ((abilities[ability] ?: 10) + 1)
                            )
                    }

                    var slots = character.spellSlots
                    if (character.className.equals("Ranger", true)) {
                        slots = pf1RangerBaseSpellSlots(nextLevel)
                            .mapValues { (spellLevel, base) ->
                                base + pf1BonusSpells(
                                    abilities[Ability.WIS] ?: 10,
                                    spellLevel
                                )
                            }
                    }

                    onConfirm(
                        character.copy(
                            level = nextLevel,
                            pf1ClassLevels = levels,
                            maxHp = character.maxHp + gain,
                            currentHp = character.currentHp + gain,
                            abilities = abilities,
                            generalFeats = if (featLevel && feat.isNotBlank()) {
                                character.generalFeats + feat.trim()
                            } else {
                                character.generalFeats
                            },
                            spellSlots = slots,
                            spellSlotsUsed = character.spellSlotsUsed
                                .filterKeys { it in slots.keys }
                                .mapValues { (level, used) ->
                                    used.coerceAtMost(slots[level] ?: 0)
                                }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenDark
                )
            ) {
                Text("Apply PF1 Level", color = Text)
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

@Composable
private fun Pf1StatGrid(values: List<Pair<String, Int>>) {
    values.chunked(4).forEach { row ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row.forEach { (label, value) ->
                Pf1Stat(label, value, Modifier.weight(1f))
            }
            repeat(4 - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun Pf1Stat(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(Card, RoundedCornerShape(9.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Muted, fontSize = 9.sp)
        Text(
            if (
                value >= 0 &&
                label !in setOf("AC", "Touch", "Flat", "CMD")
            ) signedPf1(value) else value.toString(),
            color = Text,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun Pf1TextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        colors = pf1FieldColors()
    )
}

@Composable
private fun Pf1NumberField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = {
            it.toIntOrNull()?.let { parsed ->
                onChange(parsed.coerceIn(min, max))
            }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        modifier = modifier,
        colors = pf1FieldColors()
    )
}

@Composable
private fun Pf1Dropdown(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Text("▾", color = Green) },
            modifier = Modifier.fillMaxWidth(),
            colors = pf1FieldColors()
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
        ) {
            TextButton(
                onClick = { open = true },
                modifier = Modifier.fillMaxSize()
            ) {}
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onChange(option)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Pf1ListEditor(
    label: String,
    values: List<String>,
    onChange: (List<String>) -> Unit
) {
    Pf1TextField(
        label,
        values.joinToString("\n")
    ) { raw ->
        onChange(
            raw.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        )
    }
}

@Composable
private fun pf1FieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Text,
    unfocusedTextColor = Text,
    focusedBorderColor = Green,
    unfocusedBorderColor = GoldDark,
    cursorColor = Green
)

private fun signedPf1(value: Int): String =
    if (value >= 0) "+$value" else value.toString()

private fun signedDamage(value: Int): String =
    when {
        value > 0 -> "+$value"
        value < 0 -> value.toString()
        else -> ""
    }
