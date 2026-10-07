# Tests

Executable Android unit tests live under `app/src/test/java/com/wayfarer/rpg/`.

The current suite covers character action validation, authoritative combat, creature profile parsing/catalog resolution, runtime game-state effects, Jev routing, PF1 combat, and PF1 character/caster progression.

Primary automated gate:

`gradlew.bat testDebugUnitTest lintDebug assembleDebug`

See `docs/TEST_MATRIX.md` for release-candidate verification and `docs/CURRENT_STATE.md` for the latest results.
