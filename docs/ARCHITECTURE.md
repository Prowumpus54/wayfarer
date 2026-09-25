# Architecture

| Path | Observed responsibility |
| --- | --- |
| `app/` | Only module included in settings.gradle; Kotlin/Compose Android app |
| `app/src/main/java/com/wayfarer/rpg/` | Screens, models, stores, repositories, AI clients and combat policy |
| `app/src/main/assets/` | Rules SQLite database and adventure modules |
| `app/src/test/` | JUnit combat-routing tests |
| `backend/jev-proxy/` | JavaScript proxy, Vercel configuration and Firebase Admin dependency |
| `jev/` | Python client and benchmark scripts |
| `tools/` | Rules DB/module builders and module SQL schema |
| `data-source/pf2e/` | Large nested Git repository of rules source |
| `modules/source/`, `modules/built/` | Module inputs and prepared outputs |
| `firebase.json`, `firestore.rules` | Local Firestore policy configuration |

Root plugins declare Android Gradle plugin 8.13.2 and Kotlin/Compose 2.2.20. App dependencies include Compose, Firebase Auth/Firestore/Storage/AI/App Check debug and Google identity credentials. Presence does not verify production configuration.

`Models.kt` owns character calculations/navigation. `PlayCharacterPanels.kt` renders character-derived content. `CharacterArchitect.kt` produces parsed characters via Firebase AI. `RulesRepository.kt` copies asset `wayfarer_rules.sqlite` to local `wayfarer_rules_v1.sqlite` on first use, then queries it read-only. A searchable catalog is not a complete action-validation engine.

`CampaignStateStore.kt` persists per-campaign SharedPreferences. Cloud/sync files are `CampaignCloudRepository.kt` and `CampaignSyncRepository.kt`; exact retry/conflict behavior remains unverified. Keep all existing build paths intact.
