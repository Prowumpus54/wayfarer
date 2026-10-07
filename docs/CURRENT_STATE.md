# Current state

Last updated: 2026-10-07

## Active engineering slice
Branch: `integration/lorewise-wise-standard`

Goal: integrate the completed PF1 conversion with the LoreWise/Gemma rename work, remove stale assumptions, and adopt the Wise Engineering Standard without breaking existing installs or save data.

## Version state
App: LoreWise 0.13.0, version code 17
Android package: `com.wayfarer.rpg` retained for compatibility
Primary ruleset: Pathfinder 1e

## Implemented in this branch
- PF1 game-state/combat/caster conversion from `origin/feature/pf1-conversion`
- LoreWise product/UI/code-symbol rename while preserving legacy storage identity
- local Gemma primary GM with Gemini fallback
- bounded sanitized diagnostics plus Diagnostics navigation screen
- debug/release App Check split and release cleartext restriction
- Wise-standard documentation/metadata bootstrap and project-specific documentation
- stale PF2 default removed from GameStateEngine; unused Firebase Storage dependency removed

## Verification currently completed
- `testDebugUnitTest`: passed, 35 tests / 0 failures / 0 errors
- `lintDebug`: passed with 0 errors; 21 non-blocking warnings remain
- `assembleDebug`: passed; debug APK produced
- `assembleRelease`: passed; release variant compiles/assembles with Play Integrity source set
- local gateway Python syntax check: passed
- Wise strict documentation validation: passed
- tracked-secret scan: no matching credential/signing files tracked
- `git diff --check`: passed
- Emulator: not run on this integration branch
- Physical device: not run on this integration branch
- Production deployment: not performed

## Active risks
See `KNOWN_ISSUES.md`. The main release gates are device verification, Play Integrity production configuration, secure transport for a release local-GM path, and Gradle deprecation cleanup.
