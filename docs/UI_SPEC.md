# UI specification

`AppScreen` declares Home, Play, Map, Journal, Glossary and Party. Additional source implements character creation/sheets, inventory and campaign hub.

The current Play rail is fixed: Actions, Skills, Items, Spells, Dice. Attacks are inside Actions. Actions use selected weapons, general actions and character feats; Skills use character bonuses; Items reads inventory; Spells reads rank-grouped character spells and shows an empty state.

The intended design is character-driven dynamic Skills, Spells, Attacks, Actions and Inventory tabs. Contents must follow the active character. Exact visibility/layout and placement of the current Dice panel remain to be specified. Do not describe the target as already complete.

Player rolls must remain authoritative through GM adjudication. Assistant-action validation failures should explain required corrections before save.

Proposed acceptance checks: switch characters with different weapons/spells; test a noncaster; update inventory; preserve an explicit roll through GM response; reject an illegal generated action before persistence. These are not recorded passes.
