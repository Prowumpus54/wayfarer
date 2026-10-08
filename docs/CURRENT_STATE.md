# Current state

Last updated: 2026-10-07

## Active engineering slice
Branch: `integration/lorewise-p0` (target: `main`)

Goal: integrate the LoreWise P0 GM/context repair batch into one verified Android release candidate without changing the legacy Android package, Firebase identity, or persisted save namespaces.

## Version state
App: LoreWise 0.14.0, version code 19
Android package: `com.wayfarer.rpg` retained for compatibility
Primary ruleset: Pathfinder 1e

## Implemented in this branch
- authoritative PF1 game-state/combat/caster engine and local PF1 creature/rules support
- authoritative location transitions validated against the live module scene before persistence; GM narration alone cannot move the party
- compact/relevant GM context with deduplicated recent history, rolling older-history summary, current scene filtering, and complete structured character mechanics
- deterministic PF1 mechanics assistant for questions such as spells/slots, AC, saves, skills, attacks, feats/resources when authoritative data exists
- hard GM system prompt separated from swappable tone/length/pacing controls, plus bounded repetition memory and clarification behavior
- true on-device Gemma runtime using Google LiteRT-LM 0.18.0 with app-private imported model weights and explicit ready/loading/error state
- truthful GM routes: Auto -> On-device Gemma -> configured Desktop Gemma -> Gemini Flash -> Gemini Lite
- explicit model selections never silently change providers
- Fourth Wall / Meta Chat is read-only and cannot create player actions, dice, effects, movement, XP, inventory changes, or campaign events
- Context Inspector shows the exact authoritative context categories supplied to the selected model, with hidden GM context protected behind an explicit reveal
- complete Wise 1.1 app-level AI transcripts with session/request/correlation IDs, effective context, provider/model/profile, latency, routing/retry decisions, responses, proposed effects, and applied state changes
- transcript journals survive restart, queue while offline, preserve complete sessions, support archive/new-session flow, and recursively redact credentials before persistence/network upload
- authenticated desktop transcript ingest/mirror through `local-llm/gateway.py`, independent of which GM route generated the response
- bounded sanitized diagnostics remain separate from full transcripts and reference AI operations through correlation IDs
- debug/release App Check split and release cleartext restriction
- Android package/Firebase/storage identities intentionally remain Wayfarer-compatible internally

## Verification currently completed
- `testDebugUnitTest`: passed, 87 tests / 0 failures / 0 errors / 0 skipped
- `lintDebug`: passed
- `assembleDebug`: passed; 0.14.0 / version code 19 debug APK produced
- `assembleRelease`: passed; 0.14.0 release variant assembled with Play Integrity source set
- local gateway transcript suite: passed, 5 tests / 0 failures
- `python -m py_compile local-llm/gateway.py local-llm/test_transcript_ingest.py`: passed
- `git diff --check`: passed after final release metadata update
- focused integration regressions passed for routing, system/tone, context/state, mechanics, and transcripts

## Device/release state
- Pixel 10 Pro still has the prior LoreWise build; 0.14.0 physical-device install/smoke test is the next gate
- on-device Gemma weights are intentionally not bundled in Git/APK; a compatible `.litertlm` model must be obtained under its model license and imported on device
- Desktop Gemma remains an optional development/fallback route through the configured gateway; release cleartext policy means a production desktop route requires secure transport
- production deployment has not been performed

## Active risks
See `KNOWN_ISSUES.md`. Remaining release gates are physical-device verification of LiteRT-LM/model import and normal gameplay, Play Integrity production configuration, secure transport if Desktop Gemma is desired in release builds, and Gradle/Android toolchain deprecation cleanup.
