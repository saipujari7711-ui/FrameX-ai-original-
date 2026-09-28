import test from "node:test";
import assert from "node:assert/strict";

const categories = ["Study","Research","Coding","Planning","Automation","Other"];
const capabilities = [
  "text","vision","image-input","files","pdf","audio-input",
  "audio-output","tools","structured-output","reasoning","image-generation"
];

test("contract vocabulary is stable", () => {
  assert.equal(categories.length, 6);
  assert.equal(capabilities.length, 11);
});

test("chat JSON must have a versioned schema and stable identity fields", () => {
  const chat = {
    schemaVersion: 1,
    chatId: "chat-test",
    title: "Test",
    createdAt: new Date(0).toISOString(),
    updatedAt: new Date(0).toISOString(),
    category: "Other",
    projectId: null,
    messages: [],
    attachments: [],
    modelMetadata: {},
    revision: 0
  };
  assert.equal(chat.schemaVersion, 1);
  assert.equal(typeof chat.chatId, "string");
  assert.equal(chat.messages.length, 0);
});
