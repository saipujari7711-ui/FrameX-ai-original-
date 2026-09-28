import type { ChatDocument } from "@frame-x/contracts";

const DB_NAME = "frame-x-ai";
const STORE = "chats";
const VERSION = 1;

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE)) {
        const store = db.createObjectStore(STORE, { keyPath: "chatId" });
        store.createIndex("updatedAt", "updatedAt");
        store.createIndex("category", "category");
        store.createIndex("projectId", "projectId");
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function saveChat(chat: ChatDocument): Promise<void> {
  const db = await openDb();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).put(chat);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  db.close();
}

export async function getChat(chatId: string): Promise<ChatDocument | null> {
  const db = await openDb();
  const value = await new Promise<ChatDocument | undefined>((resolve, reject) => {
    const request = db.transaction(STORE).objectStore(STORE).get(chatId);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  db.close();
  return value ?? null;
}

export async function listChats(): Promise<ChatDocument[]> {
  const db = await openDb();
  const values = await new Promise<ChatDocument[]>((resolve, reject) => {
    const request = db.transaction(STORE).objectStore(STORE).getAll();
    request.onsuccess = () => resolve(request.result ?? []);
    request.onerror = () => reject(request.error);
  });
  db.close();
  return values.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
}

export async function deleteChat(chatId: string): Promise<void> {
  const db = await openDb();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).delete(chatId);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  db.close();
}
