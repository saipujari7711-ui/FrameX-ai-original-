# FRAME X AI — Production Forensic Audit

Date: 2026-09-28
Repository: saipujari7711-ui/FrameX-ai-original-
Audit scope: Android + Web source, CI, provider core, chat persistence, Drive primitives, synchronization primitives, credential storage, UI state integrity.

## Executive status

**NOT PRODUCTION READY.**

The repository currently contains a real architectural foundation and a substantial UI shell, but it does not yet contain the complete production feature set described by the product brief. The audit therefore treats unimplemented flows as release blockers instead of masking them as successful UI states.

## 1. Build status

### Android
- **PASS — last verified CI build:** commit 5c501306685c93b6b611b66c066f03442a8a3c34.
- GitHub Actions completed gradle testDebugUnitTest assembleDebug --no-daemon successfully.
- This verifies compilation, unit-test execution and debug APK assembly in CI.
- It does **not** verify installation, emulator/device launch, runtime navigation, Logcat, network behavior, or release signing.

### Web
- **FAIL — latest completed verification before this audit.**
- Root cause: GitHub Actions configured pnpm cache while the repository had no pnpm-lock.yaml; actions/setup-node failed before dependency installation.
- Fixed in CI by removing the lockfile-dependent pnpm cache and moving the workflow to Node 24.
- A new run for the latest UI commit is currently in progress; its result is not yet verified.

## 2. Critical bugs

### P0 — Web authentication is not implemented
**Affected:** login, registration, Google Sign-In, protected workspace.

**Root cause:** the repository has presentation-only authentication screens. The planned auth gateway file is absent from the current repository tree, and the Google button only displays a status message.

**Impact:** users cannot establish a real authenticated session from these screens.

**Status:** NOT FIXED in this pass because wiring authentication without a configured auth backend/session boundary would create another fake implementation. Production implementation must use the existing architecture decision for Supabase/session handling and protect server AI routes.

### P0 — Web AI chat does not execute provider requests
**Affected:** send, streaming, retry, regenerate, stop generation.

**Root cause:** the visible chat composer only stages a UI notice. It does not call the existing OpenAI route.

**Impact:** the central chat experience is not functional from the Web UI.

**Status:** NOT FIXED yet. The secure path requires authenticated request authorization, credential ownership, request cancellation and rate limiting before exposing the provider relay to production traffic.

### P0 — AI relay is not production-secure
**Affected:** /api/ai/openai and /api/ai/openai/stream.

**Root cause:** the route accepts a provider credential from the x-frame-x-provider-key request header without an authenticated user/session boundary, rate limiting, or production request authorization.

**Impact:** an unauthenticated caller could potentially use the relay as an API proxy if deployed with network access.

**Required fix:** bind the route to an authenticated session, retrieve credentials through the server-side credential boundary, enforce origin/CSRF policy where applicable, add abuse/rate controls, and never log credential material.

### P0 — Google Drive is not an end-to-end sync system
**Affected:** OAuth, first connection, token refresh/revocation, upload/update/delete, offline queue, conflict UI.

**Root cause:** only low-level Drive folder/list/create/upload/download primitives exist. There is no complete OAuth lifecycle or synchronization service.

**Impact:** the Drive screen cannot truthfully provide production synchronization.

### P0 — Provider matrix is incomplete
**Affected:** Gemini, Groq, Anthropic, OpenRouter, Mistral, DeepSeek, xAI, Together, Cohere.

**Root cause:** provider IDs are declared in the shared contract, but only an OpenAI adapter is present in the repository tree.

**Impact:** multi-provider routing cannot currently be considered production-complete.

### P0 — Android is currently a UI shell
**Affected:** real login, provider requests, streaming, attachments, Drive, sync, offline queue, projects, voice and image generation.

**Root cause:** MainActivity contains presentation surfaces and local UI state but no Android networking/auth/Drive integration for these flows.

**Impact:** Android cannot yet be treated as a production client.

## 3. Functional bugs

### P1 — Streamed assistant response was not persisted
**Root cause:** chat-core previously appended only the user message after the provider stream completed. The assistant output was never persisted.

**Fix:** packages/chat-core/src/index.ts now creates stable user and assistant message IDs, persists both before streaming, appends streamed assistant deltas, persists progress, and rolls back the incomplete assistant message when streaming fails.

**Verification:** source-level fix committed. CI verification for the newest commit is still pending.

### P1 — UI displayed fabricated connection state
**Affected:** Android/Web home, Drive, provider surfaces.

**Root cause:** UI constants reported values such as connected providers, chat counts and Drive synchronization without reading live state.

**Fix:** replaced fabricated connection/model/sync values with explicit unconfigured/not-connected states.

**Verification:** committed; latest CI still pending.

### P1 — Model capability discovery is too optimistic
**Affected:** routing.

**Root cause:** the OpenAI adapter currently gives every discovered model a text-supported capability set. The Models API does not itself provide a complete capability matrix for every model.

**Impact:** routing can classify a model as compatible without sufficient provider-specific capability evidence.

**Required fix:** provider-specific capability registry/provenance with supported | unsupported | unknown; unknown must remain ineligible for capability-gated routing.

### P1 — Drive JSON upload creates new files rather than updating an existing chat
**Affected:** synchronization and duplicate prevention.

**Root cause:** uploadJson always calls Drive file creation. There is no update/upsert path keyed by stable remote file ID.

**Impact:** repeated chat synchronization can create uncontrolled remote revisions/files.

**Required fix:** maintain stable remote file IDs and use Drive update semantics for existing records; create only when no remote record exists.

### P1 — Conflict model is only a primitive
**Affected:** Android/Web cross-device conflict handling.

**Root cause:** sync-core can detect/apply an abstract conflict decision, but there is no live two-device synchronization workflow or user-facing conflict resolution screen wired to it.

**Impact:** the requested Android/Web same-chat conflict test cannot currently be executed end-to-end.

## 4. Security findings

### No credential was found in the source searches performed
Searches were performed against the repository for common credential indicators including API-key/password/token patterns. No matching credential material was returned.

This is **not equivalent to proving the entire Git history is secret-free**.

### Credential storage
Android uses Android Keystore-backed AES-GCM encryption for locally stored credentials. No custom cryptographic primitive was introduced.

### Remaining security blockers
- Web AI relay authentication
- request authorization/rate limiting
- secure server-side credential resolution
- production OAuth/session handling
- Google Drive token lifecycle
- dependency vulnerability audit after lockfile creation
- release artifact secret scanning
- complete CSP/security-header review

## 5. Performance findings

### High — streamed chat persistence
Persisting every text delta can become expensive for long responses.

Required optimization: batch/debounce persistence while retaining a durable initial user message and final assistant state, with cancellation-safe flushes.

### Medium — Android navigation state
The current UI keeps most navigation and screen state in MainActivity. As real screens are added this will increase recomposition/state ownership pressure.

Required direction: move navigation and screen state into scoped ViewModels/state holders.

### Medium — large attachments
No production attachment pipeline currently exists, so large PDF/image memory behavior cannot yet be validated.

### Medium — Web
The main page is a large client component containing most screens. This will limit server rendering and increase client JavaScript as functionality grows.

Required direction: split route/screen components and keep interactive state local.

## 6. Data / synchronization findings

| Area | Status |
|---|---|
| Stable chat IDs | Implemented in contracts/storage primitives |
| Per-chat JSON | Implemented on Android |
| Schema version | Implemented |
| Revision field | Implemented |
| Conflict primitive | Implemented |
| Remote Drive IDs | Attachment contract supports them |
| End-to-end sync queue | Missing |
| Remote update/upsert | Missing |
| Offline retry | Missing |
| Conflict UI | Missing |
| Android/Web live conflict test | Not executable yet |

## 7. UI/UX findings

The visual system is structurally intact: deep navy environment, cyan interaction accent, gold premium accent, green-gold success semantic, wine/crimson task mode, editorial/display hierarchy, responsive shell, and reduced-motion CSS support.

The main production UI problem found was **state honesty**, not styling. Hardcoded connected states were removed so the interface does not claim capabilities that the underlying services have not actually established.

The official binary logo is still not verifiably present in the Git repository. The code references /brand/framex-logo-master.png, but the GitHub connector available to this audit cannot upload binary assets. This must be resolved before final visual release.

## 8. Files changed during this audit

- .github/workflows/ci.yml
- package.json
- packages/chat-core/src/index.ts
- apps/android/app/src/main/java/ai/framex/app/MainActivity.kt
- apps/web/app/page.tsx

## 9. Tests executed / evidence

### Actually verified
- GitHub Actions Android unit-test + debug APK build: **PASS** at commit 5c501306.
- CI diagnosis of the Web failure: **confirmed** from GitHub Actions logs.
- Source inspection of provider core, chat core, Drive core, sync core, credential storage, Android UI and Web UI.
- Repository source credential-pattern searches: no matching credential material returned.

### Not executed
- Android APK installation on a physical device
- Android emulator launch
- Android Logcat runtime session
- Android instrumentation tests
- real login
- real Google Sign-In
- real provider request from Android
- real streaming from Android
- attachment/PDF/image testing
- Google Drive OAuth
- real synchronization
- offline queue
- two-device conflict test
- provider outage/rate-limit tests against live providers
- Web browser functional/E2E test
- Web production server launch
- memory profiler
- network profiler
- large-file performance test
- release APK/AAB signing and installation

## 10. Final verification

**Release decision: BLOCKED.**

The project has a compilable Android foundation and a substantial premium UI foundation, but it is not yet a completed production application.

The correct next sequence is:
1. Get the latest CI run green.
2. Add a reproducible pnpm-lock.yaml.
3. Implement authenticated Web session handling.
4. Secure the AI relay.
5. Implement real credential management.
6. Complete provider adapters.
7. Connect Web chat to the provider gateway.
8. Implement Android networking/auth/chat.
9. Complete Google Drive OAuth + sync queue + update/upsert.
10. Implement attachment pipeline.
11. Implement conflict UI.
12. Add Android instrumentation + Web E2E.
13. Upload and install a real APK/AAB.
14. Perform Logcat/runtime/network/memory testing.
15. Only then issue a production-release verdict.

**No APK installation, device launch, live provider call, OAuth flow, Drive synchronization or two-device conflict test has been claimed as passed because none was actually executed during this audit.**