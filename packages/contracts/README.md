# FRAME X AI Contracts

This package owns versioned, platform-neutral data contracts.

Initial contracts:
- ChatDocument
- Message
- AttachmentReference
- Project
- Category
- ProviderCredentialMetadata
- ModelDescriptor
- CapabilitySet
- SyncRecord
- SyncConflict

Rules:
- IDs are stable opaque strings.
- Timestamps use ISO-8601 UTC.
- Schemas are versioned.
- Secrets are prohibited from these contracts.
