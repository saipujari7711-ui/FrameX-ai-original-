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
  constructor(message: string, readonly alternatives: RoutingCandidate[]) {
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

function now(): string {
  return new Date().toISOString();
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

  const userMessage: Message = {
    id: crypto.randomUUID(),
    role: "user",
    content: input.text,
    createdAt: now(),
    attachments: []
  };
  const assistantMessage: Message = {
    id: crypto.randomUUID(),
    role: "assistant",
    content: "",
    createdAt: now(),
    attachments: [],
    modelMetadata: route.model
  };

  chat.messages.push(userMessage, assistantMessage);
  chat.updatedAt = now();
  chat.revision += 1;
  chat.modelMetadata = {
    providerId: route.providerId,
    modelId: route.model.modelId,
    taskRouting: input.routing
  };
  await input.repository.save(chat);

  const request: ProviderRequest = {
    modelId: route.model.modelId,
    messages: chat.messages
      .slice(0, -1)
      .map(item => ({ role: item.role, content: item.content })),
    signal: undefined
  };

  try {
    for await (const event of adapter.stream(request, credential)) {
      if (event.type === "text-delta" && event.text) {
        assistantMessage.content += event.text;
        chat.updatedAt = now();
        chat.revision += 1;
        await input.repository.save(chat);
      }

      yield event;
    }

    await input.repository.save(chat);
  } catch (error) {
    const index = chat.messages.findIndex(message => message.id === assistantMessage.id);
    if (index >= 0) chat.messages.splice(index, 1);
    chat.updatedAt = now();
    chat.revision += 1;
    await input.repository.save(chat);
    throw error;
  }
}
