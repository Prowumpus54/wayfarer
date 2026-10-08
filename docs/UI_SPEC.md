# UI specification

LoreWise uses a dark fantasy visual system with explicit safe-area handling and predictable primary navigation.

## Primary navigation

The header/hamburger exposes Home, Play, Map, Journal, Glossary, Party, and Diagnostics. Product-visible naming is LoreWise. The package/storage compatibility name is never shown as the product brand.

## System bars

Top-level content must use system-bar-safe layout behavior. No title, menu control, or bottom interaction target may overlap Android status/navigation UI.

## Play

Play shows campaign/location context, Game Master status, recent events, runtime encounter/challenge state, character actions, dice, and bounded side panels. Model status distinguishes On-device Gemma, Desktop Gemma, Gemini Flash/Lite, rules-resolved, and error states. The model picker exposes private Gemma import and NOT_INSTALLED/INSTALLED/LOADING/READY/ERROR lifecycle status. Auto alone crosses providers; explicit selections fail clearly. See `ON_DEVICE_AI.md`.

## Diagnostics

Diagnostics shows event count, error count, slow-operation count, slowest operation, newest-first history, refresh, copy-report, and clear-with-confirmation.

## State behavior

Loading, unavailable, retry, unsupported-rule, pending-roll, and combat-input states must be explicit. A failed remote request must not discard a recorded action, roll, or confirmed local state.
