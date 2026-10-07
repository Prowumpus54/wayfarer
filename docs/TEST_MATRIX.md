# Test matrix

Primary automated command:

`gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease`

## Required gates

| Gate | Required | Evidence |
| --- | --- | --- |
| Kotlin/Android compile | Yes | Gradle debug compile/build |
| Unit tests | Yes | JUnit XML under app/build/test-results |
| Android lint | Yes | lintDebug report and task result |
| Debug APK build | Yes | app/build/outputs/apk/debug/app-debug.apk |
| Release variant compile/build | Yes for integration candidate | assembleRelease / release APK output |
| Python local-gateway syntax | Yes when gateway changes | python -m py_compile |
| Documentation standard | Yes | Wise strict project-doc validator |
| Secret/build-output hygiene | Yes | tracked-file scan + git status/diff check |
| Emulator smoke | For release candidate | launch + changed flows |
| Physical device smoke | For distributable build | install/launch/network/local-GM/diagnostics |
| Release App Check | For production release | signed build with Play Integrity |

## Core unit coverage

Tests cover action validation, authoritative combat resolution, creature parsing/catalog behavior, runtime game-state effects, Jev routing policy, PF1 combat rules, and PF1 character/caster progression.

Verification results belong in `CURRENT_STATE.md`; do not infer device or production verification from a successful Gradle build.
