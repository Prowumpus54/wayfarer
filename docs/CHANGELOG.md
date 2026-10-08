# Changelog

## 0.14.0 — 2026-10-07 — GM/context and on-device AI integration

### Added
- True on-device Gemma inference through Google LiteRT-LM 0.18.0 with app-private model import/status.
- Read-only Fourth Wall / Meta Chat, Context Inspector, and complete AI transcript viewer.
- Wise 1.1 app-level transcript journals with offline queue/retry, provider/model/latency/routing data, effective context snapshots, proposed effects, and applied state changes.
- Deterministic PF1 mechanics assistant and complete character-mechanics projection for GM context.
- Separate hard GM system prompt and swappable tone/length/pacing profiles with repetition memory.

### Changed
- GM routing is now explicit and truthful: Auto -> On-device Gemma -> configured Desktop Gemma -> Gemini Flash -> Gemini Lite.
- Explicit GM choices never silently switch providers.
- Authoritative location transitions are validated/persisted by Android instead of being inferred from narration.
- GM context now uses current authoritative scene/state, compact deduplicated recent history, rolling older-history summary, and relevant module data instead of the old 24-event/raw-module payload.
- Desktop Gemma is now named distinctly from on-device inference.
- Bumped Android version to 0.14.0 / version code 19.

### Fixed
- "What spells can I cast?" and similar mechanics questions now resolve from authoritative character state rather than atmospheric narration.
- Free-text movement such as "I'll climb down the ravine" can resolve a unique connected destination without leaving authoritative state stale.
- Context Inspector and transcripts use the same authoritative context assembly as normal GM turns.
- Transcript secrets are recursively redacted before persistence or desktop upload.

### Verification
- 87 Android unit tests passed with 0 failures/errors/skips.
- `lintDebug`, `assembleDebug`, `assembleRelease`, gateway transcript tests, Python syntax checks, and the integrated unit suite passed for the 0.14.0 candidate.
- Physical-device smoke testing remains the next release gate.

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
