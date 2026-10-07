# Diagnostics

LoreWise implements the Wise diagnostics contract in `Diagnostics.kt` and exposes it through `DiagnosticsScreen.kt`.

## Event contract

Each event records timestamp, category, operation, status, optional duration in milliseconds, sanitized detail, and a correlation ID. Supported statuses are INFO, OK, WARN, RETRY, CANCELLED, and ERROR.

The local history is bounded to 1000 events, persisted asynchronously, mirrored to Logcat with tag `LoreWiseDiag`, and designed so diagnostic persistence failure cannot break play.

## Current instrumentation

- app startup and App Check provider selection
- local Gemma GM calls and cloud Gemini GM fallback
- character architect, inventory assistant, and character advice AI calls
- Jev classifier request/response/failure path
- campaign event publishing/listening
- campaign load/create/invite/join cloud operations
- Google credential selection and Firebase sign-in

## Privacy

Diagnostic detail is capped and sanitized for Bearer credentials and common secret/key/password patterns. Prompts, player actions, invite codes, Firebase tokens, email addresses, hidden GM content, and full remote response bodies must not be logged.

## LLM transcript observability

Wise standard 1.1.0 requires direct LLM conversations to have a complete developer-observable transcript separate from the bounded diagnostics history.

LoreWise must mirror, per conversation/session:
- player/app messages sent to the model;
- model responses;
- model/profile/provider selection;
- timestamps and latency;
- conversation/message/correlation IDs;
- routing/retry/fallback/error decisions;
- the protected effective-context snapshot supplied to the model, clearly separating visible chat from hidden GM/module/runtime context;
- structured effects/state changes caused by the response.

The mirror must be readable from the authorized desktop/BuildWise path, queue locally while offline, synchronize when connectivity returns, and never contain auth tokens or other credentials.

Status: **partially implemented**. Local Gemma traffic is now mirrored by `local-llm/gateway.py` into an untracked desktop JSONL transcript containing the exact prompt, response, model/profile, timestamps, latency, client address, status, and request ID. The broader app-level layer for cloud Gemini/other AI helpers, offline queuing, and unified conversation archives remains pending.

## Support workflow

Open Diagnostics from the main navigation, inspect error/slow counts, refresh history, copy a sanitized report, or clear local diagnostic history. Copying does not clear the source events.
