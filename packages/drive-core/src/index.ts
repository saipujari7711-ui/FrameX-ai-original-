const DRIVE = "https://www.googleapis.com/drive/v3";
const UPLOAD = "https://www.googleapis.com/upload/drive/v3/files";

export const FRAME_X_ROOT = "FrameX AI";
export const FRAME_X_FOLDERS = [
  "All Chats",
  "projects",
  "Attachments",
  "Study",
  "Research",
  "Coding",
  "Planning",
  "Automation",
  "Other"
] as const;

type DriveFile = {
  id?: string;
  name?: string;
  mimeType?: string;
  parents?: string[];
  appProperties?: Record<string, string>;
  modifiedTime?: string;
  size?: string;
  md5Checksum?: string;
};

function auth(token: string): HeadersInit {
  return { Authorization: `Bearer ${token}` };
}

async function listChildren(token: string, parentId: string, name: string): Promise<DriveFile | null> {
  const q = encodeURIComponent(
    `'${parentId}' in parents and name = '${name.replace(/'/g, "\\'")}' and trashed = false`
  );
  const response = await fetch(`${DRIVE}/files?q=${q}&fields=files(id,name,mimeType,parents,appProperties,modifiedTime,size,md5Checksum)&pageSize=10`, {
    headers: auth(token),
    cache: "no-store"
  });
  if (!response.ok) throw new Error(`Drive list failed (HTTP ${response.status}).`);
  const payload = await response.json() as { files?: DriveFile[] };
  return payload.files?.[0] ?? null;
}

async function createFolder(token: string, name: string, parentId?: string): Promise<string> {
  const response = await fetch(`${DRIVE}/files?fields=id,name,mimeType,parents`, {
    method: "POST",
    headers: { ...auth(token), "Content-Type": "application/json" },
    body: JSON.stringify({
      name,
      mimeType: "application/vnd.google-apps.folder",
      ...(parentId ? { parents: [parentId] } : {})
    })
  });
  if (!response.ok) throw new Error(`Drive folder creation failed (HTTP ${response.status}).`);
  const file = await response.json() as DriveFile;
  if (!file.id) throw new Error("Drive did not return a folder ID.");
  return file.id;
}

export async function ensureFrameXStructure(accessToken: string): Promise<Record<string, string>> {
  let root = await listChildren(accessToken, "root", FRAME_X_ROOT);
  const rootId = root?.id ?? await createFolder(accessToken, FRAME_X_ROOT);

  const ids: Record<string, string> = { [FRAME_X_ROOT]: rootId };
  for (const name of FRAME_X_FOLDERS) {
    const existing = await listChildren(accessToken, rootId, name);
    ids[name] = existing?.id ?? await createFolder(accessToken, name, rootId);
  }
  return ids;
}

export async function uploadJson(
  accessToken: string,
  parentId: string,
  filename: string,
  json: string,
  appProperties: Record<string, string> = {}
): Promise<DriveFile> {
  const boundary = `framex-${crypto.randomUUID()}`;
  const metadata = JSON.stringify({
    name: filename,
    mimeType: "application/json",
    parents: [parentId],
    appProperties
  });

  const body = [
    `--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${metadata}\r\n`,
    `--${boundary}\r\nContent-Type: application/json\r\n\r\n${json}\r\n`,
    `--${boundary}--`
  ].join("");

  const response = await fetch(`${UPLOAD}?uploadType=multipart&fields=id,name,mimeType,parents,appProperties,modifiedTime,md5Checksum`, {
    method: "POST",
    headers: {
      ...auth(accessToken),
      "Content-Type": `multipart/related; boundary=${boundary}`
    },
    body
  });

  if (!response.ok) throw new Error(`Drive JSON upload failed (HTTP ${response.status}).`);
  return await response.json() as DriveFile;
}

export async function downloadText(accessToken: string, fileId: string): Promise<string> {
  const response = await fetch(`${DRIVE}/files/${encodeURIComponent(fileId)}?alt=media`, {
    headers: auth(accessToken),
    cache: "no-store"
  });
  if (!response.ok) throw new Error(`Drive download failed (HTTP ${response.status}).`);
  return response.text();
}
