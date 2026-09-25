# Data model

`CharacterState` in Models.kt holds identity/background, level/XP, six abilities, HP/dying/wounded, proficiency maps, equipment, feats, inventory/currency, narrative details, actions and spellcasting resources. It computes skills, saves, AC, attacks and spell DC. Small built-in weapon/armor catalogs are not exhaustive rules support.

`InventoryItem` holds name/category/quantity/weight/icon/description/mechanics. `GameEvent` holds title/body/type/time label and a UUID-backed ID. `PartyMember` is a smaller presentation model.

`CampaignStateStore` uses `wayfarer_campaign_<campaignId>` SharedPreferences for location, event JSON, discovered locations and player note; event saving takes the first 250 entries. Preserve existing keys and namespaces without an explicit migration. Character persistence lives in CharacterStore.kt; its migrations were not audited.

RulesRepository reads entries/class progression from a local read-only copy of the bundled database. Changing the asset does not automatically refresh an existing device copy.

## Local Firestore rules observed

Rules cover users/their campaign references; campaigns and nested members, characters, worldState, events, actions; and inviteCodes. Members can read/create/update characters; owners can delete them. World-state writes are owner-only. Actions require the creating member's user ID and deny updates/deletes. Campaign get permits signed-in users, while list is denied.

This describes local policy, not deployed rules or a security audit. Cloud payloads, field validation, invitation boundaries and sync conflict behavior need tests. Keep private campaign exports and credentials out of Git.
