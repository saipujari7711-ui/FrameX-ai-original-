# FRAME X AI Security Baseline

## Credential rules
AI provider secrets are runtime credentials. They must never be committed, embedded in bundles, placed in URLs, or written to chat documents.

## Logging
Logs use structured redaction. Credential-bearing request/response fields are not logged by default.

## Web
Because browser JavaScript can access browser-held credentials, the web client is treated as a privileged local application:
- strict Content Security Policy
- dependency auditing
- no credential telemetry
- sanitized error boundaries
- safe Markdown rendering

## Android
- Keystore-backed encryption
- no plaintext credential files
- secure backup policy
- exported components minimized
- network security configuration reviewed before release

## Sync
Google Drive stores user data only after explicit authorization. Conflicts are detected rather than silently overwritten.

## Release gate
A production build is not complete until secret scanning, dependency audit, tests, Android/Web builds, and critical auth/provider/sync flows pass.
