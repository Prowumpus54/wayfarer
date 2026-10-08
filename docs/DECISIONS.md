# Engineering decisions

## Preserve installed identity during product rename
LoreWise keeps Android package `com.wayfarer.rpg`, Firebase project identity, legacy `wayfarer_*` preferences/databases, and compatible filesystem paths. Reason: a cosmetic rename must not orphan installed data or break cloud identity. A future rename requires an explicit migration.

## PF1 is the primary authoritative ruleset
Character defaults, bundled Sunless Citadel runtime behavior, PF1 creature profiles, and local combat resolution target Pathfinder 1e. PF2 assets remain legacy/reference-only.

## Android owns mechanics
AI is not trusted to roll dice or commit authoritative combat/resource values. Structured effects are validated/applied by the app.

## Local-first GM routing
Auto tries on-device Gemma, configured Desktop Gemma, Gemini Flash, then Lite. Explicit choices never switch providers. Android uses Google's current LiteRT-LM Kotlin API (0.18.0), with Kotlin/Compose compiler 2.4.0 for its metadata compatibility. Models are user-imported into app-private storage after external license acceptance; no model is bundled or automatically downloaded. The legacy Qwen picker entry is removed; saved HTTP choices map to Desktop Gemma. See `ON_DEVICE_AI.md`.

## Release security differs from debug convenience
Debug may use cleartext LAN access and App Check debug provider. Release disables cleartext and uses Play Integrity.

## Diagnostics are a local prototype for WiseCore
LoreWise currently owns the diagnostics implementation locally. Promote it to WiseCore only after behavior stabilizes across multiple Wise apps.
