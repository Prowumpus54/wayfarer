# Data model

## Character state

`CharacterState` is PF1-first and stores ruleset/schema version, class levels, XP track, ability scores, BAB-derived values, skill ranks/misc modifiers, AC components, saves, CMB/CMD, HP/death state, feats, inventory, spells, slot capacity, and used slots.

Existing saved characters are migrated through PF1 migration helpers. Do not rename persisted keys without an explicit versioned migration.

## Campaign runtime

`CampaignRuntimeState` stores the active encounter, creatures, initiative/turn state, encounter loot/currency, active challenge progress, durable flags, and related runtime state.

`GameStateEngine` applies bounded `GmEffect` values. Its default ruleset follows the active character rather than assuming PF2.

## Persistent namespaces

The following names are compatibility contracts despite the LoreWise product rename:
- Android package: `com.wayfarer.rpg`
- campaign preferences: `wayfarer_campaign_<campaignId>`
- character preferences: `wayfarer_character_<campaignId>`
- runtime preferences: `wayfarer_runtime_<campaignId>`
- campaign registry and GM preference namespaces
- rules SQLite/asset names and module storage paths

## Diagnostics

Diagnostics persist up to 1000 sanitized local events in the legacy-safe `wayfarer_diagnostics` preferences namespace. Diagnostic data is support metadata, not game state.
