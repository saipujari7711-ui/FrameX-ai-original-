# FRAME X AI Implementation Roadmap

## Phase 1 — Architecture / Core foundation
- [x] Repository baseline
- [x] Android Kotlin + Compose project shell
- [x] Web Next.js + TypeScript shell
- [x] Shared versioned contracts
- [x] Provider adapter interface
- [x] Capability tri-state model
- [x] Deterministic task analysis
- [x] Routing candidate selection
- [x] Provider detection orchestration
- [x] Android Keystore credential store
- [x] WebCrypto credential store
- [x] Per-chat local persistence primitives
- [x] Sync conflict primitives
- [x] Google Drive folder/JSON primitives
- [x] Supabase authentication gateway boundary
- [x] CI build definitions
- [ ] Complete provider adapter matrix
- [ ] Complete Drive OAuth/token lifecycle
- [ ] Android network/auth/Drive adapters
- [ ] Web auth callback/session hardening
- [ ] Attachment processing pipeline

## Phase 2 — Product UX
1. Brand assets and typography
2. Navigation and responsive shell
3. Authentication screens
4. Chat/history/search
5. Provider and credential management
6. Model/capability browser
7. Projects/categories
8. Attachment UI
9. Drive/sync UI
10. Offline/conflict UI
11. Voice/image/research surfaces

## Phase 3 — QA / Production
1. Run CI on every change
2. Unit and integration coverage
3. Provider contract tests with mocked HTTP
4. Android instrumentation tests
5. Browser E2E tests
6. OAuth/Drive integration tests
7. Secret scanning and dependency audit
8. Performance profiling
9. Release signing configuration
10. APK/AAB production build
11. Web production deployment verification

## Important release rule

A feature is not marked complete merely because a UI control exists. It must have a real implementation, tests where applicable, error handling, and successful build/integration verification.
