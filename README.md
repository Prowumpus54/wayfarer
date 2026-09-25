# Wayfarer

Android tabletop role-playing companion with Pathfinder-style characters, campaign play, maps, journals, rules lookup and Firebase services. Source inspection is not release or device verification.

The existing `app/` module declares `com.wayfarer.rpg`, version `0.12.0` (code 14), minimum SDK 26, compile/target SDK 36 and Java 17. No source paths have been moved.

## Knowledge map

- [Product](docs/PRODUCT.md)
- [Architecture](docs/ARCHITECTURE.md)
- [UI specification](docs/UI_SPEC.md)
- [Data model](docs/DATA_MODEL.md)
- [Roadmap](docs/ROADMAP.md)
- [Decisions and artifact inventory](docs/DECISIONS.md)
- [Working instructions](AGENTS.md)
- [Validation](tests/README.md)

## Development

Open this root in Android Studio with JDK 17 and SDK 36. Keep local SDK settings in ignored `local.properties`. Provision the authorized `app/google-services.json` locally on a new machine; do not casually substitute a different Firebase project. Bundled SQLite and adventure assets are build inputs.

From this root on Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

These are suggested verification commands, not recorded passes. Executable Android tests remain in `app/src/test/`.

## Git workflow

`main` is reserved for verified stable/released milestones. The imported working tree is not yet verified as a release. Configure your actual repository-local `user.name` and `user.email`; setup does not invent an identity. Review candidate files and the staged diff before the first commit. Then create `develop` for integration and `feature/<topic>` branches from it. Merge verified milestones to `main`.

Use tags such as `wayfarer-v0.12.0` only for actual verified releases. Setup creates no tags or remote and performs no publication/deployment. See Decisions before bulk staging: the rules source contains a nested repository and some module/artifact content needs review.
