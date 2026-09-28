import type { SyncConflict, SyncRecord } from "@frame-x/contracts";

export type SyncDecision = "keep-local" | "keep-remote" | "keep-both";

export function detectConflict(local: SyncRecord, remote: SyncRecord): SyncConflict | null {
  if (local.entityId !== remote.entityId) return null;
  if (local.localRevision === remote.remoteRevision) return null;
  if (local.updatedAt === remote.updatedAt) return null;

  return {
    conflictId: `conflict:${local.entityType}:${local.entityId}:${local.localRevision}:${remote.remoteRevision}`,
    entityType: local.entityType,
    entityId: local.entityId,
    localRevision: local.localRevision,
    remoteRevision: remote.remoteRevision ?? 0,
    localUpdatedAt: local.updatedAt,
    remoteUpdatedAt: remote.updatedAt,
    createdAt: new Date().toISOString()
  };
}

export function applyDecision(
  conflict: SyncConflict,
  decision: SyncDecision
): "replace-local" | "replace-remote" | "preserve-both" {
  if (decision === "keep-local") return "replace-remote";
  if (decision === "keep-remote") return "replace-local";
  return "preserve-both";
}
