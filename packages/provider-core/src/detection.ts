import type { ProviderAdapter, ProviderDetection, ProviderId } from "./index";

export interface DetectionResult {
  providerId: ProviderId | null;
  confidence: ProviderDetection["confidence"];
  reason: string;
}

export async function detectProvider(
  credential: string,
  adapters: ProviderAdapter[],
  explicitProvider?: ProviderId
): Promise<DetectionResult> {
  if (explicitProvider) {
    const adapter = adapters.find(item => item.id === explicitProvider);
    if (!adapter) {
      return {
        providerId: null,
        confidence: "unknown",
        reason: "The selected provider is not installed."
      };
    }
    const result = await adapter.detectCredential({ value: credential, providerHint: explicitProvider });
    return {
      providerId: result.providerId,
      confidence: result.confidence,
      reason: result.reason
    };
  }

  for (const adapter of adapters) {
    const result = await adapter.detectCredential({ value: credential });
    if (result.providerId) {
      return result;
    }
  }

  return {
    providerId: null,
    confidence: "unknown",
    reason: "No installed provider adapter could validate this credential."
  };
}
