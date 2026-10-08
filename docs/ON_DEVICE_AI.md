# On-device Gemma and GM routing

This factory slice integrates Google's LiteRT-LM Kotlin Android runtime:
`com.google.ai.edge.litertlm:litertlm-android:0.18.0` from Google Maven.
Version 0.18.0 was verified against Google's Maven metadata on 2026-10-07
(release updated 2026-10-06). API: `Engine`, `EngineConfig`, `Backend.CPU()`,
`initialize()`, `createConversation()`, `sendMessage(prompt).toString()`.
The tagged 0.18.0 `Message` API exposes response content through `toString()`;
the guide's `.text` example does not match the published artifact.
Source: https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/kotlin/java/com/google/ai/edge/litertlm/Message.kt
No MediaPipe LLM Inference dependency is used. LiteRT-LM 0.18.0 ships Kotlin
2.4 metadata, so the Kotlin Android and Compose compiler plugins are aligned at
2.4.0. Gradle 8.13 and AGP 8.13.2 remain unchanged (supported by Kotlin 2.4.0).
Compatibility reference: https://kotlinlang.org/docs/gradle-configure-project.html

Official references:
- https://developers.google.com/edge/litert-lm/android
- https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/litertlm-android/maven-metadata.xml
- https://github.com/google-ai-edge/LiteRT-LM

## Routing contract
The choices are Auto, On-device Gemma, Desktop Gemma, Gemini 3.8 Flash,
and Gemini 3.5 Flash Lite. Explicit choices never switch routes on failure.
Cloud transient retries stay on the selected cloud model.
Auto tries on-device, then configured/reachable desktop, then Flash, then Lite.
An unavailable device model, unreachable desktop, empty answer, or cloud error
advances Auto to the next route. Cancellation never initiates fallback.
All attempts use exactly the same prompt, including recorded player rolls.
The selected answering route is returned in `GmAnswer.route`, surfaced through
`GmTurn.modelName`, and recorded as sanitized `gm_route` diagnostic metadata.

Desktop retains the existing Tailscale/HTTP `LOCAL_LLM_URL` and token configuration,
request contract and GM profile. Its diagnostic event is now `gm_desktop`.
Legacy saved `LOCAL` and `LOCAL_FAST` choices migrate to Desktop Gemma;
the old Qwen picker entry is removed. No legacy HTTP selection becomes on-device.

## Model installation
In Play, open Game Master Model and choose **Import on-device model**.
Obtain compatible Gemma (including supported Gemma 4 mobile variants) weights
in LiteRT-LM's `.litertlm` format after accepting the distributor's license.
GGUF/Ollama and MediaPipe task files are not interchangeable with LiteRT-LM.
The Android document picker grants access to one selected document; the app
streams it into `filesDir/gemma/gemma.litertlm`. The filename is not trusted
as format validation: LiteRT-LM initialization validates model compatibility.
No storage permission, credentials or persistent source URI grant is required.
Keep enough free space for both the old and incoming models during replacement.

Weights are not bundled, downloaded automatically, committed, or placed in the APK.
Licensed/gated downloads require the user's credentials/license acceptance outside
the app. Import is the supported installation boundary; there is no fake download.

States: NOT_INSTALLED, INSTALLED (detected; not loaded), LOADING (copy/initialization), READY (engine initialized),
ERROR (copy, incompatible model, unsupported native runtime, inference failure).
Existing weights are detected on restart; initialization and inference run on IO.
A mutex serializes initialization, imports and inference. Conversations are closed
after each answer; one application-scoped engine is reused and closed on replacement
or inference failure. CPU is the explicit baseline backend; no hidden GPU/NPU switch.
Models not supported by the CPU backend/device surface an error and Auto can advance.
Import uses a temporary file and atomic replacement: interrupted/empty copies preserve
existing weights and remove only the app-created partial file. A fully copied but
incompatible model remains installed in ERROR until the user imports a compatible one.

## Verification boundaries
Routing tests use fake runtimes and require no phone/model file.
Model-store tests cover detection/restart, atomic replacement, interrupted and empty
imports. Debug compilation verifies API integration, not inference on a real device.
Physical-device import, compatible Gemma 4 weights, latency/RAM, backgrounding,
native ABI support, and real desktop/cloud connectivity require separate checks.
