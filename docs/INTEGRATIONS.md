# Integrations

## Firebase

Firebase Auth supplies signed-in identity. Firestore stores campaign membership/pointers and shared event streams. Firebase AI provides cloud Gemini fallback and character/inventory/advice generation.

App Check uses the debug provider only in debug builds. Release builds use Play Integrity.

## Local LLM gateway

`local-llm/gateway.py` exposes an authenticated local HTTP gateway to Ollama. The primary GM profile is `gemma4:e2b-it-qat`. LoreWise environment variables are preferred while legacy WAYFARER variables remain accepted for transition compatibility.

Auto mode uses local Gemma first when `LOCAL_LLM_URL` is configured, then cloud Gemini fallback. Release builds do not permit cleartext HTTP, so production local inference requires a secure transport.

## Jev combat classifier

`JevCombatClient.kt` calls the HTTPS Jev proxy with Firebase authentication. Jev classifies intent and routing only; Android remains authoritative for mechanics.

## Bundled data

The Sunless Citadel module and PF1 creature catalog supply campaign/combat context. The bundled PF2 SQLite catalog is legacy reference content only and must not be presented as PF1 rules evidence.

## Failure behavior

Remote failures preserve local state, surface recoverable user messaging, and record sanitized diagnostics where implemented. No prompt, token, invite code, or hidden module text belongs in diagnostic detail.
