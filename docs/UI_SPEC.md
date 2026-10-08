# UI specification

LoreWise uses a dark fantasy visual system with explicit safe-area handling and predictable primary navigation.

## Primary navigation

The header/hamburger exposes Home, Play, Map, Journal, Glossary, Party, and Diagnostics. Product-visible naming is LoreWise. The package/storage compatibility name is never shown as the product brand.

## System bars

Top-level content must use system-bar-safe layout behavior. No title, menu control, or bottom interaction target may overlap Android status/navigation UI.

## Play

Play shows campaign/location context, Game Master status, recent events, runtime encounter/challenge state, character actions, dice, and bounded side panels. Model status must distinguish Local, Gemini, rules-resolved, and error states rather than labeling every response Gemini.

## Map

Map is a live campaign view backed by authoritative map state, not a static illustration. Exploration mode shows discovered rooms/areas, routes, party position, fog of war, and player-visible encounter state. Tactical mode adds a 5-foot grid, exact token footprints, targeting, movement, ranges, and effect overlays. Missing generated art must fall back to functional schematic visuals without blocking play.

## Diagnostics

Diagnostics shows event count, error count, slow-operation count, slowest operation, newest-first history, refresh, copy-report, and clear-with-confirmation.

## State behavior

Loading, unavailable, retry, unsupported-rule, pending-roll, and combat-input states must be explicit. A failed remote request must not discard a recorded action, roll, or confirmed local state.
