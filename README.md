# FRAME X AI

FRAME X AI is a local-first, user-owned personal AI platform for Android and Web.

## Repository identity

- GitHub repository: `FrameX-ai-original-`
- Product name: **FRAME X AI**
- Default branch: `main`

## Build strategy

This repository is being rebuilt from an empty baseline. No previous FRAME X implementation is reused.

### Phase 1 — Core

- Native Android: Kotlin + Jetpack Compose
- Web: TypeScript + Next.js
- Shared contracts and schemas
- Provider-adapter AI gateway
- Secure credential storage
- Model/capability discovery
- Smart routing
- Versioned per-chat JSON
- Local-first persistence and sync queue
- Google Drive synchronization
- Authentication boundary

### Phase 2 — Product UX

Premium FRAME X AI visual system, responsive navigation, chat, projects, provider management, Drive, attachments, accessibility, and complete states.

### Phase 3 — QA / Production

Build, test, security review, integration verification, performance review, Android packaging, and production readiness.

## Security baseline

Secrets are never committed to source control. API credentials are runtime user data and must be stored using platform-appropriate secure mechanisms. Chat documents never contain API keys, passwords, or OAuth secrets.

## Brand

The supplied FRAME X AI logo is the authoritative brand asset. Derived technical assets must preserve the supplied artwork and identity.


## Android verification
The Android implementation is validated through the repository CI pipeline before release artifacts are considered verified.
