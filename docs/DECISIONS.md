# FRAME X AI — Architecture Decisions

## ADR-001: Native Android + Next.js Web

Android uses Kotlin + Jetpack Compose for lifecycle-safe native storage, background work, secure credential handling, and platform integrations.

Web uses TypeScript + Next.js App Router. Next.js 16.3.x is the current Active LTS line used by this baseline. Node.js 20.9+ is required by Next.js 16.

## ADR-002: Small backend boundary

Supabase Auth is used for FRAME X AI identity/session management. The application does not create a general-purpose application backend.

The Web AI relay is intentionally narrow: it accepts a runtime provider credential, forwards the request to the selected provider adapter, and does not persist the credential. This exists because browser-side AI API keys are secrets and should not be embedded into frontend source/bundles.

Android can call provider APIs directly from the device through platform networking once its secure credential and provider adapters are integrated.

## ADR-003: Local-first

The local client is authoritative for interactive work. Google Drive is synchronization/storage owned by the user.

## ADR-004: Per-chat documents

Each chat is a separate JSON document. Indexes may be maintained locally for search/history, but chats are never consolidated into one giant JSON document.

## ADR-005: Capability uncertainty

Provider model APIs frequently expose model identity without a complete capability matrix. FRAME X AI therefore stores capabilities as supported/unsupported/unknown. Unknown is never promoted to supported by assumption.

## ADR-006: Google Drive scope

The synchronization layer is designed around user-owned My Drive content and the `drive.file` scope where sufficient. Broader Drive scopes are not requested unless a concrete feature requires them.

## ADR-007: Conflict safety

Conflicting revisions are surfaced to the user. The sync engine does not silently pick a winner.

## Current verified upstream references

- Next.js 16.3.6 Active LTS
- Android Gradle Plugin 9.4.0
- Kotlin Gradle Plugin 2.2.10 for the current Android baseline
- Compose BOM 2026.09.00
- Room is intentionally not required yet; Android foundation uses per-chat JSON files to keep the first persistence layer dependency-light.
