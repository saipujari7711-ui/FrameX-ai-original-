# FRAME X AI — Architecture Baseline

Status: Phase 1 / architecture baseline

## Product boundaries
- Android: Kotlin + Jetpack Compose
- Web: TypeScript + Next.js
- Shared concepts use versioned contracts rather than shared UI code.

## Repository layout
apps/
  android/
  web/
packages/
  contracts/
  routing/
  provider-core/
docs/
assets/
  brand/

## Local-first data flow
UI → application services → local repository → sync queue → Google Drive adapter

Google Drive is user-owned synchronization/storage, not the runtime database. Each chat is a separate versioned JSON document.

## AI gateway
User request → task/capability analysis → routing policy → provider adapter → normalized stream → persistence

Provider-specific request formats never leak into UI code.

Capabilities use:
- supported
- unsupported
- unknown

Unknown is never treated as supported.

## Credentials
Android uses Android Keystore-backed secure storage.

Web credentials remain user-controlled runtime data and are never bundled or sent to the FRAME X AI application backend. WebCrypto at-rest protection, strict CSP, and dependency controls are mandatory.

The backend, if used, never receives raw AI provider API keys.

## Authentication
Use Supabase Auth for FRAME X AI account identity and sessions. Google Drive authorization remains a separate OAuth capability with minimum required Drive scopes.

## Sync conflicts
Synced documents carry stable ID, schema version, content revision, timestamp, source metadata, and checksum. Divergent edits are surfaced as conflicts; the sync engine never silently overwrites one side.

## Implementation order
1. Contracts and persistence schemas
2. Provider-core interfaces
3. One provider adapter end-to-end
4. Credential storage abstraction
5. Chat engine + streaming
6. Model discovery/capabilities
7. Routing engine
8. Attachments
9. Projects/categories
10. Drive sync
11. Authentication
12. Android/Web integration
13. UI/UX
14. QA and production packaging
