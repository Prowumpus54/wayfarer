# Current state

Last updated: 2026-10-07

## Active engineering slice
Branch: `main` (reconciled from `integration/lorewise-wise-standard`)

Goal: integrate the completed PF1 conversion with the LoreWise/Gemma rename work, remove stale assumptions, and adopt the Wise Engineering Standard without breaking existing installs or save data.

## Version state
App: LoreWise 0.13.1, version code 18
Android package: `com.wayfarer.rpg` retained for compatibility
Primary ruleset: Pathfinder 1e

## Implemented in this branch
- PF1 game-state/combat/caster conversion from `origin/feature/pf1-conversion`
- LoreWise product/UI/code-symbol rename while preserving legacy storage identity
- local Gemma primary GM with Gemini fallback
- phone-to-desktop local GM transport uses the configured Tailscale gateway endpoint so local inference remains reachable off the office LAN
- bounded sanitized diagnostics plus Diagnostics navigation screen
- debug/release App Check split and release cleartext restriction
- Wise-standard documentation/metadata bootstrap and project-specific documentation
- stale PF2 default removed from GameStateEngine; unused Firebase Storage dependency removed
- PF1 character creation now clamps generated spells to legal slot/spells-known limits

## Verification currently completed
- `testDebugUnitTest`: passed, 39 tests / 0 failures / 0 errors
- `lintDebug`: passed with 0 errors; 21 non-blocking warnings remain
- `assembleDebug`: passed; debug APK produced
- `assembleRelease`: passed; release variant compiles/assembles with Play Integrity source set
- local gateway Python syntax check: passed
- Wise strict documentation validation: passed
- tracked-secret scan: no matching credential/signing files tracked
- `git diff --check`: passed
- Emulator: LoreWise 0.13.0 installed/launched successfully; PID and startup diagnostics confirmed
- Physical device: LoreWise 0.13.0 installed on Pixel 10 Pro; 0.13.1 transport-fix deployment pending
- Main reconciliation: complete; pre-reconcile dirty state preserved on `backup/main-dirty-before-lorewise-reconcile-20261007`
- Production deployment: not performed

## Active risks
See `KNOWN_ISSUES.md`. The main release gates are device verification, Play Integrity production configuration, secure transport for a release local-GM path, and Gradle deprecation cleanup.
