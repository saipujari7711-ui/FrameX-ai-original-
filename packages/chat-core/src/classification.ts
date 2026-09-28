import type { Category } from "@frame-x/contracts";

const rules: Array<[Category, RegExp[]]> = [
  ["Coding", [/\b(code|coding|debug|typescript|javascript|python|kotlin|java|sql|html|css|api|github)\b/i]],
  ["Study", [/\b(study|exam|assignment|homework|learn|lecture|syllabus|chapter|notes)\b/i]],
  ["Research", [/\b(research|sources|citation|literature|investigate|survey|compare)\b/i]],
  ["Planning", [/\b(plan|planning|roadmap|schedule|strategy|checklist|itinerary)\b/i]],
  ["Automation", [/\b(automation|automate|workflow|n8n|zapier|trigger|cron)\b/i]]
];

export function classifyChat(text: string): Category {
  const scores = new Map<Category, number>();
  for (const [category, patterns] of rules) {
    scores.set(category, patterns.reduce((score, pattern) => score + (pattern.test(text) ? 1 : 0), 0));
  }

  const best = [...scores.entries()].sort((a, b) => b[1] - a[1])[0];
  return best && best[1] > 0 ? best[0] : "Other";
}

export function generateChatTitle(text: string): string {
  const clean = text.replace(/\s+/g, " ").trim();
  if (!clean) return "New Chat";
  return clean.length <= 72 ? clean : `${clean.slice(0, 69).trimEnd()}…`;
}
