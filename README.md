# LoreWise

LoreWise is an Android tabletop RPG companion focused on Pathfinder 1e play, persistent campaign state, deterministic local rules, and AI-assisted narration.

Current development version: **0.13.0** (code 17). Android package ID remains `com.wayfarer.rpg` intentionally so existing installs and saved data continue to work.

## Build and verify

`gradlew.bat testDebugUnitTest lintDebug assembleDebug`

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Engineering entry points

Start with `docs/INDEX.md`, then `docs/CURRENT_STATE.md` and `docs/AI_CONTEXT.md`. The repository follows the Wise Engineering Standard and exposes metadata through `buildwise-project.json`.

## Compatibility

Product/UI naming is LoreWise. Legacy `wayfarer_*` preference/database names, the package ID, Firebase project identity, and some on-disk paths remain unchanged until an explicit migration is designed and tested.

## Runtime model

Android owns dice, combat math, state transitions, HP, XP, initiative, spell resources, loot, and persistence. Auto tries on-device Gemma (Google LiteRT-LM), configured Desktop Gemma, Gemini Flash, then Gemini Lite. Explicit model choices never switch providers. See [on-device AI](docs/ON_DEVICE_AI.md) for model import and verification boundaries. AI narration may request bounded effects but cannot override authoritative mechanics.
