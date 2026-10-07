package com.wayfarer.rpg

enum class CombatResolutionStatus {
    RESOLVED,
    NEED_TARGET,
    NEED_VALIDATED_STATS,
    NEED_INITIATIVE,
    NOT_PLAYER_TURN,
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
        ruleset: String,
        d20Roller: (Int, Int) -> CheckResult = DiceEngine::d20,
        damageRoller: (String) -> DiceRollResult? = DiceEngine::rollNotation
    ): CombatMechanicalResolution {
        val preflight = playerPreflight(runtime, selectedTargetId, actionText)
        if (preflight.resolution != null) return preflight.resolution
        val encounter = preflight.encounter!!
        val target = preflight.target!!

        return when {
            isPf1(ruleset) -> resolvePf1PlayerStrike(
                character = character,
                target = target,
                actionText = actionText,
                d20Roller = { modifier, dc ->
                    if (d20Roller === DiceEngine::d20) {
                        DiceEngine.pf1Check(
                            modifier,
                            dc,
                            automaticOnNatural = true
                        )
                    } else {
                        d20Roller(modifier, dc)
                    }
                },
                damageRoller = damageRoller
            )
            isPf2Adapted(ruleset) -> resolvePf2PlayerStrike(
                character,
                target,
                actionText,
                d20Roller,
                damageRoller
            )
            else -> CombatMechanicalResolution(
                CombatResolutionStatus.UNSUPPORTED_RULESET,
                "Unsupported combat ruleset: $ruleset."
            )
        }
    }

    fun resolvePlayerManeuver(
        character: CharacterState,
        runtime: CampaignRuntimeState,
        selectedTargetId: String?,
        actionText: String,
        ruleset: String,
        d20Roller: (Int, Int) -> CheckResult = { modifier, dc ->
            DiceEngine.pf1Check(modifier, dc, automaticOnNatural = true)
        }
    ): CombatMechanicalResolution {
        if (!isPf1(ruleset)) {
            return CombatMechanicalResolution(
                CombatResolutionStatus.UNSUPPORTED_RULESET,
                "Combat maneuvers are currently authoritative only for PF1 campaigns."
            )
        }
        val preflight = playerPreflight(runtime, selectedTargetId, actionText)
        if (preflight.resolution != null) return preflight.resolution
        val target = preflight.target!!

        val maneuver = when {
            actionText.contains("grapple", true) -> "Grapple"
            actionText.contains("trip", true) -> "Trip"
            actionText.contains("bull rush", true) ||
                actionText.contains("shove", true) -> "Bull Rush"
            actionText.contains("disarm", true) -> "Disarm"
            else -> "Combat Maneuver"
        }

        val result = d20Roller(character.cmb(), target.cmd)
        val success = pf1AttackHits(result)
        val effects = buildList {
            if (success && maneuver == "Grapple") {
                add(
                    GmEffect(
                        type = GmEffectType.APPLY_CONDITION.wireName,
                        target = target.id,
                        condition = "grappled"
                    )
                )
            }
            if (success && maneuver == "Trip") {
                add(
                    GmEffect(
                        type = GmEffectType.APPLY_CONDITION.wireName,
                        target = target.id,
                        condition = "prone"
                    )
                )
            }
            add(GmEffect(type = GmEffectType.ADVANCE_TURN.wireName))
        }

        val summary = character.characterName + " attempted " + maneuver +
            " against " + target.name + ": d20 " + result.die +
            " + CMB " + result.modifier + " = " + result.total +
            " vs CMD " + target.cmd + " (" +
            if (success) "success" else "failure" + ")."

        return CombatMechanicalResolution(
            CombatResolutionStatus.RESOLVED,
            summary,
            effects,
            summary
        )
    }

    fun startInitiative(
        character: CharacterState,
        encounter: EncounterState,
        ruleset: String,
        initiativeRoller: (Int) -> DiceRollResult = {
            DiceEngine.roll(20, modifier = it)
        }
    ): EncounterState {
        if (encounter.initiativeOrder.isNotEmpty()) return encounter
        if (!isPf1(ruleset) && !isPf2Adapted(ruleset)) return encounter

        val playerBonus = if (isPf1(ruleset)) {
            character.initiative()
        } else {
            character.perception()
        }
        val order = mutableListOf<CombatTurnEntry>()
        val playerRoll = initiativeRoller(playerBonus)
        order += CombatTurnEntry(
            actorType = CombatActorType.PLAYER,
            actorId = "player",
            name = character.characterName,
            initiative = playerRoll.total,
            initiativeBonus = playerBonus
        )

        encounter.creatures
            .filter { it.status == CreatureStatus.ACTIVE && it.currentHp > 0 }
            .forEach { creature ->
                val roll = initiativeRoller(creature.initiativeBonus)
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
        ruleset: String,
        d20Roller: (Int, Int) -> CheckResult = DiceEngine::d20,
        damageRoller: (String) -> DiceRollResult? = DiceEngine::rollNotation
    ): CreatureTurnResolution {
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
        val currentTurn = encounter.initiativeOrder
            .getOrNull(encounter.currentTurnIndex)
        if (
            encounter.initiativeOrder.isNotEmpty() &&
            (currentTurn?.actorType != CombatActorType.CREATURE ||
                currentTurn.actorId != creature.id)
        ) {
            return CreatureTurnResolution(
                character,
                encounter,
                emptyList(),
                "Creature strike blocked: it is " +
                    (currentTurn?.name ?: "another combatant") + "'s turn."
            )
        }
        if (
            !creature.statsResolved ||
            creature.attackBonus == null ||
            creature.damageDice.isBlank()
        ) {
            return CreatureTurnResolution(
                character,
                encounter,
                emptyList(),
                creature.name + " has no validated offensive stat block."
            )
        }

        return when {
            isPf1(ruleset) -> resolvePf1CreatureStrike(
                character,
                encounter,
                creature,
                d20Roller,
                damageRoller
            )
            isPf2Adapted(ruleset) -> resolvePf2CreatureStrike(
                character,
                encounter,
                creature,
                d20Roller,
                damageRoller
            )
            else -> CreatureTurnResolution(
                character,
                encounter,
                emptyList(),
                "Creature strike not resolved: unsupported ruleset $ruleset."
            )
        }
    }

    private data class PlayerPreflight(
        val encounter: EncounterState? = null,
        val target: EncounterCreatureState? = null,
        val resolution: CombatMechanicalResolution? = null
    )

    private fun playerPreflight(
        runtime: CampaignRuntimeState,
        selectedTargetId: String?,
        actionText: String
    ): PlayerPreflight {
        val encounter = runtime.activeEncounter
            ?: return PlayerPreflight(
                resolution = CombatMechanicalResolution(
                    CombatResolutionStatus.NO_ACTIVE_ENCOUNTER,
                    "There is no active encounter."
                )
            )

        if (encounter.initiativeOrder.isEmpty()) {
            return PlayerPreflight(
                resolution = CombatMechanicalResolution(
                    status = CombatResolutionStatus.NEED_INITIATIVE,
                    summary = "Initiative must be rolled before the action resolves.",
                    effects = listOf(
                        GmEffect(type = GmEffectType.START_INITIATIVE.wireName)
                    )
                )
            )
        }

        val currentTurn = encounter.initiativeOrder
            .getOrNull(encounter.currentTurnIndex)
        if (currentTurn?.actorType != CombatActorType.PLAYER) {
            return PlayerPreflight(
                resolution = CombatMechanicalResolution(
                    CombatResolutionStatus.NOT_PLAYER_TURN,
                    "It is " + (currentTurn?.name ?: "another combatant") +
                        "'s turn."
                )
            )
        }

        val target = resolveTarget(encounter, selectedTargetId, actionText)
            ?: return PlayerPreflight(
                resolution = CombatMechanicalResolution(
                    CombatResolutionStatus.NEED_TARGET,
                    "Choose a target. Active targets: " +
                        encounter.creatures
                            .filter {
                                it.status == CreatureStatus.ACTIVE &&
                                    it.currentHp > 0
                            }
                            .joinToString(", ") { it.name }
                )
            )

        if (!target.statsResolved) {
            return PlayerPreflight(
                resolution = CombatMechanicalResolution(
                    CombatResolutionStatus.NEED_VALIDATED_STATS,
                    target.name + " does not have a validated rules stat block yet."
                )
            )
        }

        return PlayerPreflight(encounter, target)
    }

    private fun resolvePf1PlayerStrike(
        character: CharacterState,
        target: EncounterCreatureState,
        actionText: String,
        d20Roller: (Int, Int) -> CheckResult,
        damageRoller: (String) -> DiceRollResult?
    ): CombatMechanicalResolution {
        val weapon = resolveWeapon(character, actionText)
        val fullAttack = actionText.contains("full attack", true)
        val attackBonuses = if (fullAttack) {
            character.iterativeAttackBonuses(weapon)
        } else {
            listOf(character.attackBonus(weapon))
        }

        val lines = mutableListOf<String>()
        var totalDamage = 0

        attackBonuses.forEachIndexed { index, attackBonus ->
            val attack = d20Roller(attackBonus, target.armorClass)
            val hit = pf1AttackHits(attack)
            val threatens = hit && attack.die >= weapon.criticalThreatMin
            var confirmed = false
            var confirm: CheckResult? = null

            if (threatens) {
                confirm = d20Roller(attackBonus, target.armorClass)
                confirmed = pf1AttackHits(confirm)
            }

            var attackDamage = 0
            val damageParts = mutableListOf<Int>()
            if (hit) {
                val expression = appendModifier(
                    weapon.damageDice,
                    character.damageBonus(weapon)
                )
                val rolls = if (confirmed) weapon.criticalMultiplier else 1
                repeat(rolls) {
                    damageRoller(expression)?.let {
                        damageParts += it.total
                        attackDamage += it.total
                    }
                }
                totalDamage += attackDamage
            }

            val label = if (attackBonuses.size == 1) {
                "Attack"
            } else {
                "Attack " + (index + 1)
            }
            lines += buildString {
                append(label)
                append(": d20 ")
                append(attack.die)
                append(" + ")
                append(attack.modifier)
                append(" = ")
                append(attack.total)
                append(" vs AC ")
                append(target.armorClass)
                append(if (hit) " hit" else " miss")
                if (threatens) {
                    append("; threat confirmation d20 ")
                    append(confirm?.die)
                    append(" + ")
                    append(confirm?.modifier)
                    append(" = ")
                    append(confirm?.total)
                    append(if (confirmed) " confirmed" else " not confirmed")
                }
                if (damageParts.isNotEmpty()) {
                    append("; damage ")
                    append(damageParts.joinToString(" + "))
                    append(" = ")
                    append(attackDamage)
                    if (confirmed) {
                        append(" (×")
                        append(weapon.criticalMultiplier)
                        append(" critical)")
                    }
                }
            }
        }

        val effects = buildList {
            if (totalDamage > 0) {
                add(
                    GmEffect(
                        type = GmEffectType.DAMAGE_CREATURE.wireName,
                        target = target.id,
                        amount = totalDamage
                    )
                )
            }
            add(GmEffect(type = GmEffectType.ADVANCE_TURN.wireName))
        }

        val summary = character.characterName + " attacked " +
            target.name + " with " + weapon.name + ". " +
            lines.joinToString(" | ")

        return CombatMechanicalResolution(
            CombatResolutionStatus.RESOLVED,
            summary,
            effects,
            summary
        )
    }

    private fun resolvePf2PlayerStrike(
        character: CharacterState,
        target: EncounterCreatureState,
        actionText: String,
        d20Roller: (Int, Int) -> CheckResult,
        damageRoller: (String) -> DiceRollResult?
    ): CombatMechanicalResolution {
        val weapon = resolveWeapon(character, actionText)
        val attackBonus = character.attackBonus(weapon)
        val attack = d20Roller(attackBonus, target.armorClass)
        val damageExpression = appendModifier(
            weapon.damageDice,
            character.damageBonus(weapon)
        )
        val damageRoll = if (
            attack.degree == Degree.SUCCESS ||
            attack.degree == Degree.CRITICAL_SUCCESS
        ) {
            damageRoller(damageExpression)
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
            if (damage > 0) {
                add(
                    GmEffect(
                        type = GmEffectType.DAMAGE_CREATURE.wireName,
                        target = target.id,
                        amount = damage
                    )
                )
            }
            add(GmEffect(type = GmEffectType.ADVANCE_TURN.wireName))
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
            CombatResolutionStatus.RESOLVED,
            attackText + damageText + ".",
            effects,
            attackText + damageText
        )
    }

    private fun resolvePf1CreatureStrike(
        character: CharacterState,
        encounter: EncounterState,
        creature: EncounterCreatureState,
        d20Roller: (Int, Int) -> CheckResult,
        damageRoller: (String) -> DiceRollResult?
    ): CreatureTurnResolution {
        val attack = if (d20Roller === DiceEngine::d20) {
            DiceEngine.pf1Check(
                creature.attackBonus ?: 0,
                character.ac(),
                automaticOnNatural = true
            )
        } else {
            d20Roller(creature.attackBonus ?: 0, character.ac())
        }
        val hit = pf1AttackHits(attack)
        val threatens = hit && attack.die >= creature.criticalThreatMin
        val confirmation = if (threatens) {
            if (d20Roller === DiceEngine::d20) {
                DiceEngine.pf1Check(
                    creature.attackBonus ?: 0,
                    character.ac(),
                    automaticOnNatural = true
                )
            } else {
                d20Roller(creature.attackBonus ?: 0, character.ac())
            }
        } else null
        val confirmed = confirmation?.let(::pf1AttackHits) == true
        val rolls = if (confirmed) creature.criticalMultiplier else 1

        var damage = 0
        val damageParts = mutableListOf<Int>()
        if (hit) {
            repeat(rolls) {
                damageRoller(creature.damageDice)?.let { roll ->
                    damage += roll.total
                    damageParts += roll.total
                }
            }
        }

        val updated = applyDamage(character, damage)
        val events = listOf(
            GameEvent(
                "Enemy attack",
                if (hit) {
                    creature.name + " hit " + character.characterName +
                        " for " + damage + " " + creature.damageType +
                        " damage. " + updated.currentHp + "/" +
                        updated.maxHp + " HP remains."
                } else {
                    creature.name + " missed " + character.characterName + "."
                },
                "encounter"
            )
        )

        val summary = buildString {
            append(creature.name)
            append(" used ")
            append(creature.attackName.ifBlank { "an attack" })
            append(": d20 ")
            append(attack.die)
            append(" + ")
            append(attack.modifier)
            append(" = ")
            append(attack.total)
            append(" vs AC ")
            append(character.ac())
            append(if (hit) " hit" else " miss")
            if (threatens) {
                append("; threat confirmation ")
                append(confirmation?.total)
                append(if (confirmed) " confirmed" else " failed")
            }
            if (damageParts.isNotEmpty()) {
                append("; damage ")
                append(damageParts.joinToString(" + "))
                append(" = ")
                append(damage)
            }
            append(".")
        }

        return CreatureTurnResolution(
            updated,
            advanceTurn(encounter),
            events,
            summary
        )
    }

    private fun resolvePf2CreatureStrike(
        character: CharacterState,
        encounter: EncounterState,
        creature: EncounterCreatureState,
        d20Roller: (Int, Int) -> CheckResult,
        damageRoller: (String) -> DiceRollResult?
    ): CreatureTurnResolution {
        val attack = d20Roller(creature.attackBonus ?: 0, character.ac())
        val damageRoll = if (
            attack.degree == Degree.SUCCESS ||
            attack.degree == Degree.CRITICAL_SUCCESS
        ) damageRoller(creature.damageDice) else null
        val multiplier = if (attack.degree == Degree.CRITICAL_SUCCESS) 2 else 1
        val damage = (damageRoll?.total ?: 0) * multiplier
        val updated = applyDamage(character, damage)
        val outcome = when (attack.degree) {
            Degree.CRITICAL_SUCCESS -> "critical hit"
            Degree.SUCCESS -> "hit"
            Degree.FAILURE -> "miss"
            Degree.CRITICAL_FAILURE -> "critical miss"
        }
        val events = listOf(
            GameEvent(
                "Enemy attack",
                if (damage > 0) {
                    creature.name + " hit " + character.characterName +
                        " for " + damage + " " + creature.damageType +
                        " damage. " + updated.currentHp + "/" +
                        updated.maxHp + " HP remains."
                } else {
                    creature.name + " missed " + character.characterName + "."
                },
                "encounter"
            )
        )
        val summary = creature.name + " used " +
            creature.attackName.ifBlank { "an attack" } + ": " +
            attack.die + " + " + attack.modifier + " = " + attack.total +
            " vs AC " + character.ac() + " (" + outcome + ")" +
            damageRoll?.let {
                "; damage " + it.expression + " = " + it.total +
                    if (multiplier == 2) " ×2 = " + damage else ""
            }.orEmpty() + "."

        return CreatureTurnResolution(
            updated,
            advanceTurn(encounter),
            events,
            summary
        )
    }

    private fun applyDamage(character: CharacterState, damage: Int): CharacterState {
        if (damage <= 0) return character
        val absorbed = minOf(character.tempHp, damage)
        val hpDamage = (damage - absorbed).coerceAtLeast(0)
        return character.copy(
            tempHp = (character.tempHp - absorbed).coerceAtLeast(0),
            currentHp = (character.currentHp - hpDamage).coerceAtLeast(0)
        )
    }

    private fun pf1AttackHits(result: CheckResult): Boolean = when (result.die) {
        1 -> false
        20 -> true
        else -> result.total >= result.dc
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

        return active.firstOrNull { target ->
            val number = Regex("""#(\d+)$""")
                .find(target.name)
                ?.groupValues
                ?.get(1)
            val base = normalize(target.name.substringBefore("#"))
            number != null &&
                normalizedAction.contains(base) &&
                Regex("""\b$number\b""").containsMatchIn(normalizedAction)
        }
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

        if (
            ranged != null &&
            (
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

    private fun isPf1(ruleset: String): Boolean =
        GameRuleset.fromWire(ruleset) == GameRuleset.PF1E

    private fun isPf2Adapted(ruleset: String): Boolean =
        GameRuleset.fromWire(ruleset) == GameRuleset.PF2E_ADAPTED
}
