package com.wayfarer.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateEngineTest {

    @Test
    fun encounterTracksCreaturesAndAwardsXpWhenLastCreatureFalls() {
        val character = CharacterState(characterName = "Tester", xp = 10)
        val started = GameStateEngine.apply(
            character = character,
            runtime = CampaignRuntimeState(),
            effects = listOf(
                GmEffect(type = "start_encounter", name = "Goblin ambush"),
                GmEffect(
                    type = "spawn_creature",
                    name = "Goblin",
                    quantity = 2,
                    maxHp = 6,
                    armorClass = 16,
                    xpValue = 50
                )
            ),
            location = "Old Hall"
        )

        val encounter = assertNotNull(started.runtime.activeEncounter)
            .let { started.runtime.activeEncounter!! }
        assertEquals(2, encounter.creatures.size)
        assertTrue(encounter.creatures.all { it.currentHp == 6 })
        assertEquals(10, started.character.xp)

        val first = encounter.creatures[0].name
        val afterFirst = GameStateEngine.apply(
            character = started.character,
            runtime = started.runtime,
            effects = listOf(
                GmEffect(
                    type = "damage_creature",
                    target = first,
                    amount = 6
                )
            ),
            location = "Old Hall"
        )
        assertEquals(
            CreatureStatus.DEFEATED,
            afterFirst.runtime.activeEncounter!!.creatures[0].status
        )
        assertEquals(EncounterStatus.ACTIVE, afterFirst.runtime.activeEncounter!!.status)
        assertEquals(10, afterFirst.character.xp)

        val second = afterFirst.runtime.activeEncounter!!.creatures[1].name
        val afterSecond = GameStateEngine.apply(
            character = afterFirst.character,
            runtime = afterFirst.runtime,
            effects = listOf(
                GmEffect(
                    type = "damage_creature",
                    target = second,
                    amount = 6
                )
            ),
            location = "Old Hall"
        )

        assertEquals(EncounterStatus.VICTORY, afterSecond.runtime.activeEncounter!!.status)
        assertTrue(afterSecond.runtime.activeEncounter!!.xpAwarded)
        assertEquals(110, afterSecond.character.xp)
    }

    @Test
    fun lootMovesFromEncounterIntoInventory() {
        val character = CharacterState(
            characterName = "Tester",
            inventory = emptyList()
        )
        val withLoot = GameStateEngine.apply(
            character = character,
            runtime = CampaignRuntimeState(),
            effects = listOf(
                GmEffect(type = "start_encounter", name = "Cache"),
                GmEffect(
                    type = "add_loot",
                    name = "Sunstone",
                    category = "Treasure",
                    quantity = 2,
                    weight = 0.1
                )
            ),
            location = "Vault"
        )

        assertEquals(2, withLoot.runtime.activeEncounter!!.loot.single().quantity)

        val taken = GameStateEngine.apply(
            character = withLoot.character,
            runtime = withLoot.runtime,
            effects = listOf(
                GmEffect(
                    type = "take_loot",
                    name = "Sunstone",
                    quantity = 1
                )
            ),
            location = "Vault"
        )

        assertEquals(1, taken.runtime.activeEncounter!!.loot.single().quantity)
        assertEquals(1, taken.character.inventory.single().quantity)
        assertEquals("Sunstone", taken.character.inventory.single().name)
    }

    @Test
    fun characterDamageConsumesTemporaryHpBeforeCurrentHp() {
        val character = CharacterState(
            characterName = "Tester",
            maxHp = 20,
            currentHp = 20,
            tempHp = 3
        )
        val result = GameStateEngine.apply(
            character = character,
            runtime = CampaignRuntimeState(),
            effects = listOf(
                GmEffect(type = "damage_character", amount = 8)
            ),
            location = "Arena"
        )

        assertEquals(0, result.character.tempHp)
        assertEquals(15, result.character.currentHp)
    }

    @Test
    fun spellSlotsTrackCapacityAndUsageSeparately() {
        val character = CharacterState(
            characterName = "Caster",
            spellSlots = mapOf(1 to 2),
            spellSlotsUsed = mapOf(1 to 1),
            spells = mapOf(1 to listOf("Magic Missile"))
        )

        assertEquals(1, character.spellSlotsRemaining(1))
        val afterCast = GameStateEngine.consumeSpell(character, "Magic Missile")
        assertNotNull(afterCast)
        assertEquals(2, afterCast!!.spellSlotsUsed[1])
        assertEquals(0, afterCast.spellSlotsRemaining(1))
        assertNull(GameStateEngine.consumeSpell(afterCast, "Magic Missile"))
    }
}
