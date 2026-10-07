# Roadmap

1. Supply actual Git identity, review candidate files and create the baseline commit; then create develop. Review the nested rules repository and module provenance before bulk staging.
2. Run Android unit tests, lint and debug build before treating the baseline as stable. A Gradle version is not release evidence.
3. Trace/test player-roll preservation through local execution, AI adjudication and cloud event paths.
4. Reconcile the current rail with the intended character-driven tabs; verify switching and empty states.
5. Add an explicit rules-engine validation boundary before saving assistant actions, with invalid prerequisite/resource/unsupported-action cases.
6. Verify persistence upgrades, invitations, Firestore permissions and sync conflicts without changing existing user data.

Device, live account and remote AI checks require separate evidence. No completion dates or production-readiness claims are implied.

## September 22 local development update

Git work is deferred at the user's request. See [Play and character update](PLAY_CHARACTER_UPDATE.md) for implemented behavior and the remaining PF1e schema/action-engine work. Build and device evidence is in [validation](../tests/PLAY_CHARACTER_VALIDATION.md).


## Game-state engine update

Implemented on `feature/game-state-engine`:
- persistent encounter/challenge/world runtime state;
- structured GM state effects and recent-history/runtime context injection;
- individual creature HP/conditions/status and encounter loot tracking;
- character HP, inventory/currency and spell-resource persistence;
- separate spell-slot capacity and usage with automatic Play-panel consumption;
- module encounter-creature and treasure context;
- live Play-screen encounter/challenge tracker;
- unit coverage for creature/XP completion, loot transfer, temp-HP damage and spell slots.

Remaining before calling combat mechanically authoritative:
- resolve creature stat blocks from the selected ruleset/module instead of relying on unresolved placeholders for improvised creatures;
- route attacks, saves, damage, initiative and turn order through a rules-engine result object rather than model-selected mechanics;
- define PF1e-specific XP/CR and spell-resource rules once the current PF2e-adapted data/model mismatch is resolved;
- add cloud synchronization/conflict policy for runtime state.
