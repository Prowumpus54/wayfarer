# Product

Wayfarer is an Android tabletop role-playing companion. Source includes character creation/sheets, inventory, campaign hub, play, map, journal, glossary, Google account integration and AI-assisted character/game-master components. Models and prompts use Pathfinder 2e-style concepts; complete rules compliance is not established.

## Intended decisions

1. Player-generated dice results must survive GM processing unchanged. Authorized rerolls must be explicit.
2. Skills, Spells, Attacks, Actions and Inventory are character-driven dynamic tabs, based on equipment, spells, proficiencies, feats and carried items.
3. A character assistant may construct Pathfinder actions but a rules engine must validate them before persistence.

These are requirements from prior product decisions, not claims of end-to-end enforcement. `CharacterArchitect` requests a legal level-one hero, parses JSON, clamps values and restricts supported weapons/armor; that does not demonstrate complete legality validation.

Exact offline guarantees, multiplayer conflict behavior and live AI/cloud reliability require separate verification.
