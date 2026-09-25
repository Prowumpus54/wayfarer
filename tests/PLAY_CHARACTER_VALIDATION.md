# Local validation — September 22, 2026

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`: **passed** against the final source (57 seconds).
- 11 unit tests: 7 character-action/manual-roll checks and 4 existing Jev routing checks. Zero failures or errors.
- Android lint: zero errors, 19 warnings, one hint. Dependency/SDK upgrades were not part of this change.
- APK: `C:\Wayfarer\app\build\outputs\apk\debug\app-debug.apk`.
- SHA-256: `EE18B6C2EB933E57D370C53516F4F578AF2DE0C7AE587706331067A03EB1CF45`.
- Application ID and version retained: `com.wayfarer.rpg`, 0.12.0, version code 14. This is a local development update, not a tagged release.
- Connected Pixel 8: authorized ADB device rediscovered; `install -r` returned Success. MainActivity launch accepted. No clear-data or uninstall operation was performed.
- Device UI verification: pending because the phone requires its owner to unlock the PIN screen. No PIN was requested or entered.
- Live Gemini responses, multi-client cloud synchronization, PF1e combat execution and process-death recovery of unfinished turns are **not verified**.

## Device acceptance checklist

1. In Play, open each side tab; confirm chat stays visible and the rail does not cover its text.
2. Select a skill/attack/item; confirm it fills the composer for review before sending.
3. Type a draft, visit Character/Glossary, return to Play and confirm the draft persists.
4. Make a manual roll, navigate away and back, then send it to the GM; confirm the existing result is included. Repeat with two rolls to verify ordering.
5. With a GM-requested check, interrupt network availability after rolling, then retry; confirm there is one recorded roll and the same result is sent.
6. Ask the sheet assistant to create a Perception check, review and save it, and confirm it appears in Play Actions. Edit the sheet and confirm future check modifiers use the updated sheet.
7. Request Smite + Charge + Power Attack; confirm the edition-validation explanation appears and no action is saved.
8. Search the glossary, browse a category, open a detail, navigate away and return; confirm query/category persist.
9. Ask for inventory and level-up advice; confirm the response does not silently change the sheet.

## Side-menu follow-up

At the user's request, panels now open as full-height left side menus instead of a card above chat. Tests (11), lint and debug build passed in 44 seconds. SHA-256: 7D8EAE51D6AF1043923DDDB283EDD77D10320A69436A864B9489F0C2D2596FF9. Installed with install -r and launched on the authorized Pixel 10 Pro. On-device UI inspection confirmed switching Dice to Attacks and outside-tap dismissal back to chat. Previous above-chat layout checklist item is superseded. Git/GitHub remained untouched.
