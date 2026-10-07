package com.wayfarer.rpg

enum class CombatResolutionStatus {
    RESOLVED,
    NEED_TARGET,
    NEED_VALIDATED_STATS,
    NO_ACTIVE_ENCOUNTER,
    UNSUPPORTED_RULESET
}

data class CombatMechanicalResolution(
    val status: CombatResolutionStatus,
    val summary: String,
    val effects: List<GmEffect> = emptyList(),
    val rollText: String = ""
)

data class CreatureTurnResolution(
    val character: CharacterState,
    val encounter: EncounterState,
    val events: List<GameEvent>,
    val summary: String
)

object CombatRulesEngine {
    fun resolvePlayerStrike(
        character: CharacterState,
        runtime: CampaignRuntimeState,
        selectedTargetId: String?,
        actionText: String,
        ruleset: String
    ): CombatMechanicalResolution {
        if (!isPf2Adapted(ruleset)) {
            return CombatMechanicalResolution(
                CombatResolutionStatus.UNSUPPORTED_RULESET,
                "The current character schema cannot resolve $ruleset combat authoritatively."
            )
        }

        val encounter = runtime.activeEncounter
            ?: return CombatMechanicalResolution(
                CombatResolutionStatus.NO_ACTIVE_ENCOUNTER,
                "There is no active encounter to attack."
            )

        val target = resolveTarget(
            encounter = encounter,
            selectedTargetId = selectedTargetId,
            actionText = actionText
        ) ?: return CombatMechanicalResolution(
            CombatResolutionStatus.NEED_TARGET,
            "Choose a target before attacking. Active targets: " +
                encounter.creatures
                    .filter { it.status == CreatureStatus.ACTIVE && it.currentHp > 0 }
                    .joinToString(", ") { it.name }
        )

        if (!target.statsResolved) {
            return CombatMechanicalResolution(
                CombatResolutionStatus.NEED_VALIDATED_STATS,
                target.name + " does not have a validated rules stat block yet."
            )
        }

        val weapon = resolveWeapon(character, actionText)
        val attackBonus = character.attackBonus(weapon)
        val attack = DiceEngine.d20(attackBonus, target.armorClass)

        val damageModifier = if (
            weapon.name.equals(character.meleeWeapon, true)
        ) {
            character.abilityModifier(Ability.STR)
        } else {
            0
        }
        val damageExpression = appendModifier(weapon.damageDice, damageModifier)
        val damageRoll = if (
            attack.degree == Degree.SUCCESS ||
            attack.degree == Degree.CRITICAL_SUCCESS
        ) {
            DiceEngine.rollNotation(damageExpression)
        } else null
        val multiplier = if (attack.degree == Degree.CRITICAL_SUCCESS) 2 else 1
        val damage = (damageRoll?.total ?: 0) * multiplier

        val outcome = when (attack.degree) {
            Degree.CRITICAL_SUCCESS -> "critical hit"
            Degree.SUCCESS -> "hit"
            Degree.FAILURE -> "miss"
            Degree.CRITICAL_FAILURE -> "critical miss"
        }

        val effects = buildList {
            add(
                GmEffect(
                    type = GmEffectType.START_INITIATIVE.wireName
                )
            )
            if (damage > 0) {
                add(
                    GmEffect(
                        type = GmEffectType.DAMAGE_CREATURE.wireName,
                        target = target.id,
                        amount = damage
                    )
                )
            }
            add(
                GmEffect(
                    type = GmEffectType.ADVANCE_TURN.wireName
                )
            )
        }

        val attackText = character.characterName + " rolled " +
            attack.die + " + " + attack.modifier + " = " + attack.total +
            " vs " + target.name + " AC " + target.armorClass +
            " (" + outcome + ")"
        val damageText = damageRoll?.let {
            "; damage " + it.expression + " = " + it.total +
                if (multiplier == 2) " ×2 = " + damage else ""
        }.orEmpty()

        return CombatMechanicalResolution(
            status = CombatResolutionStatus.RESOLVED,
            summary = attackText + damageText + ".",
            effects = effects,
            rollText = attackText + damageText
        )
    }

    fun startInitiative(
        character: CharacterState,
        encounter: EncounterState,
        ruleset: String
    ): EncounterState {
        if (encounter.initiativeOrder.isNotEmpty()) return encounter
        if (!isPf2Adapted(ruleset)) return encounter

        val order = mutableListOf<CombatTurnEntry>()
        val playerRoll = DiceEngine.roll(20, modifier = character.perception())
        order += CombatTurnEntry(
            actorType = CombatActorType.PLAYER,
            actorId = "player",
            name = character.characterName,
            initiative = playerRoll.total,
            initiativeBonus = character.perception()
        )

        encounter.creatures
            .filter { it.status == CreatureStatus.ACTIVE && it.currentHp > 0 }
            .forEach { creature ->
                val roll = DiceEngine.roll(20, modifier = creature.initiativeBonus)
                order += CombatTurnEntry(
                    actorType = CombatActorType.CREATURE,
                    actorId = creature.id,
                    name = creature.name,
                    initiative = roll.total,
                    initiativeBonus = creature.initiativeBonus
                )
            }

        val sorted = order.sortedWith(
            compareByDescending<CombatTurnEntry> { it.initiative }
                .thenByDescending { it.initiativeBonus }
                .thenBy { it.name }
        )
        return encounter.copy(
            initiativeOrder = sorted,
            currentTurnIndex = 0,
            round = 1
        )
    }

    fun advanceTurn(encounter: EncounterState): EncounterState {
        if (encounter.initiativeOrder.isEmpty()) return encounter
        val activeIds = encounter.creatures
            .filter { it.status == CreatureStatus.ACTIVE && it.currentHp > 0 }
            .map { it.id }
            .toSet() + "player"
        val filtered = encounter.initiativeOrder.filter { it.actorId in activeIds }
        if (filtered.isEmpty()) return encounter.copy(initiativeOrder = emptyList())

        val oldActor = encounter.initiativeOrder
            .getOrNull(encounter.currentTurnIndex)
            ?.actorId
        val oldPosition = filtered.indexOfFirst { it.actorId == oldActor }
        val nextIndex = if (oldPosition < 0) 0 else (oldPosition + 1) % filtered.size
        val wrapped = oldPosition >= 0 && nextIndex == 0

        return encounter.copy(
            initiativeOrder = filtered,
            currentTurnIndex = nextIndex,
            round = encounter.round + if (wrapped) 1 else 0
        )
    }

    fun resolveCreatureStrike(
        character: CharacterState,
        encounter: EncounterState,
        creatureNameOrId: String,
        ruleset: String
    ): CreatureTurnResolution {
        if (!isPf2Adapted(ruleset)) {
            return CreatureTurnResolution(
                character,
                encounter,
                emptyList(),
                "Creature strike not resolved: unsupported ruleset $ruleset."
            )
        }

        val index = encounter.creatures.indexOfFirst {
            it.id.equals(creatureNameOrId, true) ||
                it.name.equals(creatureNameOrId, true)
        }
        if (index < 0) {
            return CreatureTurnResolution(
                character,
                encounter,
                emptyList(),
                "Creature strike not resolved: creature not found."
            )
        }
        val creature = encounter.creatures[index]
        if (!creature.statsResolved || creature.attackBonus == null ||
            creature.damageDice.isBlank()
        ) {
            return CreatureTurnResolution(
                character,
                encounter,
                emptyList(),
                creature.name + " has no validated offensive stat block."
            )
        }

        val attack = DiceEngine.d20(creature.attackBonus, character.ac())
        val damageRoll = if (
            attack.degree == Degree.SUCCESS ||
            attack.degree == Degree.CRITICAL_SUCCESS
        ) DiceEngine.rollNotation(creature.damageDice) else null
        val multiplier = if (attack.degree == Degree.CRITICAL_SUCCESS) 2 else 1
        val damage = (damageRoll?.total ?: 0) * multiplier

        var updatedCharacter = character
        val events = mutableListOf<GameEvent>()
        if (damage > 0) {
            val absorbed = minOf(updatedCharacter.tempHp, damage)
            val hpDamage = (damage - absorbed).coerceAtLeast(0)
            updatedCharacter = updatedCharacter.copy(
                tempHp = (updatedCharacter.tempHp - absorbed).coerceAtLeast(0),
                currentHp = (updatedCharacter.currentHp - hpDamage).coerceAtLeast(0)
            )
            events += GameEvent(
                "Enemy attack",
                creature.name + " hit " + character.characterName + " for " +
                    damage + " " + creature.damageType + " damage. " +
                    updatedCharacter.currentHp + "/" + updatedCharacter.maxHp +
                    " HP remains.",
                "encounter"
            )
        } else {
            events += GameEvent(
                "Enemy attack",
                creature.name + " missed " + character.characterName + ".",
                "encounter"
            )
        }

        val outcome = when (attack.degree) {
            Degree.CRITICAL_SUCCESS -> "critical hit"
            Degree.SUCCESS -> "hit"
            Degree.FAILURE -> "miss"
            Degree.CRITICAL_FAILURE -> "critical miss"
        }
        val summary = creature.name + " used " +
            creature.attackName.ifBlank { "an attack" } + ": " +
            attack.die + " + " + attack.modifier + " = " + attack.total +
            " vs AC " + character.ac() + " (" + outcome + ")" +
            damageRoll?.let {
                "; damage " + it.expression + " = " + it.total +
                    if (multiplier == 2) " ×2 = " + damage else ""
            }.orEmpty() + "."

        return CreatureTurnResolution(
            character = updatedCharacter,
            encounter = advanceTurn(encounter),
            events = events,
            summary = summary
        )
    }

    private fun resolveTarget(
        encounter: EncounterState,
        selectedTargetId: String?,
        actionText: String
    ): EncounterCreatureState? {
        val active = encounter.creatures.filter {
            it.status == CreatureStatus.ACTIVE && it.currentHp > 0
        }
        if (selectedTargetId != null) {
            active.firstOrNull { it.id == selectedTargetId }?.let { return it }
        }
        if (active.size == 1) return active.single()

        val normalizedAction = normalize(actionText)
        val direct = active.filter { target ->
            val full = normalize(target.name)
            full.isNotBlank() && normalizedAction.contains(full)
        }
        if (direct.size == 1) return direct.single()

        val numbered = active.firstOrNull { target ->
            val number = Regex("""#(\d+)$""").find(target.name)?.groupValues?.get(1)
            val base = normalize(target.name.substringBefore("#"))
            number != null &&
                normalizedAction.contains(base) &&
                Regex("""\b$number\b""").containsMatchIn(normalizedAction)
        }
        return numbered
    }

    private fun resolveWeapon(
        character: CharacterState,
        actionText: String
    ): WeaponDefinition {
        val text = actionText.lowercase()
        val melee = weaponCatalog.firstOrNull {
            it.name.equals(character.meleeWeapon, true)
        } ?: weaponCatalog.first()
        val ranged = weaponCatalog.firstOrNull {
            it.name.equals(character.rangedWeapon, true)
        }

        if (ranged != null && (
                text.contains(ranged.name.lowercase()) ||
                    text.contains("shoot") ||
                    text.contains("bow")
                )
        ) {
            return ranged
        }
        return melee
    }

    private fun appendModifier(dice: String, modifier: Int): String =
        dice + when {
            modifier > 0 -> "+" + modifier
            modifier < 0 -> modifier.toString()
            else -> ""
        }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace("#", " ")
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()

    private fun isPf2Adapted(ruleset: String): Boolean =
        ruleset.lowercase().contains("pf2")
}
