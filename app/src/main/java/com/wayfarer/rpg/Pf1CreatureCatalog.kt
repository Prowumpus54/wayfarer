package com.wayfarer.rpg

object Pf1CreatureCatalog {
    private val goblin = CreatureCombatProfile(
        ruleRef = "pf1:creature:goblin",
        name = "Goblin",
        level = 0,
        maxHp = 6,
        armorClass = 16,
        initiativeBonus = 6,
        fortitude = 3,
        reflex = 2,
        will = -1,
        attacks = listOf(
            CreatureAttackProfile(
                name = "Short Sword",
                attackBonus = 2,
                damageDice = "1d4",
                damageType = "piercing",
                criticalThreatMin = 19,
                criticalMultiplier = 2
            ),
            CreatureAttackProfile(
                name = "Shortbow",
                attackBonus = 4,
                damageDice = "1d4",
                damageType = "piercing",
                rangeFt = 60,
                criticalThreatMin = 20,
                criticalMultiplier = 3
            )
        ),
        ruleset = GameRuleset.PF1E,
        challengeRating = "1/3",
        xpValue = 135,
        touchArmorClass = 13,
        flatFootedArmorClass = 14,
        baseAttackBonus = 1,
        cmb = 0,
        cmd = 12
    )

    private val kobold = CreatureCombatProfile(
        ruleRef = "pf1:creature:kobold",
        name = "Kobold",
        level = 0,
        maxHp = 5,
        armorClass = 15,
        initiativeBonus = 1,
        fortitude = 2,
        reflex = 1,
        will = -1,
        attacks = listOf(
            CreatureAttackProfile(
                name = "Spear",
                attackBonus = 1,
                damageDice = "1d6-1",
                damageType = "piercing",
                criticalMultiplier = 3
            ),
            CreatureAttackProfile(
                name = "Sling",
                attackBonus = 3,
                damageDice = "1d3-1",
                damageType = "bludgeoning",
                rangeFt = 50
            )
        ),
        ruleset = GameRuleset.PF1E,
        challengeRating = "1/4",
        xpValue = 100,
        touchArmorClass = 12,
        flatFootedArmorClass = 14,
        baseAttackBonus = 1,
        cmb = -1,
        cmd = 10
    )

    private val direRat = CreatureCombatProfile(
        ruleRef = "pf1:creature:dire-rat",
        name = "Dire Rat",
        level = 0,
        maxHp = 5,
        armorClass = 14,
        initiativeBonus = 3,
        fortitude = 3,
        reflex = 5,
        will = 1,
        attacks = listOf(
            CreatureAttackProfile(
                name = "Bite",
                attackBonus = 1,
                damageDice = "1d4",
                damageType = "piercing"
            )
        ),
        ruleset = GameRuleset.PF1E,
        challengeRating = "1/3",
        xpValue = 135,
        touchArmorClass = 14,
        flatFootedArmorClass = 11,
        baseAttackBonus = 0,
        cmb = -1,
        cmd = 12
    )

    private val skeleton = CreatureCombatProfile(
        ruleRef = "pf1:creature:human-skeleton",
        name = "Human Skeleton",
        level = 0,
        maxHp = 4,
        armorClass = 16,
        initiativeBonus = 6,
        fortitude = 0,
        reflex = 2,
        will = 2,
        attacks = listOf(
            CreatureAttackProfile(
                name = "Claw",
                attackBonus = 2,
                damageDice = "1d4+2",
                damageType = "slashing"
            )
        ),
        ruleset = GameRuleset.PF1E,
        challengeRating = "1/3",
        xpValue = 135,
        touchArmorClass = 12,
        flatFootedArmorClass = 14,
        baseAttackBonus = 0,
        cmb = 2,
        cmd = 14
    )

    private val bugbear = CreatureCombatProfile(
        ruleRef = "pf1:creature:bugbear",
        name = "Bugbear",
        level = 0,
        maxHp = 16,
        armorClass = 17,
        initiativeBonus = 1,
        fortitude = 2,
        reflex = 4,
        will = 1,
        attacks = listOf(
            CreatureAttackProfile(
                name = "Morningstar",
                attackBonus = 5,
                damageDice = "1d8+3",
                damageType = "bludgeoning/piercing"
            ),
            CreatureAttackProfile(
                name = "Javelin",
                attackBonus = 3,
                damageDice = "1d6+3",
                damageType = "piercing",
                rangeFt = 30
            )
        ),
        ruleset = GameRuleset.PF1E,
        challengeRating = "2",
        xpValue = 600,
        touchArmorClass = 11,
        flatFootedArmorClass = 16,
        baseAttackBonus = 2,
        cmb = 5,
        cmd = 16
    )

    private val hobgoblin = CreatureCombatProfile(
        ruleRef = "pf1:creature:hobgoblin",
        name = "Hobgoblin",
        level = 0,
        maxHp = 17,
        armorClass = 16,
        initiativeBonus = 2,
        fortitude = 5,
        reflex = 2,
        will = 1,
        attacks = listOf(
            CreatureAttackProfile(
                name = "Longsword",
                attackBonus = 4,
                damageDice = "1d8+2",
                damageType = "slashing",
                criticalThreatMin = 19
            ),
            CreatureAttackProfile(
                name = "Longbow",
                attackBonus = 3,
                damageDice = "1d8",
                damageType = "piercing",
                rangeFt = 100,
                criticalMultiplier = 3
            )
        ),
        ruleset = GameRuleset.PF1E,
        challengeRating = "1/2",
        xpValue = 200,
        touchArmorClass = 12,
        flatFootedArmorClass = 14,
        baseAttackBonus = 1,
        cmb = 3,
        cmd = 15
    )

    private val profiles = listOf(
        goblin, kobold, direRat, skeleton, bugbear, hobgoblin
    )

    fun resolve(query: String): CreatureCombatProfile? {
        val normalized = normalize(query)
        if (normalized.isBlank()) return null

        val aliases = when {
            normalized.contains("hobgoblin") -> listOf("hobgoblin")
            normalized.contains("bugbear") -> listOf("bugbear")
            normalized.contains("dire rat") -> listOf("dire rat")
            normalized.contains("skeleton") -> listOf("human skeleton")
            normalized.contains("kobold") -> listOf("kobold")
            normalized.contains("goblin") -> listOf("goblin")
            else -> emptyList()
        }

        return profiles.firstOrNull { profile ->
            val name = normalize(profile.name)
            name == normalized ||
                aliases.any { name == it } ||
                normalize(profile.ruleRef) == normalized
        }
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
}
