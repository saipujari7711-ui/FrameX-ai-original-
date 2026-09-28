import type { ModelDescriptor, CapabilitySet } from "@frame-x/contracts";
import type {
  CredentialCandidate,
  CredentialValidation,
  ProviderAdapter,
  ProviderDetection,
  ProviderRequest,
  ProviderResponse,
  ProviderStreamEvent
} from "@frame-x/provider-core";

const API = "https://api.openai.com/v1";

function headers(key: string): HeadersInit {
  return {
    Authorization: `Bearer ${key}`,
    "Content-Type": "application/json"
  };
}

function textCapabilities(): CapabilitySet {
  return {
    text: "supported",
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

export class OpenAIAdapter implements ProviderAdapter {
  readonly id = "openai" as const;

  async detectCredential(candidate: CredentialCandidate): Promise<ProviderDetection> {
    const result = await this.validateCredential(candidate.value);
    return {
      providerId: result.valid ? this.id : null,
      confidence: result.valid ? "high" : "unknown",
      reason: result.message
    };
  }

  async validateCredential(credential: string): Promise<CredentialValidation> {
    if (!credential.trim()) {
      return { valid: false, providerId: this.id, message: "Credential is empty." };
    }

    try {
      const response = await fetch(`${API}/models`, {
        headers: headers(credential),
        cache: "no-store"
      });
      if (response.ok) {
        return { valid: true, providerId: this.id, message: "OpenAI credential validated." };
      }
      return {
        valid: false,
        providerId: this.id,
        message: `OpenAI rejected the credential (HTTP ${response.status}).`
      };
    } catch {
      return {
        valid: false,
        providerId: this.id,
        message: "OpenAI validation could not reach the provider."
      };
    }
  }

  async listModels(credential: string): Promise<ModelDescriptor[]> {
    const response = await fetch(`${API}/models`, {
      headers: headers(credential),
      cache: "no-store"
    });

    if (!response.ok) {
      throw new Error(`OpenAI model discovery failed (HTTP ${response.status}).`);
    }

    const payload = await response.json() as {
      data?: Array<{ id?: string; owned_by?: string }>;
    };

    return (payload.data ?? [])
      .filter(model => typeof model.id === "string")
      .map(model => ({
        providerId: this.id,
        modelId: model.id!,
        displayName: model.id,
        capabilities: textCapabilities(),
        rawMetadata: { ownedBy: model.owned_by }
      }));
  }

  getCapabilities(model: ModelDescriptor): CapabilitySet {
    return model.capabilities;
  }

  async generate(request: ProviderRequest, credential: string): Promise<ProviderResponse> {
    const response = await fetch(`${API}/responses`, {
      method: "POST",
      headers: headers(credential),
      body: JSON.stringify({
        model: request.modelId,
        input: request.messages,
        temperature: request.temperature,
        max_output_tokens: request.maxOutputTokens,
        tools: request.tools
      }),
      signal: request.signal
    });

    const payload = await response.json() as {
      output_text?: string;
      usage?: { input_tokens?: number; output_tokens?: number; total_tokens?: number };
      error?: { message?: string };
    };

    if (!response.ok) {
      throw new Error(payload.error?.message ?? `OpenAI request failed (HTTP ${response.status}).`);
    }

    return {
      providerId: this.id,
      modelId: request.modelId,
      text: payload.output_text ?? "",
      usage: payload.usage
        ? {
            inputTokens: payload.usage.input_tokens,
            outputTokens: payload.usage.output_tokens,
            totalTokens: payload.usage.total_tokens
          }
        : undefined
    };
  }

  async *stream(request: ProviderRequest, credential: string): AsyncIterable<ProviderStreamEvent> {
    const response = await fetch(`${API}/responses`, {
      method: "POST",
      headers: headers(credential),
      body: JSON.stringify({
        model: request.modelId,
        input: request.messages,
        stream: true,
        temperature: request.temperature,
        max_output_tokens: request.maxOutputTokens,
        tools: request.tools
      }),
      signal: request.signal
    });

    if (!response.ok || !response.body) {
      const body = await response.text().catch(() => "");
      throw new Error(body || `OpenAI streaming request failed (HTTP ${response.status}).`);
    }

    const decoder = new TextDecoder();
    let buffer = "";

    const reader = response.body.getReader();
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() ?? "";

        for (const line of lines) {
          if (!line.startsWith("data:")) continue;
          const raw = line.slice(5).trim();
          if (!raw || raw === "[DONE]") continue;

          let event: {
            type?: string;
            delta?: string;
            response?: {
              usage?: {
                input_tokens?: number;
                output_tokens?: number;
                total_tokens?: number;
              };
            };
          };
          try {
            event = JSON.parse(raw);
          } catch {
            continue;
          }

          if (event.type === "response.output_text.delta" && event.delta) {
            yield { type: "text-delta", text: event.delta };
          } else if (event.type === "response.completed") {
            const usage = event.response?.usage;
            yield {
              type: "completed",
              data: usage ? {
                inputTokens: usage.input_tokens,
                outputTokens: usage.output_tokens,
                totalTokens: usage.total_tokens
              } : undefined
            };
          }
        }
      }
    } finally {
      reader.releaseLock();
    }
  }
}
