# Data model

`CharacterState` in Models.kt holds identity/background, level/XP, six abilities, HP/dying/wounded, proficiency maps, equipment, feats, inventory/currency, narrative details, actions and spellcasting resources. Spell-slot daily capacity (`spellSlots`) is tracked separately from usage (`spellSlotsUsed`) so casting can consume a slot without destroying the character's daily capacity. It computes skills, saves, AC, attacks and spell DC. Small built-in weapon/armor catalogs are not exhaustive rules support.

`InventoryItem` holds name/category/quantity/weight/icon/description/mechanics. `GameEvent` holds title/body/type/time label and a UUID-backed ID. `PartyMember` is a smaller presentation model.

`CampaignStateStore` uses `wayfarer_campaign_<campaignId>` SharedPreferences for location, event JSON, discovered locations and player note; event saving takes the first 250 entries. Preserve existing keys and namespaces without an explicit migration. Character persistence lives in CharacterStore.kt; its migrations were not audited.

RulesRepository reads entries/class progression from a local read-only copy of the bundled database. Changing the asset does not automatically refresh an existing device copy.

## Local Firestore rules observed

Rules cover users/their campaign references; campaigns and nested members, characters, worldState, events, actions; and inviteCodes. Members can read/create/update characters; owners can delete them. World-state writes are owner-only. Actions require the creating member's user ID and deny updates/deletes. Campaign get permits signed-in users, while list is denied.

This describes local policy, not deployed rules or a security audit. Cloud payloads, field validation, invitation boundaries and sync conflict behavior need tests. Keep private campaign exports and credentials out of Git.


## Live campaign runtime

`CampaignRuntimeState` contains one active `EncounterState`, one active `ChallengeState`, and durable world flags.

An `EncounterState` owns individually addressable `EncounterCreatureState` records with HP, AC, conditions, status, optional rule reference, XP value and a `statsResolved` marker. It also owns encounter loot and coin pools plus an XP-awarded guard. Improvised creatures can therefore exist immediately while unresolved/placeholder mechanics remain visibly distinguishable from validated creature stats.

`GmEffect` is the structured bridge between GM narration and persisted game state. Supported mutations include encounter start/spawn, character/creature damage and healing, loot/item/currency changes, conditions, world flags, challenges, spell-resource spending and encounter completion. Random damage/healing may be expressed as dice notation and is rolled locally by `DiceEngine`.

`CharacterStore` persists current/temp HP, dying/wounded, conditions, currency, focus resources, spell-slot capacity and usage, spells and inventory. These resource fields are required for GM/runtime state transitions to survive app restarts.
