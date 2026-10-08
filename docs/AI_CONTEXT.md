# AI context

LoreWise is a Pathfinder 1e-first Android tabletop RPG companion. It combines deterministic local game rules and persistent campaign state with AI-assisted narration, character guidance, and optional local-model inference.

## Non-negotiable invariants

- Android is authoritative for dice, HP, XP, initiative, attacks, spell resources, inventory, loot, and durable game state.
- AI may narrate and request bounded effects; it must not invent or silently commit authoritative mechanics.
- Preserve a player's recorded roll across retry/fallback paths.
- PF1 is the primary ruleset. The bundled PF2 database is legacy reference content and is never PF1 evidence.
- Preserve `com.wayfarer.rpg` and existing `wayfarer_*` storage names until a tested migration exists.
- Auto tries on-device Gemma, configured Desktop Gemma, Gemini Flash, then Lite. Explicit choices never switch providers. On-device uses Google LiteRT-LM and user-imported weights; see `ON_DEVICE_AI.md`.
- Never log prompts, auth tokens, invite codes, personal data, or hidden module content in diagnostics.
- Diagnostics are bounded, sanitized, asynchronous, and must never block play.
- Debug may use LAN cleartext for local development; release builds must not.
- Debug App Check uses the debug provider; release uses Play Integrity.

## Before editing

Read `CURRENT_STATE.md`, keep unrelated work isolated, update affected docs in the same branch, and verify the smallest relevant slice before the full release gate.
