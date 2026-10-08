# Roadmap

## Immediate release-candidate work
1. Complete the automated gate and Wise strict documentation validation.
2. Run emulator and Pixel device smoke tests for existing-save migration, PF1 combat, local Gemma, Gemini fallback, and Diagnostics.
3. Verify signed release App Check with Play Integrity.

## Near-term map and visual engine
- Build authoritative world graph and persistent current-room state for Oakhurst -> Old Road -> Ravine -> Citadel entry.
- Add exploration map rendering, discovery/fog, party and creature tokens, and local visual asset packs.
- Add tactical 5-foot-grid mode using the same MapState.
- Add weapon-family and spell-family live effects after mechanics resolve.
- Build desktop AssetBuildPipeline and BuildWise/Tailscale development sync for generated art packs.
- Add deterministic procedural room generation driven by reusable theme packs plus optional map-specific accent packs.
- Use `VISUAL_MAP_ASSET_ARCHITECTURE.md` as the implementation contract.

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
