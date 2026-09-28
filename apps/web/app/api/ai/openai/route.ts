import { OpenAIAdapter } from "@frame-x/provider-openai";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const adapter = new OpenAIAdapter();

function credential(request: Request): string {
  return request.headers.get("x-frame-x-provider-key") ?? "";
}

export async function POST(request: Request) {
  const key = credential(request);
  if (!key) {
    return Response.json({ error: "Provider credential is required." }, { status: 400 });
  }

  const body = await request.json() as {
    action: "validate" | "models" | "generate";
    modelId?: string;
    messages?: Array<{ role: "system" | "user" | "assistant" | "tool"; content: string }>;
  };

  if (body.action === "validate") {
    const result = await adapter.validateCredential(key);
    return Response.json({
      valid: result.valid,
      providerId: result.providerId,
      message: result.message
    });
  }

  if (body.action === "models") {
    try {
      const models = await adapter.listModels(key);
      return Response.json({ models });
    } catch {
      return Response.json({ error: "Model discovery failed." }, { status: 502 });
    }
  }

  if (!body.modelId || !body.messages) {
    return Response.json({ error: "modelId and messages are required." }, { status: 400 });
  }

  try {
    const result = await adapter.generate(
      { modelId: body.modelId, messages: body.messages },
      key
    );
    return Response.json({ result });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Provider request failed.";
    return Response.json({ error: message }, { status: 502 });
  }
}
