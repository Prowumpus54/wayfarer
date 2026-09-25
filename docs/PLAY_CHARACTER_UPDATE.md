# Play and character update — September 22, 2026

## Implemented

- The narrow Play rail exposes Attacks, Actions, Skills, Inventory, Spells and Dice. At the player's request, tapping a rail tab now opens a full-height left side menu instead of a card above chat. The menu temporarily covers the chat, with a dimmed strip outside it; Close, Back, tapping outside, or tapping the selected rail tab dismisses it. The rail stays in its existing position with accessibility labels. Choosing a character action fills the composer for player review.
- Panels read the active CharacterState, including equipment, spells, inventory, feats, actions and reactions. Empty spell/inventory states are explicit.
- PlaySession holds drafts, pending checks, recorded rolls, failed actions and GM status at campaign scope. GM work continues while visiting other screens. Campaign switching creates a separate session and cancels the old coroutine scope.
- Multiple manual rolls accumulate in order for the next action. They are also recorded in campaign events. The player can explicitly send the recorded rolls to the GM. A failed requested-check resolution reuses its original result and does not log a second roll.
- Character and level-up screens offer Gemini advice through the existing provider and model fallback. Generated prose cannot write the sheet. A bounded deterministic validator allows basic skill-check shortcuts, with confirmation and revalidation before saving. Unsupported actions fail closed.
- The existing local glossary remains searchable and categorized; its query/category survive screen navigation and its description explicitly identifies the bundled PF2e catalog.

## Edition boundary and remaining work

The installed character model is PF2e-adapted: proficiency includes level, the advancement form uses PF2e progressions, and the local database is PF2e. It does not contain PF1e BAB, class-level accounting, Smite Evil uses or PF1e weapon handling. No conversion of existing characters or saved data was attempted.

Smite + Charge + Power Attack is therefore rejected before save. Supporting it requires a PF1e character schema, campaign ruleset selection, prerequisites/resource tracking, target and movement checks, an action execution engine and integration tests. AI prose must not bypass this boundary. PF1e references inspected: [Charge](https://www.aonprd.com/Rules.aspx?ID=187) and [Paladin](https://legacy.aonprd.com/coreRuleBook/classes/paladin.html).

Character/inventory and level-up AI assistance is advisory; existing manual editors and inventory proposal confirmation remain the write paths. It is not a general validated AI character editor. Local references for a question are limited to matching catalog text and may be absent.

Chat events already persist on disk. Unfinished PlaySession state survives navigation within a running app, but process-death recovery of in-flight GM requests is not implemented. Cloud correctness, independent rolls requested by the GM, and live AI availability require separate verification.

## Verification

See the accompanying validation record for build, lint, unit tests and device checks. Git, branches, commits, tags, remotes and GitHub were not used for this work.
