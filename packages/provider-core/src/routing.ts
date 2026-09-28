import type { Capability } from "@frame-x/contracts";
import type { RoutingRequest } from "./index";

export type TaskType =
  | "general"
  | "coding"
  | "mathematics"
  | "reasoning"
  | "research"
  | "summarization"
  | "document-analysis"
  | "image-analysis"
  | "image-generation"
  | "voice"
  | "tool-use";

export interface TaskAnalysis {
  task: TaskType;
  requiredCapabilities: Capability[];
  confidence: number;
}

export function analyzeTask(text: string, attachmentMimeTypes: string[] = []): TaskAnalysis {
  const value = text.toLowerCase();
  const hasPdf = attachmentMimeTypes.some(type => type === "application/pdf");
  const hasImage = attachmentMimeTypes.some(type => type.startsWith("image/"));

  if (/(generate|create|draw|make).*(image|picture|illustration|logo|poster)/i.test(value)) {
    return { task: "image-generation", requiredCapabilities: ["image-generation"], confidence: 0.95 };
  }
  if (hasImage || /(analy[sz]e|describe|read).*(image|photo|picture|screenshot)/i.test(value)) {
    return { task: "image-analysis", requiredCapabilities: ["image-input"], confidence: hasImage ? 0.95 : 0.8 };
  }
  if (hasPdf || /(pdf|document|contract|report|paper)/i.test(value)) {
    return { task: "document-analysis", requiredCapabilities: ["files"], confidence: hasPdf ? 0.95 : 0.65 };
  }
  if (/(function call|tool call|tool use|use tools|call a tool)/i.test(value)) {
    return { task: "tool-use", requiredCapabilities: ["tools"], confidence: 0.9 };
  }
  if (/(voice|audio|speak|transcribe|transcription)/i.test(value)) {
    return { task: "voice", requiredCapabilities: ["audio-input"], confidence: 0.85 };
  }
  if (/(summari[sz]e|tl;dr|key points|shorten)/i.test(value)) {
    return { task: "summarization", requiredCapabilities: ["text"], confidence: 0.9 };
  }
  if (/(prove|derive|solve|equation|calculate|integral|matrix|probability)/i.test(value)) {
    return { task: "mathematics", requiredCapabilities: ["text"], confidence: 0.85 };
  }
  if (/(debug|implement|program|code|typescript|javascript|kotlin|python|java|sql|html|css)/i.test(value)) {
    return { task: "coding", requiredCapabilities: ["text"], confidence: 0.9 };
  }
  if (/(why|compare|reason|analy[sz]e|evaluate|tradeoff)/i.test(value)) {
    return { task: "reasoning", requiredCapabilities: ["text"], confidence: 0.7 };
  }
  if (/(research|sources|citations|latest|current|investigate)/i.test(value)) {
    return { task: "research", requiredCapabilities: ["text"], confidence: 0.75 };
  }

  return { task: "general", requiredCapabilities: ["text"], confidence: 0.5 };
}

export function buildRoutingRequest(text: string, attachmentMimeTypes: string[] = []): RoutingRequest {
  const analysis = analyzeTask(text, attachmentMimeTypes);
  return {
    text,
    requestedCapabilities: analysis.requiredCapabilities
  };
}
