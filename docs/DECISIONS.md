# Decisions and artifact review

Keep the root and existing app module intact. Add top-level knowledge documents and tests guidance; executable Android tests stay in their Gradle source set. main is for stable/released milestones, develop for integration, feature/* for bounded work. Do not invent identity/history/tags or configure a remote without authorization.

Product requirements: preserve player-generated dice rolls; character-driven Skills/Spells/Attacks/Actions/Inventory; rules-engine validation before saving assistant-created actions. Implementation differences are documented in Product and UI specification.

Ignore caches/build outputs, local SDK/IDE settings, credential/signing files, logs, temporary files, backup/release directories and local Tesseract installation. All remain on disk. Existing root XML hierarchy captures are individually ignored; root PNGs are individually excluded as apparent diagnostic captures pending curation. No blanket XML/PNG/JSON/SQLite/JAR ignore is used, so resources, module maps, fixtures, bundled databases and Gradle wrapper remain eligible.

`data-source/pf2e/` contains its own .git directory. Preserve it; decide explicitly between external source, submodule or vendoring before staging. Its revision could not be queried under the sandbox account because Git reported different ownership; no safe.directory exception was added.

`jev-smoke.json`, `modules/source/SunlessCitadel/`, and `modules/built/` remain unignored pending review of content, provenance, size, possible personal information and rebuild needs. Do not delete them. Record the source revision and required inputs when the import policy is settled.

`app/google-services.json` is excluded conservatively as locally provisioned configuration, although Firebase client configuration is not generally a private service-account key. Retain its local contents unchanged. Ignore rules are not proof that all possible secrets have been found.
