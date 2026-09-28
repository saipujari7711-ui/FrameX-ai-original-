import type { ChatDocument, Message } from "@frame-x/contracts";
import type {
  ProviderId,
  ProviderRegistry,
  ProviderRequest,
  ProviderStreamEvent,
  RoutingCandidate,
  RoutingRequest
} from "@frame-x/provider-core";
import { selectRoute } from "@frame-x/provider-core";

export interface CredentialResolver {
  resolve(credentialId: string): Promise<string | null>;
}

export interface ChatRepository {
  save(chat: ChatDocument): Promise<void>;
  get(chatId: string): Promise<ChatDocument | null>;
}

export class ProviderLimitError extends Error {
  constructor(
    message: string,
    readonly alternatives: RoutingCandidate[]
  ) {
    super(message);
    this.name = "ProviderLimitError";
  }
}

export interface SendMessageInput {
  chatId: string;
  text: string;
  routing: RoutingRequest;
  candidates: RoutingCandidate[];
  credentialResolver: CredentialResolver;
  repository: ChatRepository;
}

export async function* streamMessage(
  input: SendMessageInput,
  registry: ProviderRegistry
): AsyncIterable<ProviderStreamEvent> {
  const route = selectRoute(input.routing, input.candidates);
  if (!route) throw new Error("No compatible configured model is available.");

  const adapter = registry.get(route.providerId);
  if (!adapter) throw new Error(`Provider adapter '${route.providerId}' is unavailable.`);

  const credential = await input.credentialResolver.resolve(route.credentialId);
  if (!credential) throw new Error("Selected provider credential is unavailable.");

  const chat = await input.repository.get(input.chatId);
  if (!chat) throw new Error("Chat not found.");

  const message: Message = {
    id: crypto.randomUUID(),
    role: "user",
    content: input.text,
    createdAt: new Date().toISOString(),
    attachments: []
  };

  const request: ProviderRequest = {
    modelId: route.model.modelId,
    messages: [
      ...chat.messages.map(item => ({
        role: item.role,
        content: item.content
      })),
      { role: "user", content: input.text }
    ],
    signal: undefined
  };

  for await (const event of adapter.stream(request, credential)) {
    yield event;
  }

  chat.messages.push(message);
  chat.updatedAt = new Date().toISOString();
  chat.revision += 1;
  chat.modelMetadata = {
    providerId: route.providerId,
    modelId: route.model.modelId,
    taskRouting: input.routing
  };
  await input.repository.save(chat);
}
