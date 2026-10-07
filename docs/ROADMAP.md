# Roadmap

## Immediate release-candidate work
1. Complete the automated gate and Wise strict documentation validation.
2. Run emulator and Pixel device smoke tests for existing-save migration, PF1 combat, local Gemma, Gemini fallback, and Diagnostics.
3. Verify signed release App Check with Play Integrity.

## Near-term rules depth
- Expand PF1 feat, spell, equipment, condition, and creature coverage beyond the bounded core catalog.
- Add deterministic validation for more class features and prerequisite chains.
- Improve encounter rewards, treasure tables, and module-specific automation without giving AI mechanical authority.

## Runtime and sync
- Define a clear conflict/ownership model for shared runtime state beyond the event stream.
- Add explicit offline/reconnect state for cloud campaign operations.
- Expand diagnostic correlation across auth, sync, Jev, AI, and build/deploy workflows.

## Platform
- Move stable cross-app diagnostics primitives into WiseCore after at least one additional Wise app validates the contract.
- Remove Gradle 9 deprecations before the toolchain upgrade.
- Design a versioned migration only if package/storage/Firebase names are ever changed from legacy Wayfarer identifiers.
