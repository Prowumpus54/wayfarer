# Working in Wayfarer

- Read README and the relevant docs. Distinguish source observations from intended requirements and tested behavior.
- Preserve `com.wayfarer.rpg`, Firebase settings/rules, signing configuration, saved data and Gradle paths. Keep `app/` in place.
- Preserve player-generated rolls through GM adjudication; no silent rerolls.
- Skills, Spells, Attacks, Actions and Inventory are intended to be character-driven dynamic tabs. Current panel labels differ.
- Assistant-created Pathfinder actions must pass rules-engine validation before save. Prompt instructions and clamping are insufficient.
- Use bounded `feature/*` changes from `develop`; reserve `main` for verified stable milestones. Never invent Git identity or release tags.
- Keep secrets and generated artifacts out of Git. Preserve useful fixtures and bundled assets. Never delete untracked files or backups as cleanup.
- For behavior changes run relevant unit tests, lint and debug build. Report device/cloud checks separately. No deployment, publication or remote creation without authorization.
- Update relevant docs when behavior or durable decisions change.
