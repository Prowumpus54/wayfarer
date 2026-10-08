# Architecture

LoreWise is a single Android application with local persistence, bundled rules/module data, optional local inference, and Firebase-backed identity/campaign sync.

## Authority boundaries

- `MainActivity.kt` composes campaign, character, runtime state, navigation, sync, and persistence.
- `GameStateEngine.kt` validates/applies bounded state effects and owns persistent game-state changes.
- `CombatRulesEngine.kt` resolves initiative, PF1 attacks, criticals, maneuvers, damage, and turn advancement.
- `Pf1Rules.kt` owns PF1 character math, XP, skills, saves, BAB, spell progression, and migration helpers.
- `Pf1CreatureCatalog.kt` supplies authoritative PF1 creature combat profiles for supported encounters.
- `GeminiGameMaster.kt` handles narration/adjudication. `GmRouter.kt` enforces explicit choices and Auto order: on-device Gemma, configured Desktop Gemma, Gemini Flash, then Lite. `OnDeviceGemmaRuntime.kt` uses Google LiteRT-LM; `GemmaModelStore.kt` owns app-private imported weights. See `ON_DEVICE_AI.md`.
- `JevCombatClient.kt` classifies bounded combat intent; Android still owns the final mechanics.
- `Diagnostics.kt` records bounded sanitized operational evidence. `DiagnosticsScreen.kt` exposes it in-app.
- The planned map/visual subsystem is specified in `VISUAL_MAP_ASSET_ARCHITECTURE.md`: MapStateEngine owns authoritative geometry/location/discovery, while MapRenderEngine, VisualAssetEngine, EffectEngine, and the desktop AssetBuildPipeline remain presentation/infrastructure layers.

## Persistence

Legacy `wayfarer_*` SharedPreferences/database/asset names remain intentionally stable. Product rename does not imply storage migration.

## Security

Debug builds may use cleartext LAN access for the local LLM and Firebase App Check debug provider. Release builds disable cleartext and use Play Integrity App Check.

## Legacy PF2 boundary

The bundled `wayfarer_rules.sqlite` is legacy PF2 reference data. PF1 characters never use it as authoritative PF1 evidence; PF1 combat uses dedicated PF1 rules/catalog code.

## Conversation quality slice

See [GM conversation controls](GM_CONVERSATION.md) for system/tone separation, clarification and structured OOC rendering, bounded lexical repetition memory, and live route/context/latency/retry instrumentation.
