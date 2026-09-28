import { OpenAIAdapter } from "@frame-x/provider-openai";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const adapter = new OpenAIAdapter();

export async function POST(request: Request) {
  const key = request.headers.get("x-frame-x-provider-key") ?? "";
  if (!key) return Response.json({ error: "Provider credential is required." }, { status: 400 });

  const body = await request.json() as {
    modelId?: string;
    messages?: Array<{ role: "system" | "user" | "assistant" | "tool"; content: string }>;
  };

  if (!body.modelId || !body.messages) {
    return Response.json({ error: "modelId and messages are required." }, { status: 400 });
  }

  const encoder = new TextEncoder();

  const stream = new ReadableStream({
    async start(controller) {
      try {
        for await (const event of adapter.stream(
          { modelId: body.modelId!, messages: body.messages! },
          key
        )) {
          controller.enqueue(encoder.encode(`data: ${JSON.stringify(event)}\n\n`));
        }
        controller.enqueue(encoder.encode("data: [DONE]\n\n"));
        controller.close();
      } catch {
        controller.enqueue(encoder.encode(`data: ${JSON.stringify({ type: "error", data: "Provider streaming failed." })}\n\n`));
        controller.close();
      }
    }
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-store, no-cache",
      Connection: "keep-alive",
      "X-Content-Type-Options": "nosniff"
    }
  });
}
