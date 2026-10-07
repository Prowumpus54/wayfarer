package com.wayfarer.rpg

import java.util.UUID

enum class EncounterStatus {
    ACTIVE,
    VICTORY,
    ESCAPED,
    COMPLETE
}

enum class CreatureStatus {
    ACTIVE,
    DEFEATED,
    FLED,
    SURRENDERED
}

enum class ChallengeStatus {
    ACTIVE,
    COMPLETE,
    FAILED
}

enum class CombatActorType {
    PLAYER,
    CREATURE
}

data class CombatTurnEntry(
    val actorType: CombatActorType,
    val actorId: String,
    val name: String,
    val initiative: Int,
    val initiativeBonus: Int
)

data class EncounterCreatureState(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ruleRef: String = "",
    val maxHp: Int,
    val currentHp: Int = maxHp,
    val armorClass: Int,
    val initiative: Int? = null,
    val initiativeBonus: Int = 0,
    val attackName: String = "",
    val attackBonus: Int? = null,
    val damageDice: String = "",
    val damageType: String = "",
    val fortitude: Int = 0,
    val reflex: Int = 0,
    val will: Int = 0,
    val xpValue: Int = 0,
    val conditions: List<String> = emptyList(),
    val status: CreatureStatus = CreatureStatus.ACTIVE,
    val statsResolved: Boolean = false
)

data class EncounterState(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val sourceEncounterId: String = "",
    val round: Int = 1,
    val initiativeOrder: List<CombatTurnEntry> = emptyList(),
    val currentTurnIndex: Int = 0,
    val creatures: List<EncounterCreatureState> = emptyList(),
    val loot: List<InventoryItem> = emptyList(),
    val lootCp: Int = 0,
    val lootSp: Int = 0,
    val lootGp: Int = 0,
    val lootPp: Int = 0,
    val status: EncounterStatus = EncounterStatus.ACTIVE,
    val xpAwarded: Boolean = false
)

data class ChallengeState(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val dc: Int = 0,
    val progress: Int = 0,
    val goal: Int = 1,
    val xpValue: Int = 0,
    val status: ChallengeStatus = ChallengeStatus.ACTIVE
)

data class CampaignRuntimeState(
    val activeEncounter: EncounterState? = null,
    val activeChallenge: ChallengeState? = null,
    val worldFlags: Set<String> = emptySet()
)

enum class GmEffectType(val wireName: String) {
    START_ENCOUNTER("start_encounter"),
    SPAWN_CREATURE("spawn_creature"),
    START_INITIATIVE("start_initiative"),
    ADVANCE_TURN("advance_turn"),
    CREATURE_STRIKE("creature_strike"),
    DAMAGE_CHARACTER("damage_character"),
    HEAL_CHARACTER("heal_character"),
    DAMAGE_CREATURE("damage_creature"),
    HEAL_CREATURE("heal_creature"),
    ADD_LOOT("add_loot"),
    TAKE_LOOT("take_loot"),
    ADD_ITEM("add_item"),
    ADD_CURRENCY("add_currency"),
    SPEND_SPELL_SLOT("spend_spell_slot"),
    APPLY_CONDITION("apply_condition"),
    REMOVE_CONDITION("remove_condition"),
    SET_FLAG("set_flag"),
    START_CHALLENGE("start_challenge"),
    ADVANCE_CHALLENGE("advance_challenge"),
    COMPLETE_CHALLENGE("complete_challenge"),
    COMPLETE_ENCOUNTER("complete_encounter");

    companion object {
        fun fromWire(value: String): GmEffectType? =
            entries.firstOrNull {
                it.wireName.equals(value.trim(), true) ||
                    it.name.equals(value.trim(), true)
            }
    }
}

data class GmEffect(
    val type: String,
    val target: String = "",
    val name: String = "",
    val ruleRef: String = "",
    val quantity: Int = 1,
    val amount: Int = 0,
    val dice: String = "",
    val maxHp: Int = 0,
    val armorClass: Int = 0,
    val xpValue: Int = 0,
    val condition: String = "",
    val currency: String = "",
    val level: Int = 0,
    val description: String = "",
    val mechanics: String = "",
    val category: String = "Other",
    val weight: Double = 0.0,
    val icon: String = "🎒",
    val flag: String = "",
    val dc: Int = 0,
    val goal: Int = 1
)

data class GameStateApplication(
    val character: CharacterState,
    val runtime: CampaignRuntimeState,
    val events: List<GameEvent>
)

object GameStateEngine {
    fun apply(
        character: CharacterState,
        runtime: CampaignRuntimeState,
        effects: List<GmEffect>,
        location: String,
        ruleset: String = "pf2e-adapted",
        creatureResolver: (String) -> CreatureCombatProfile? = { null }
    ): GameStateApplication {
        var nextCharacter = character
        var nextRuntime = runtime
        val events = mutableListOf<GameEvent>()

        effects.take(24).forEach { effect ->
            when (GmEffectType.fromWire(effect.type)) {
                GmEffectType.START_ENCOUNTER -> {
                    val title = effect.name.ifBlank { "Encounter" }
                    nextRuntime = nextRuntime.copy(
                        activeEncounter = EncounterState(
                            name = title,
                            location = location,
                            sourceEncounterId = effect.ruleRef
                        )
                    )
                    events += GameEvent(
                        "Encounter started",
                        title + " began at " + location + ".",
                        "encounter"
                    )
                }

                GmEffectType.SPAWN_CREATURE -> {
                    val quantity = effect.quantity.coerceIn(1, 12)
                    val requestedName = effect.name.ifBlank { effect.ruleRef.ifBlank { "Creature" } }
                    val profile = creatureResolver(
                        effect.ruleRef.ifBlank { requestedName }
                    ) ?: creatureResolver(requestedName)
                    val maxHp = profile?.maxHp
                        ?: effect.maxHp.takeIf { it > 0 }?.coerceIn(1, 9999)
                        ?: 1
                    val ac = profile?.armorClass
                        ?: effect.armorClass.takeIf { it > 0 }?.coerceIn(1, 99)
                        ?: 10
                    val xp = if (profile != null && ruleset.lowercase().contains("pf2")) {
                        pf2CreatureXp(profile.level, nextCharacter.level)
                    } else {
                        effect.xpValue.coerceIn(0, 1_000_000)
                    }
                    var encounter = nextRuntime.activeEncounter
                    if (encounter == null || encounter.status != EncounterStatus.ACTIVE) {
                        encounter = EncounterState(
                            name = effect.description.ifBlank { "Improvised encounter" },
                            location = location
                        )
                    }
                    val canonicalName = profile?.name ?: requestedName
                    val existingCount = encounter.creatures.count {
                        it.name.substringBefore(" #").equals(canonicalName, true)
                    }
                    val primaryAttack = profile?.primaryAttack
                    val spawned = List(quantity) { index ->
                        val suffix = if (quantity > 1 || existingCount > 0) {
                            " #" + (existingCount + index + 1)
                        } else {
                            ""
                        }
                        EncounterCreatureState(
                            name = canonicalName + suffix,
                            ruleRef = profile?.ruleRef ?: effect.ruleRef,
                            maxHp = maxHp,
                            currentHp = maxHp,
                            armorClass = ac,
                            initiativeBonus = profile?.initiativeBonus ?: 0,
                            attackName = primaryAttack?.name.orEmpty(),
                            attackBonus = primaryAttack?.attackBonus,
                            damageDice = primaryAttack?.damageDice.orEmpty(),
                            damageType = primaryAttack?.damageType.orEmpty(),
                            fortitude = profile?.fortitude ?: 0,
                            reflex = profile?.reflex ?: 0,
                            will = profile?.will ?: 0,
                            xpValue = xp,
                            statsResolved = profile != null
                        )
                    }
                    encounter = encounter.copy(creatures = encounter.creatures + spawned)
                    nextRuntime = nextRuntime.copy(activeEncounter = encounter)
                    events += GameEvent(
                        "Creatures entered",
                        quantity.toString() + " × " + canonicalName +
                            if (profile != null) {
                                " loaded from the rules database."
                            } else {
                                " added with unresolved placeholder stats."
                            },
                        "encounter"
                    )
                }

                GmEffectType.START_INITIATIVE -> {
                    val encounter = nextRuntime.activeEncounter
                    if (encounter != null && encounter.status == EncounterStatus.ACTIVE) {
                        val started = CombatRulesEngine.startInitiative(
                            nextCharacter,
                            encounter,
                            ruleset
                        )
                        if (started != encounter) {
                            nextRuntime = nextRuntime.copy(activeEncounter = started)
                            events += GameEvent(
                                "Initiative",
                                started.initiativeOrder.joinToString(" • ") {
                                    it.name + " " + it.initiative
                                },
                                "encounter"
                            )
                        }
                    }
                }

                GmEffectType.ADVANCE_TURN -> {
                    val encounter = nextRuntime.activeEncounter
                    if (encounter != null && encounter.initiativeOrder.isNotEmpty()) {
                        nextRuntime = nextRuntime.copy(
                            activeEncounter = CombatRulesEngine.advanceTurn(encounter)
                        )
                    }
                }

                GmEffectType.CREATURE_STRIKE -> {
                    val encounter = nextRuntime.activeEncounter
                    if (encounter != null) {
                        val source = effect.name.ifBlank { effect.target }
                        val resolved = CombatRulesEngine.resolveCreatureStrike(
                            nextCharacter,
                            encounter,
                            source,
                            ruleset
                        )
                        nextCharacter = resolved.character
                        nextRuntime = nextRuntime.copy(
                            activeEncounter = resolved.encounter
                        )
                        events += resolved.events
                        if (resolved.events.isEmpty()) {
                            events += GameEvent(
                                "Enemy action blocked",
                                resolved.summary,
                                "encounter"
                            )
                        }
                    }
                }

                GmEffectType.DAMAGE_CHARACTER -> {
                    val damage = resolvedAmount(effect)
                    if (damage > 0) {
                        val absorbed = minOf(nextCharacter.tempHp, damage)
                        val hpDamage = (damage - absorbed).coerceAtLeast(0)
                        nextCharacter = nextCharacter.copy(
                            tempHp = (nextCharacter.tempHp - absorbed).coerceAtLeast(0),
                            currentHp = (nextCharacter.currentHp - hpDamage).coerceAtLeast(0)
                        )
                        events += GameEvent(
                            "Damage taken",
                            nextCharacter.characterName + " took " + damage +
                                " damage and is at " + nextCharacter.currentHp +
                                "/" + nextCharacter.maxHp + " HP.",
                            "state"
                        )
                    }
                }

                GmEffectType.HEAL_CHARACTER -> {
                    val healing = resolvedAmount(effect)
                    if (healing > 0) {
                        nextCharacter = nextCharacter.copy(
                            currentHp = (nextCharacter.currentHp + healing)
                                .coerceAtMost(nextCharacter.maxHp)
                        )
                        events += GameEvent(
                            "HP restored",
                            nextCharacter.characterName + " recovered " + healing +
                                " HP and is at " + nextCharacter.currentHp +
                                "/" + nextCharacter.maxHp + " HP.",
                            "state"
                        )
                    }
                }

                GmEffectType.DAMAGE_CREATURE,
                GmEffectType.HEAL_CREATURE -> {
                    val encounter = nextRuntime.activeEncounter
                    if (encounter != null) {
                        val targetIndex = findCreatureIndex(encounter, effect.target.ifBlank { effect.name })
                        if (targetIndex >= 0) {
                            val amount = resolvedAmount(effect)
                            if (amount > 0) {
                                val old = encounter.creatures[targetIndex]
                                val isDamage = GmEffectType.fromWire(effect.type) == GmEffectType.DAMAGE_CREATURE
                                val hp = if (isDamage) {
                                    (old.currentHp - amount).coerceAtLeast(0)
                                } else {
                                    (old.currentHp + amount).coerceAtMost(old.maxHp)
                                }
                                val updated = old.copy(
                                    currentHp = hp,
                                    status = if (hp == 0) CreatureStatus.DEFEATED else old.status
                                )
                                val creatures = encounter.creatures.toMutableList().apply {
                                    this[targetIndex] = updated
                                }
                                var updatedEncounter = encounter.copy(creatures = creatures)
                                val beforeStatus = updatedEncounter.status
                                updatedEncounter = maybeCompleteEncounter(updatedEncounter)
                                var app = GameStateApplication(
                                    nextCharacter,
                                    nextRuntime.copy(activeEncounter = updatedEncounter),
                                    emptyList()
                                )
                                if (beforeStatus == EncounterStatus.ACTIVE &&
                                    updatedEncounter.status == EncounterStatus.VICTORY
                                ) {
                                    app = awardEncounterXp(app)
                                }
                                nextCharacter = app.character
                                nextRuntime = app.runtime
                                events += app.events
                                events += GameEvent(
                                    if (isDamage) "Creature damaged" else "Creature healed",
                                    updated.name + " is at " + updated.currentHp +
                                        "/" + updated.maxHp + " HP.",
                                    "encounter"
                                )
                            }
                        }
                    }
                }

                GmEffectType.ADD_LOOT -> {
                    val encounter = nextRuntime.activeEncounter
                    if (encounter != null) {
                        val item = itemFromEffect(effect)
                        nextRuntime = nextRuntime.copy(
                            activeEncounter = encounter.copy(
                                loot = mergeInventory(encounter.loot, item)
                            )
                        )
                        events += GameEvent(
                            "Loot discovered",
                            item.name + " ×" + item.quantity + " is available to loot.",
                            "loot"
                        )
                    }
                }

                GmEffectType.TAKE_LOOT -> {
                    val encounter = nextRuntime.activeEncounter
                    if (encounter != null) {
                        val requested = effect.quantity.coerceAtLeast(1)
                        val name = effect.name.ifBlank { effect.target }
                        val index = encounter.loot.indexOfFirst { it.name.equals(name, true) }
                        if (index >= 0) {
                            val source = encounter.loot[index]
                            val takenQty = minOf(source.quantity, requested)
                            val taken = source.copy(quantity = takenQty)
                            val remaining = encounter.loot.toMutableList()
                            if (takenQty >= source.quantity) {
                                remaining.removeAt(index)
                            } else {
                                remaining[index] = source.copy(quantity = source.quantity - takenQty)
                            }
                            nextCharacter = nextCharacter.copy(
                                inventory = mergeInventory(nextCharacter.inventory, taken)
                            )
                            nextRuntime = nextRuntime.copy(
                                activeEncounter = encounter.copy(loot = remaining)
                            )
                            events += GameEvent(
                                "Loot collected",
                                nextCharacter.characterName + " took " +
                                    taken.name + " ×" + taken.quantity + ".",
                                "loot"
                            )
                        }
                    }
                }

                GmEffectType.ADD_ITEM -> {
                    val item = itemFromEffect(effect)
                    nextCharacter = nextCharacter.copy(
                        inventory = mergeInventory(nextCharacter.inventory, item)
                    )
                    events += GameEvent(
                        "Item gained",
                        nextCharacter.characterName + " received " +
                            item.name + " ×" + item.quantity + ".",
                        "loot"
                    )
                }

                GmEffectType.ADD_CURRENCY -> {
                    val amount = effect.amount.coerceIn(-1_000_000, 1_000_000)
                    if (amount != 0) {
                        if (effect.target.equals("loot", true) && nextRuntime.activeEncounter != null) {
                            val encounter = nextRuntime.activeEncounter
                            nextRuntime = nextRuntime.copy(
                                activeEncounter = when (effect.currency.lowercase()) {
                                    "cp" -> encounter.copy(lootCp = (encounter.lootCp + amount).coerceAtLeast(0))
                                    "sp" -> encounter.copy(lootSp = (encounter.lootSp + amount).coerceAtLeast(0))
                                    "gp" -> encounter.copy(lootGp = (encounter.lootGp + amount).coerceAtLeast(0))
                                    "pp" -> encounter.copy(lootPp = (encounter.lootPp + amount).coerceAtLeast(0))
                                    else -> encounter
                                }
                            )
                        } else {
                            nextCharacter = when (effect.currency.lowercase()) {
                                "cp" -> nextCharacter.copy(currencyCp = (nextCharacter.currencyCp + amount).coerceAtLeast(0))
                                "sp" -> nextCharacter.copy(currencySp = (nextCharacter.currencySp + amount).coerceAtLeast(0))
                                "gp" -> nextCharacter.copy(currencyGp = (nextCharacter.currencyGp + amount).coerceAtLeast(0))
                                "pp" -> nextCharacter.copy(currencyPp = (nextCharacter.currencyPp + amount).coerceAtLeast(0))
                                else -> nextCharacter
                            }
                        }
                    }
                }

                GmEffectType.SPEND_SPELL_SLOT -> {
                    val spellName = effect.name.trim()
                    val updated = if (spellName.isNotBlank()) {
                        consumeSpell(nextCharacter, spellName)
                    } else if (effect.level == -2) {
                        if (nextCharacter.focusCurrent > 0) {
                            nextCharacter.copy(focusCurrent = nextCharacter.focusCurrent - 1)
                        } else null
                    } else if (effect.level > 0) {
                        val maximum = nextCharacter.spellSlots[effect.level] ?: 0
                        val used = nextCharacter.spellSlotsUsed[effect.level] ?: 0
                        if (used < maximum) {
                            nextCharacter.copy(
                                spellSlotsUsed = nextCharacter.spellSlotsUsed +
                                    (effect.level to used + 1)
                            )
                        } else null
                    } else nextCharacter
                    if (updated != null) {
                        nextCharacter = updated
                        if (spellName.isNotBlank()) {
                            events += GameEvent(
                                "Spell cast",
                                nextCharacter.characterName + " cast " + spellName + ".",
                                "resource"
                            )
                        }
                    } else {
                        events += GameEvent(
                            "Spell resource unavailable",
                            if (spellName.isBlank()) {
                                "No spell resource remained for the requested cast."
                            } else {
                                spellName + " could not be cast because no matching resource remained."
                            },
                            "resource"
                        )
                    }
                }

                GmEffectType.APPLY_CONDITION,
                GmEffectType.REMOVE_CONDITION -> {
                    val condition = effect.condition.ifBlank { effect.name }.trim()
                    if (condition.isNotBlank()) {
                        val isRemove = GmEffectType.fromWire(effect.type) == GmEffectType.REMOVE_CONDITION
                        val encounter = nextRuntime.activeEncounter
                        val creatureIndex = if (encounter == null) -1 else
                            findCreatureIndex(encounter, effect.target)
                        if (creatureIndex >= 0 && encounter != null) {
                            val old = encounter.creatures[creatureIndex]
                            val updatedConditions = if (isRemove) {
                                old.conditions.filterNot { it.equals(condition, true) }
                            } else {
                                (old.conditions + condition).distinctBy { it.lowercase() }
                            }
                            val creatures = encounter.creatures.toMutableList().apply {
                                this[creatureIndex] = old.copy(conditions = updatedConditions)
                            }
                            nextRuntime = nextRuntime.copy(
                                activeEncounter = encounter.copy(creatures = creatures)
                            )
                        } else {
                            val current = nextCharacter.conditions
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            val updated = if (isRemove) {
                                current.filterNot { it.equals(condition, true) }
                            } else {
                                (current + condition).distinctBy { it.lowercase() }
                            }
                            nextCharacter = nextCharacter.copy(
                                conditions = updated.joinToString(", ")
                            )
                        }
                    }
                }

                GmEffectType.SET_FLAG -> {
                    val flag = effect.flag.ifBlank { effect.name }.trim()
                    if (flag.isNotBlank()) {
                        nextRuntime = nextRuntime.copy(
                            worldFlags = nextRuntime.worldFlags + flag
                        )
                        events += GameEvent(
                            "World state changed",
                            flag,
                            "world"
                        )
                    }
                }

                GmEffectType.START_CHALLENGE -> {
                    val challenge = ChallengeState(
                        name = effect.name.ifBlank { "Challenge" },
                        description = effect.description,
                        dc = effect.dc.coerceIn(0, 99),
                        goal = effect.goal.coerceIn(1, 100),
                        xpValue = effect.xpValue.coerceIn(0, 1_000_000)
                    )
                    nextRuntime = nextRuntime.copy(activeChallenge = challenge)
                    events += GameEvent(
                        "Challenge started",
                        challenge.name,
                        "challenge"
                    )
                }

                GmEffectType.ADVANCE_CHALLENGE -> {
                    val challenge = nextRuntime.activeChallenge
                    if (challenge != null && challenge.status == ChallengeStatus.ACTIVE) {
                        val step = effect.amount.takeIf { it != 0 } ?: 1
                        val progress = (challenge.progress + step).coerceIn(0, challenge.goal)
                        var updated = challenge.copy(progress = progress)
                        if (progress >= challenge.goal) {
                            updated = updated.copy(status = ChallengeStatus.COMPLETE)
                            nextCharacter = nextCharacter.copy(
                                xp = nextCharacter.xp + updated.xpValue
                            )
                            events += GameEvent(
                                "Challenge completed",
                                updated.name + " completed. " +
                                    if (updated.xpValue > 0) updated.xpValue.toString() + " XP gained." else "",
                                "challenge"
                            )
                        }
                        nextRuntime = nextRuntime.copy(activeChallenge = updated)
                    }
                }

                GmEffectType.COMPLETE_CHALLENGE -> {
                    val challenge = nextRuntime.activeChallenge
                    if (challenge != null && challenge.status == ChallengeStatus.ACTIVE) {
                        val updated = challenge.copy(
                            progress = challenge.goal,
                            status = ChallengeStatus.COMPLETE
                        )
                        nextCharacter = nextCharacter.copy(
                            xp = nextCharacter.xp + updated.xpValue
                        )
                        nextRuntime = nextRuntime.copy(activeChallenge = updated)
                        events += GameEvent(
                            "Challenge completed",
                            updated.name + " completed. " +
                                if (updated.xpValue > 0) updated.xpValue.toString() + " XP gained." else "",
                            "challenge"
                        )
                    }
                }

                GmEffectType.COMPLETE_ENCOUNTER -> {
                    val encounter = nextRuntime.activeEncounter
                    if (encounter != null && encounter.status == EncounterStatus.ACTIVE) {
                        val completed = encounter.copy(status = EncounterStatus.VICTORY)
                        val app = awardEncounterXp(
                            GameStateApplication(
                                nextCharacter,
                                nextRuntime.copy(activeEncounter = completed),
                                emptyList()
                            )
                        )
                        nextCharacter = app.character
                        nextRuntime = app.runtime
                        events += app.events
                    }
                }

                null -> Unit
            }
        }

        return GameStateApplication(nextCharacter, nextRuntime, events)
    }

    fun consumeSpell(
        character: CharacterState,
        spellName: String
    ): CharacterState? {
        val match = character.spells.entries.firstOrNull { (_, names) ->
            names.any { it.equals(spellName.trim(), true) }
        } ?: return character

        val rank = match.key
        return when {
            rank == 0 || rank == -1 -> character
            rank == -2 -> {
                if (character.focusCurrent <= 0) null
                else character.copy(focusCurrent = character.focusCurrent - 1)
            }
            rank > 0 -> {
                val maximum = character.spellSlots[rank] ?: 0
                val used = character.spellSlotsUsed[rank] ?: 0
                if (maximum <= used) null
                else character.copy(
                    spellSlotsUsed = character.spellSlotsUsed + (rank to used + 1)
                )
            }
            else -> character
        }
    }

    fun contextLines(runtime: CampaignRuntimeState): List<String> {
        val lines = mutableListOf<String>()
        runtime.activeEncounter?.let { encounter ->
            lines += "Encounter: " + encounter.name + " [" + encounter.status.name + "] round " + encounter.round
            if (encounter.initiativeOrder.isNotEmpty()) {
                val current = encounter.initiativeOrder.getOrNull(encounter.currentTurnIndex)
                lines += "Initiative: " + encounter.initiativeOrder.joinToString(" > ") {
                    it.name + " " + it.initiative
                }
                current?.let { lines += "Current turn: " + it.name }
            }
            encounter.creatures.forEach { creature ->
                lines += creature.name + ": HP " + creature.currentHp + "/" + creature.maxHp +
                    ", AC " + creature.armorClass + ", status " + creature.status.name +
                    if (creature.conditions.isEmpty()) "" else
                        ", conditions " + creature.conditions.joinToString(", ") +
                    if (creature.statsResolved) {
                        if (creature.attackBonus != null) {
                            ", attack " + creature.attackName + " +" + creature.attackBonus +
                                ", damage " + creature.damageDice
                        } else ""
                    } else " [stats unresolved]"
            }
            if (encounter.loot.isNotEmpty()) {
                lines += "Encounter loot: " + encounter.loot.joinToString("; ") {
                    it.name + " x" + it.quantity
                }
            }
        }
        runtime.activeChallenge?.let { challenge ->
            lines += "Challenge: " + challenge.name + " [" + challenge.status.name + "] " +
                challenge.progress + "/" + challenge.goal +
                if (challenge.dc > 0) ", DC " + challenge.dc else ""
        }
        if (runtime.worldFlags.isNotEmpty()) {
            lines += "World flags: " + runtime.worldFlags.sorted().joinToString("; ")
        }
        return lines
    }

    private fun pf2CreatureXp(creatureLevel: Int, partyLevel: Int): Int =
        when (creatureLevel - partyLevel) {
            -4 -> 10
            -3 -> 15
            -2 -> 20
            -1 -> 30
            0 -> 40
            1 -> 60
            2 -> 80
            3 -> 120
            4 -> 160
            else -> 0
        }

    private fun awardEncounterXp(application: GameStateApplication): GameStateApplication {
        val encounter = application.runtime.activeEncounter ?: return application
        if (encounter.xpAwarded) return application
        val xp = encounter.creatures.sumOf { it.xpValue }.coerceAtLeast(0)
        val updatedEncounter = encounter.copy(xpAwarded = true)
        if (xp <= 0) {
            return application.copy(
                runtime = application.runtime.copy(activeEncounter = updatedEncounter)
            )
        }
        return application.copy(
            character = application.character.copy(xp = application.character.xp + xp),
            runtime = application.runtime.copy(activeEncounter = updatedEncounter),
            events = application.events + GameEvent(
                "XP gained",
                application.character.characterName + " earned " + xp +
                    " XP from " + encounter.name + ".",
                "xp"
            )
        )
    }

    private fun maybeCompleteEncounter(encounter: EncounterState): EncounterState {
        val hasCreatures = encounter.creatures.isNotEmpty()
        val allDone = hasCreatures && encounter.creatures.none {
            it.status == CreatureStatus.ACTIVE && it.currentHp > 0
        }
        return if (encounter.status == EncounterStatus.ACTIVE && allDone) {
            encounter.copy(status = EncounterStatus.VICTORY)
        } else {
            encounter
        }
    }

    private fun findCreatureIndex(encounter: EncounterState, target: String): Int {
        if (target.isBlank()) {
            return encounter.creatures.indexOfFirst {
                it.status == CreatureStatus.ACTIVE
            }
        }
        return encounter.creatures.indexOfFirst {
            it.id.equals(target, true) || it.name.equals(target, true)
        }.takeIf { it >= 0 } ?: encounter.creatures.indexOfFirst {
            it.name.contains(target, true)
        }
    }

    private fun resolvedAmount(effect: GmEffect): Int {
        val rolled = DiceEngine.rollNotation(effect.dice)
        return (rolled?.total ?: effect.amount).coerceIn(0, 100_000)
    }

    private fun itemFromEffect(effect: GmEffect): InventoryItem =
        InventoryItem(
            name = effect.name.ifBlank { "Item" },
            category = effect.category.ifBlank { "Other" },
            quantity = effect.quantity.coerceIn(1, 999),
            weight = effect.weight.coerceAtLeast(0.0),
            icon = effect.icon.ifBlank { "🎒" },
            description = effect.description,
            mechanics = effect.mechanics
        )

    private fun mergeInventory(
        inventory: List<InventoryItem>,
        incoming: InventoryItem
    ): List<InventoryItem> {
        val index = inventory.indexOfFirst {
            it.name.equals(incoming.name, true) &&
                it.category.equals(incoming.category, true) &&
                it.mechanics.equals(incoming.mechanics, true)
        }
        if (index < 0) return inventory + incoming
        val out = inventory.toMutableList()
        val existing = out[index]
        out[index] = existing.copy(
            quantity = (existing.quantity + incoming.quantity).coerceAtMost(9999)
        )
        return out
    }
}
