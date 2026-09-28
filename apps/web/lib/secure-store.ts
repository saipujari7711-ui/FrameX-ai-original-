const DB = "frame-x-ai-security";
const STORE = "keys";
const KEY_ID = "credential-key";

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function loadOrCreateKey(): Promise<CryptoKey> {
  const db = await openDb();
  const existing = await new Promise<CryptoKey | undefined>((resolve, reject) => {
    const request = db.transaction(STORE).objectStore(STORE).get(KEY_ID);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  if (existing) {
    db.close();
    return existing;
  }

  const key = await crypto.subtle.generateKey(
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"]
  );

  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).put(key, KEY_ID);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  db.close();
  return key;
}

export async function securePut(id: string, value: string): Promise<void> {
  const key = await loadOrCreateKey();
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encoded = new TextEncoder().encode(value);
  const ivBuffer = iv.buffer.slice(iv.byteOffset, iv.byteOffset + iv.byteLength) as ArrayBuffer;
  const ciphertext = await crypto.subtle.encrypt({ name: "AES-GCM", iv: ivBuffer }, key, encoded);

  const db = await openDb();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).put({ iv, ciphertext }, id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  db.close();
}

export async function secureGet(id: string): Promise<string | null> {
  const key = await loadOrCreateKey();
  const db = await openDb();
  const record = await new Promise<{ iv: Uint8Array; ciphertext: ArrayBuffer } | undefined>((resolve, reject) => {
    const request = db.transaction(STORE).objectStore(STORE).get(id);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  db.close();
  if (!record) return null;

  const iv = record.iv.buffer.slice(record.iv.byteOffset, record.iv.byteOffset + record.iv.byteLength) as ArrayBuffer;
  const plaintext = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv },
    key,
    record.ciphertext
  );
  return new TextDecoder().decode(plaintext);
}

export async function secureRemove(id: string): Promise<void> {
  const db = await openDb();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
  db.close();
}
