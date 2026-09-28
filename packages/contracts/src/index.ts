export const SCHEMA_VERSION = 1 as const;

export type Category =
  | "Study"
  | "Research"
  | "Coding"
  | "Planning"
  | "Automation"
  | "Other";

export type Capability =
  | "text"
  | "vision"
  | "image-input"
  | "files"
  | "pdf"
  | "audio-input"
  | "audio-output"
  | "tools"
  | "structured-output"
  | "reasoning"
  | "image-generation";

export type CapabilityState = "supported" | "unsupported" | "unknown";

export type CapabilitySet = Record<Capability, CapabilityState>;

export interface Message {
  id: string;
  role: "system" | "user" | "assistant" | "tool";
  content: string;
  createdAt: string;
  attachments: AttachmentReference[];
  modelMetadata?: ModelMetadata;
}

export interface AttachmentReference {
  id: string;
  filename: string;
  mimeType: string;
  sizeBytes: number;
  checksum?: string;
  driveFileId?: string;
}

export interface ModelMetadata {
  providerId: string;
  modelId: string;
  displayName?: string;
  capabilities: CapabilitySet;
}

export interface ChatDocument {
  schemaVersion: typeof SCHEMA_VERSION;
  chatId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  category: Category;
  projectId: string | null;
  messages: Message[];
  attachments: AttachmentReference[];
  modelMetadata: Record<string, unknown>;
  revision: number;
  checksum?: string;
}

export interface Project {
  schemaVersion: typeof SCHEMA_VERSION;
  projectId: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface ModelDescriptor {
  providerId: string;
  modelId: string;
  displayName?: string;
  contextLength?: number;
  capabilities: CapabilitySet;
  rawMetadata?: Record<string, unknown>;
}

export interface ProviderCredentialMetadata {
  credentialId: string;
  providerId: string;
  label: string;
  maskedValue: string;
  enabled: boolean;
  createdAt: string;
  lastValidatedAt?: string;
}

export interface SyncRecord {
  recordId: string;
  entityType: "chat" | "project" | "attachment" | "category";
  entityId: string;
  localRevision: number;
  remoteRevision?: number;
  state: "pending" | "synced" | "conflict" | "failed";
  updatedAt: string;
}

export interface SyncConflict {
  conflictId: string;
  entityType: SyncRecord["entityType"];
  entityId: string;
  localRevision: number;
  remoteRevision: number;
  localUpdatedAt: string;
  remoteUpdatedAt: string;
  createdAt: string;
}

export function createEmptyCapabilities(): CapabilitySet {
  return {
    text: "unknown",
    vision: "unknown",
    "image-input": "unknown",
    files: "unknown",
    pdf: "unknown",
    "audio-input": "unknown",
    "audio-output": "unknown",
    tools: "unknown",
    "structured-output": "unknown",
    reasoning: "unknown",
    "image-generation": "unknown"
  };
}
