# Release checklist

## Source and docs
- [x] Candidate branch identified; commit created after final gate.
- [x] CURRENT_STATE, KNOWN_ISSUES, CHANGELOG, and affected architecture docs match the candidate.
- [x] Version/build identifiers are intentional.
- [x] Legacy package/storage compatibility is preserved or explicitly migrated.

## Automated gate
- [x] `testDebugUnitTest` passes.
- [x] `lintDebug` passes.
- [x] `assembleDebug` passes.
- [x] `assembleRelease` passes.
- [x] Local-gateway Python syntax check passes when gateway changed.
- [x] Wise strict documentation validation passes.
- [x] `git diff --check` passes.
- [x] No secrets, signing keys, local databases, logs, SDKs, or build outputs are newly tracked.

## Device and runtime
- [ ] Fresh install/update succeeds.
- [ ] Existing save opens without data loss.
- [ ] Home/Play/Map/Journal/Glossary/Party/Diagnostics navigation works.
- [ ] PF1 encounter, initiative, attack/damage, XP, loot, and spell-resource flows work.
- [ ] Local Gemma path and Gemini fallback behave correctly.
- [ ] Offline/retry behavior preserves recorded actions and rolls.
- [ ] Diagnostics records expected evidence without sensitive content.

## Production
- [ ] Signed release build verified with Play Integrity App Check.
- [ ] Secure transport exists for any release local-LLM path.
- [ ] Artifact maps to the released commit/tag.
