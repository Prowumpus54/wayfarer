# Validation

Executable Android unit tests stay in `app/src/test/java/com/wayfarer/rpg/`. `JevCombatPolicyTest` checks four routing cases: confident bounded action routes locally; confident GM route, low confidence and creative probability escalate. These tests were inspected, not run during repository setup.

Run `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` from the root with Java/SDK/Firebase configuration available. Use this directory for future cross-component plans and curated fixtures; moving executable tests here requires Gradle changes.

Before release verify roll preservation, character-dependent panels, assistant validation before save, persistence compatibility and Firestore boundaries. Record local checks separately from emulator/device and live cloud evidence.
