# Diagnostics

LoreWise implements the Wise diagnostics contract in `Diagnostics.kt` and exposes it through `DiagnosticsScreen.kt`.

## Event contract

Each diagnostic event records timestamp, category, operation, status, optional duration in milliseconds, sanitized detail, and a correlation ID. Supported statuses are INFO, OK, WARN, RETRY, CANCELLED, and ERROR.

The local diagnostic history is bounded to 1000 events, persisted asynchronously, mirrored to Logcat with tag `LoreWiseDiag`, and designed so diagnostic persistence failure cannot break play. Full prompts/responses do not belong in this bounded diagnostic ring.

## Current instrumentation

- app startup and App Check provider selection
- GM routing across On-device Gemma, Desktop Gemma, Gemini Flash, and Gemini Lite
- route/model/profile, estimated context size, latency, retries, fallback decisions, and errors
- character architect, inventory assistant, and character advice AI calls
- Jev classifier request/response/failure path
- campaign event publishing/listening
- campaign load/create/invite/join cloud operations
- Google credential selection and Firebase sign-in

## Privacy

Diagnostic detail is capped and sanitized for Bearer credentials and common secret/key/password patterns. Prompts, player actions, invite codes, Firebase tokens, email addresses, hidden GM content, and full remote response bodies must not be logged in the bounded diagnostic history.

## LLM transcript observability

Wise standard 1.1.0 requires direct LLM conversations to have a complete developer-observable transcript separate from the bounded diagnostics history.

LoreWise now records, per conversation/session:
- player/app message and protected effective request;
- model response;
- provider/model/profile selection;
- timestamps and latency;
- conversation/session, message/request, event, and correlation IDs;
- routing/retry/fallback/error decisions;
- effective context categories, including authoritative location, recent and older history, character/party mechanics, module/scene data, runtime state, encounters/challenges, and hidden GM context;
- structured effects proposed by the model and the state changes actually applied by Android.

Implementation:
- `AiTranscripts.kt` defines the append-only redacted journal contract and session/archive behavior.
- `TranscriptRuntime.kt` owns app-level transcript lifecycle, offline queueing, retry, effect/state recording, restart recovery, and desktop mirroring.
- `MetaObservabilityScreen.kt` provides Fourth Wall / Meta Chat, Context Inspector, and the in-app transcript viewer.
- `local-llm/gateway.py` exposes an authenticated `/v1/transcripts` ingest endpoint and writes the protected desktop JSONL mirror.
- `ObservedCloudAi.kt` and the GM/assistant integrations route cloud/direct AI calls through the same transcript contract.

The local journal remains authoritative when the desktop is unavailable. Mirror records are acknowledged only after successful authenticated upload; unsuccessful uploads remain queued. Session journals are preserved rather than silently truncated.

All transcript content is recursively redacted at the persistence/network boundary. Known runtime secrets, authorization fields, common credential keys, Bearer values, API-key-like strings, and private-key blocks are removed before disk/network persistence.

## Support workflow

Use **Diagnostics** for bounded operational health and errors. Use **AI Transcripts** for complete AI conversation/effect history, and **Fourth Wall / Meta Chat -> Context Inspector** to see what context is actually being supplied to the selected model. Transcript context can contain GM spoilers and is protected in the UI.

## Conversation quality slice

See [GM conversation controls](GM_CONVERSATION.md) for system/tone separation, clarification and structured OOC rendering, bounded lexical repetition memory, and live route/context/latency/retry instrumentation.
