# Authoritative location and bounded GM context

Campaign location remains in the existing `wayfarer_campaign_<id>` / `location_id` preference. There is no save migration or package change.

Explicit single-destination free-text travel is interpreted locally and revalidated against the current module's unlocked outgoing connections before persistence. Stale, missing, ambiguous, locked, compound, and unsupported routes are no-ops with an unresolved result. Active encounters/challenges block travel. GM narration never commits a location. Retrying narration does not repeat a committed move.

ContextAssembler reads the campaign's saved location via the application callback for each submission and check resolution. A mismatched or missing scene produces an unresolved saved-location label and no fallback town/module material. Scene notes, NPCs, encounters and treasure are restricted to that location and bounded; destination context contains adjacent names rather than full travel descriptions. Hidden module notes stay separate from player-visible event history.

HistoryCompactor selects up to eight useful recent events, collapses nearby retry echoes, preserves chronological order and recorded rolls, and uses bounded extractive historical excerpts. Historical dialogue is explicitly marked non-authoritative. Older persisted events are summarized before the 250-event cap discards them, using the optional `history_summary` preference; old saves default to an empty summary. The summary is lossy and does not claim to preserve every older fact.

The local interpreter intentionally does not infer aliases, multi-hop routes, directional movement, or travel feasibility from prose. Structured module connections are required; unresolved movement needs a clearer adjacent destination or future structured module data.

Verification used the normal ignored local Firebase configuration copied from the authorized LoreWise workspace. `testDebugUnitTest`, `lintDebug`, and `assembleDebug` all passed with 46 tests / 0 failures / 0 errors, and a debug APK was produced. This verifies JVM rules/context behavior and Android compilation; physical-device travel/sync and cloud behavior still require integration smoke testing.
