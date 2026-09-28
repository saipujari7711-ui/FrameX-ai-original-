import type {
  Capability,
  CapabilitySet,
  ModelDescriptor
} from "@frame-x/contracts";

export type ProviderId =
  | "openai"
  | "google-gemini"
  | "groq"
  | "anthropic"
  | "openrouter"
  | "mistral"
  | "deepseek"
  | "xai"
  | "together"
  | "cohere"
  | string;

export interface ProviderDetection {
  providerId: ProviderId | null;
  confidence: "high" | "medium" | "low" | "unknown";
  reason: string;
}

export interface CredentialCandidate {
  value: string;
  providerHint?: ProviderId;
}

export interface CredentialValidation {
  valid: boolean;
  providerId: ProviderId;
  message: string;
}

export interface ProviderRequest {
  modelId: string;
  messages: Array<{
    role: "system" | "user" | "assistant" | "tool";
    content: string;
  }>;
  temperature?: number;
  maxOutputTokens?: number;
  tools?: unknown[];
  attachments?: unknown[];
  signal?: AbortSignal;
}

export interface ProviderResponse {
  modelId: string;
  providerId: ProviderId;
  text: string;
  usage?: {
    inputTokens?: number;
    outputTokens?: number;
    totalTokens?: number;
  };
}

export interface ProviderStreamEvent {
  type: "text-delta" | "tool-call" | "usage" | "completed" | "error";
  text?: string;
  data?: unknown;
}

export interface ProviderAdapter {
  readonly id: ProviderId;
  detectCredential(candidate: CredentialCandidate): Promise<ProviderDetection>;
  validateCredential(credential: string): Promise<CredentialValidation>;
  listModels(credential: string): Promise<ModelDescriptor[]>;
  getCapabilities(model: ModelDescriptor): CapabilitySet;
  generate(request: ProviderRequest, credential: string): Promise<ProviderResponse>;
  stream(request: ProviderRequest, credential: string): AsyncIterable<ProviderStreamEvent>;
}

export interface ProviderRegistry {
  register(adapter: ProviderAdapter): void;
  get(id: ProviderId): ProviderAdapter | undefined;
  list(): ProviderAdapter[];
}

export class InMemoryProviderRegistry implements ProviderRegistry {
  private readonly adapters = new Map<string, ProviderAdapter>();

  register(adapter: ProviderAdapter): void {
    this.adapters.set(adapter.id, adapter);
  }

  get(id: ProviderId): ProviderAdapter | undefined {
    return this.adapters.get(id);
  }

  list(): ProviderAdapter[] {
    return [...this.adapters.values()];
  }
}

export interface RoutingRequest {
  text: string;
  requestedCapabilities?: Capability[];
  preferredProviders?: ProviderId[];
  excludedProviders?: ProviderId[];
  preferredModels?: string[];
}

export interface RoutingCandidate {
  providerId: ProviderId;
  model: ModelDescriptor;
  credentialId: string;
  score: number;
  reasons: string[];
}

export function selectRoute(
  request: RoutingRequest,
  candidates: RoutingCandidate[]
): RoutingCandidate | null {
  const required = new Set<Capability>(request.requestedCapabilities ?? ["text"]);

  const eligible = candidates
    .filter(c => !request.excludedProviders?.includes(c.providerId))
    .filter(c => [...required].every(cap => c.model.capabilities[cap] === "supported"))
    .map(c => {
      let score = c.score;
      if (request.preferredProviders?.includes(c.providerId)) score += 100;
      if (request.preferredModels?.includes(c.model.modelId)) score += 50;
      return { ...c, score };
    })
    .sort((a, b) => b.score - a.score);

  return eligible[0] ?? null;
}
