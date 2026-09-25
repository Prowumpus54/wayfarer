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
