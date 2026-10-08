# Known issues

## LW-001 — Release App Check configuration
Status: open. Release code uses Play Integrity, but production Firebase/Play Console configuration and a signed release-device verification are still required.

## LW-002 — Local GM transport in release builds
Status: open by design. Debug builds allow cleartext LAN traffic for the local gateway. Release builds disable cleartext, so a production Desktop Gemma path requires HTTPS or another secure transport. On-device Gemma uses imported private weights and no HTTP transport; see `ON_DEVICE_AI.md`.

## LW-003 — Legacy PF2 reference database
Status: contained. The bundled rules SQLite is PF2 legacy reference content. PF1 code explicitly avoids using it as PF1 evidence, but the asset remains for legacy/reference behavior.

## LW-004 — Legacy identity names
Status: intentional compatibility debt. Package ID, Firebase project identity, storage namespaces, rules asset names, the GitHub repository slug, and some filesystem paths still contain Wayfarer. Rename only with a versioned migration plan.

## LW-005 — Device/release verification
Status: pending. LoreWise 0.14.0 requires physical Pixel smoke testing after the automated gate: app upgrade/save compatibility, on-device Gemma model import/load/inference, explicit Desktop/Cloud routing behavior, authoritative movement, PF1 mechanics questions, Fourth Wall / Context Inspector, transcript viewer/mirror, and Diagnostics UI.

## LW-006 — Gradle deprecations
Status: open. Gradle reports deprecated features that will require cleanup before moving to Gradle 9.

## LW-007 — SDK/dependency maintenance
Status: open, non-blocking. Android lint passes with zero errors but reports 21 warnings, primarily newer dependency versions, KTX-style suggestions, and target SDK 36 not being the newest installed API. Upgrade these deliberately with regression testing rather than as an audit-only mass update.
