# Project

## Product

LoreWise is an Android companion for running tabletop campaigns with a focus on Pathfinder 1e mechanics and The Sunless Citadel as the bundled reference campaign.

## Primary user flows

- Create/load a character and campaign.
- Explore locations, map destinations, journal events, and party state.
- Declare actions in Play and resolve deterministic mechanics locally.
- Use local Gemma or cloud Gemini for narration and bounded adjudication.
- Persist encounter, challenge, loot, spell, HP, XP, and world state.
- Inspect local diagnostics when a remote or rules path fails.

## Source of truth

- Character and PF1 math: `Models.kt`, `Pf1Rules.kt`, `Pf1CharacterSheet.kt`.
- Combat: `CombatRulesEngine.kt` and `Pf1CreatureCatalog.kt`.
- Runtime state/effects: `GameStateEngine.kt` and `GameStateStore.kt`.
- Module content: bundled module assets plus `AdventureModules.kt`.
- AI routing/prompts: `GeminiGameMaster.kt` and `local-llm/`.
- Project status: `CURRENT_STATE.md`.

Legacy PF2 assets remain for migration/reference compatibility but are not the authoritative PF1 rules source.
