# Changelog

## 0.13.1 — 2026-10-07 — local GM transport fix

### Changed
- Bumped Android version to 0.13.1 / version code 18.
- Phone builds now use the configured LoreWise Tailscale gateway endpoint for the local Gemma GM path instead of the office-LAN-only endpoint.
- Verified Gemma 4 E2B answers through the LoreWise local gateway before rebuild.

## 0.13.0 — 2026-10-07 — integration candidate

### Added
- PF1 authoritative game-state/combat conversion, creature catalog, negative-HP/death handling, caster slot progression, and Sunless Citadel PF1 runtime support.
- Wise-standard project documentation and `buildwise-project.json`.
- Bounded sanitized in-app diagnostics screen and operational event store.
- Release Play Integrity App Check configuration.

### Changed
- Product-visible identity from Wayfarer to LoreWise.
- Auto GM routing to local Gemma first when configured, with Gemini cloud fallback.
- State-engine default ruleset now follows the active character.
- PF1 character creation validates generated spell lists against class slot/spells-known limits.
- Local LLM environment variables are LoreWise-first with legacy compatibility fallbacks.
- Release builds disable cleartext traffic; debug keeps LAN local-model development support.

### Removed
- Unused Firebase Storage dependency.
- Stale product-facing Wayfarer labels and Qwen-as-primary-GM descriptions.

### Compatibility
Android package ID, Firebase project identity, persisted `wayfarer_*` namespaces, and legacy rules/module paths remain unchanged intentionally.
