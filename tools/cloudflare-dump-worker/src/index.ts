interface Env {
  DUMP_DB: D1Database;
  DUMP_AUTH_TOKEN?: string;
  EDITOR_RELAY: DurableObjectNamespace;
  PAIR_RELAY: DurableObjectNamespace;
}

const EDITOR_SESSION_TTL_MS = 60 * 60 * 1000;
const PAIR_CODE_TTL_MS = 30 * 60 * 1000;

const DEFAULT_RETENTION_DAYS = 14;
const MAX_RETENTION_DAYS = 30;
const MAX_DUMP_BYTES = 2_000_000;

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Dump-Retention-Days",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

const JSON_HEADERS: Record<string, string> = {
  ...CORS_HEADERS,
  "Content-Type": "application/json; charset=utf-8",
  "Cache-Control": "no-store",
};

type DumpRow = {
  id: string;
  created_at: number;
  expires_at: number;
  payload: string;
};

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    const url = new URL(request.url);

    if (url.pathname === "/api/dump/upload") {
      return handleUpload(request, env, ctx);
    }

    if (url.pathname.startsWith("/api/dump/view/")) {
      const id = url.pathname.substring("/api/dump/view/".length).trim();
      return redirectToPublicDump(id);
    }

    if (url.pathname === "/api/dump/view") {
      const id = (url.searchParams.get("id") || "").trim();
      return redirectToPublicDump(id);
    }

    if (url.pathname.startsWith("/api/dump/")) {
      const id = url.pathname.substring("/api/dump/".length).trim();
      return handleFetch(id, env);
    }

    const editorMatch = url.pathname.match(/^\/api\/editor\/([0-9a-f]{32})\/ws$/);
    if (editorMatch) {
      return handleEditorSocket(request, env, editorMatch[1], url.searchParams.get("role"));
    }

    const pairMatch = url.pathname.match(/^\/api\/pair\/([0-9a-f]{64})\/ws$/);
    if (pairMatch) {
      return handlePairSocket(request, env, pairMatch[1], url.searchParams.get("role"));
    }

    if (url.pathname === "/api/health") {
      return json(200, { ok: true, service: "cb-dump-worker", storage: "d1" });
    }

    return json(404, { error: "Not found" });
  },
};

async function handleUpload(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
  if (request.method !== "POST") {
    return json(405, { error: "Method not allowed" });
  }

  const expectedToken = env.DUMP_AUTH_TOKEN || "cb-dump-upload-v1";
  const authorization = request.headers.get("Authorization") || "";
  if (authorization !== `Bearer ${expectedToken}`) {
    return json(401, { error: "Unauthorized" });
  }

  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return json(400, { error: "Invalid JSON body" });
  }

  if (!isObject(payload)) {
    return json(400, { error: "Dump payload must be a JSON object" });
  }

  const payloadText = JSON.stringify(payload);
  const payloadBytes = new TextEncoder().encode(payloadText).byteLength;
  if (payloadBytes > MAX_DUMP_BYTES) {
    return json(413, { error: "Dump payload too large" });
  }

  const retentionDays = parseRetentionDays(request.headers.get("X-Dump-Retention-Days"));
  const nowSec = Math.floor(Date.now() / 1000);
  const expiresAtSec = nowSec + retentionDays * 24 * 60 * 60;
  const id = randomId();

  await env.DUMP_DB.prepare(
    "INSERT INTO dumps (id, created_at, expires_at, payload) VALUES (?1, ?2, ?3, ?4)"
  )
    .bind(id, nowSec, expiresAtSec, payloadText)
    .run();

  ctx.waitUntil(
    env.DUMP_DB.prepare("DELETE FROM dumps WHERE expires_at < ?1")
      .bind(nowSec)
      .run()
      .then(() => undefined)
      .catch(() => undefined)
  );

  return json(201, {
    id,
    url: publicDumpUrl(id),
    createdAt: new Date(nowSec * 1000).toISOString(),
    expiresAt: new Date(expiresAtSec * 1000).toISOString(),
  });
}

async function handleFetch(id: string, env: Env): Promise<Response> {
  if (!isValidDumpId(id)) {
    return json(400, { error: "Invalid dump id" });
  }

  const row = await env.DUMP_DB.prepare(
    "SELECT id, created_at, expires_at, payload FROM dumps WHERE id = ?1"
  )
    .bind(id)
    .first<DumpRow>();

  if (!row) {
    return json(404, { error: "Dump not found or expired" });
  }

  const nowSec = Math.floor(Date.now() / 1000);
  if (row.expires_at < nowSec) {
    await env.DUMP_DB.prepare("DELETE FROM dumps WHERE id = ?1").bind(id).run();
    return json(404, { error: "Dump not found or expired" });
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(row.payload);
  } catch {
    return json(500, { error: "Stored dump payload is invalid" });
  }

  return json(200, {
    id: row.id,
    createdAt: new Date(row.created_at * 1000).toISOString(),
    expiresAt: new Date(row.expires_at * 1000).toISOString(),
    data: parsed,
  });
}

async function handleEditorSocket(
  request: Request,
  env: Env,
  id: string,
  role: string | null
): Promise<Response> {
  if (request.headers.get("Upgrade") !== "websocket") {
    return json(426, { error: "Expected WebSocket upgrade" });
  }
  if (role !== "proxy" && role !== "editor") {
    return json(400, { error: "Invalid role" });
  }
  if (role === "proxy") {
    const expectedToken = env.DUMP_AUTH_TOKEN || "cb-dump-upload-v1";
    if (request.headers.get("Authorization") !== `Bearer ${expectedToken}`) {
      return json(401, { error: "Unauthorized" });
    }
  }
  return env.EDITOR_RELAY.get(env.EDITOR_RELAY.idFromName(id)).fetch(request);
}

/**
 * Relays messages between one proxy and one editor. It never reads them: both
 * ends sign every message with a secret the relay never sees.
 */
export class EditorRelay implements DurableObject {
  constructor(private state: DurableObjectState) {}

  async fetch(request: Request): Promise<Response> {
    const role = new URL(request.url).searchParams.get("role") as "proxy" | "editor";

    // pages are accepted and then closed with a reason instead of refused, so they can say why;
    // browsers also slow down new connections after refused ones
    if (role === "editor" && this.state.getWebSockets("proxy").length === 0) {
      return closedWith(4404, "No proxy is waiting on this link anymore. Run the command on your proxy again for a new one.");
    }
    if (role === "proxy" && this.state.getWebSockets("proxy").length > 0) {
      return json(409, { error: "A proxy is already connected to this session" });
    }
    // a reload or a second tab takes over; the old page is told where it went
    for (const old of this.state.getWebSockets("editor")) {
      safeClose(old, 4001, "Opened in another tab");
    }
    const pair = new WebSocketPair();
    this.state.acceptWebSocket(pair[1], [role]);
    if (role === "proxy") {
      await this.state.storage.setAlarm(Date.now() + EDITOR_SESSION_TTL_MS);
    }
    return new Response(null, { status: 101, webSocket: pair[0] });
  }

  async webSocketMessage(ws: WebSocket, message: string | ArrayBuffer): Promise<void> {
    const other = this.roleOf(ws) === "proxy" ? "editor" : "proxy";
    for (const peer of this.state.getWebSockets(other)) {
      peer.send(message);
    }
  }

  async webSocketClose(ws: WebSocket, code: number, reason: string): Promise<void> {
    if (this.roleOf(ws) === "proxy") {
      this.closeAll(1000, "Proxy closed the session");
    } else {
      safeClose(ws, code, reason);
    }
  }

  async webSocketError(ws: WebSocket): Promise<void> {
    await this.webSocketClose(ws, 1011, "Socket error");
  }

  async alarm(): Promise<void> {
    this.closeAll(1000, "Editor session expired");
  }

  private roleOf(ws: WebSocket): string {
    return this.state.getTags(ws)[0];
  }

  private closeAll(code: number, reason: string): void {
    for (const ws of this.state.getWebSockets()) {
      safeClose(ws, code, reason);
    }
  }
}

async function handlePairSocket(
  request: Request,
  env: Env,
  id: string,
  role: string | null
): Promise<Response> {
  if (request.headers.get("Upgrade") !== "websocket") {
    return json(426, { error: "Expected WebSocket upgrade" });
  }
  if (role !== "proxy" && role !== "server") {
    return json(400, { error: "Invalid role" });
  }
  if (role === "proxy") {
    const expectedToken = env.DUMP_AUTH_TOKEN || "cb-dump-upload-v1";
    if (request.headers.get("Authorization") !== `Bearer ${expectedToken}`) {
      return json(401, { error: "Unauthorized" });
    }
  }
  return env.PAIR_RELAY.get(env.PAIR_RELAY.idFromName(id)).fetch(request);
}

/**
 * Joins the proxy holding a pairing code with the server that runs /cbc pair.
 * The channel is named by a hash of the code and the config is sealed with a
 * key derived from it, so the relay only ever forwards ciphertext.
 */
export class PairRelay implements DurableObject {
  constructor(private state: DurableObjectState) {}

  async fetch(request: Request): Promise<Response> {
    const role = new URL(request.url).searchParams.get("role") as "proxy" | "server";

    if (this.state.getWebSockets(role).length > 0) {
      return json(409, { error: `A ${role} is already on this code` });
    }
    if (role === "server" && this.state.getWebSockets("proxy").length === 0) {
      return json(404, { error: "No setup is waiting for this code" });
    }
    const pair = new WebSocketPair();
    this.state.acceptWebSocket(pair[1], [role]);
    if (role === "proxy") {
      await this.state.storage.setAlarm(Date.now() + PAIR_CODE_TTL_MS);
    }
    return new Response(null, { status: 101, webSocket: pair[0] });
  }

  async webSocketMessage(ws: WebSocket, message: string | ArrayBuffer): Promise<void> {
    const other = this.state.getTags(ws)[0] === "proxy" ? "server" : "proxy";
    for (const peer of this.state.getWebSockets(other)) {
      peer.send(message);
    }
  }

  async webSocketClose(ws: WebSocket, code: number, reason: string): Promise<void> {
    this.closeAll(1000, "The other side left");
  }

  async webSocketError(ws: WebSocket): Promise<void> {
    this.closeAll(1011, "Socket error");
  }

  async alarm(): Promise<void> {
    this.closeAll(1000, "Pairing code expired");
  }

  private closeAll(code: number, reason: string): void {
    for (const ws of this.state.getWebSockets()) {
      safeClose(ws, code, reason);
    }
  }
}

function closedWith(code: number, reason: string): Response {
  const pair = new WebSocketPair();
  pair[1].accept();
  pair[1].close(code, reason);
  return new Response(null, { status: 101, webSocket: pair[0] });
}

function safeClose(ws: WebSocket, code: number, reason: string): void {
  try {
    ws.close(code, reason);
  } catch {
    // already closed
  }
}

function redirectToPublicDump(id: string): Response {
  if (!isValidDumpId(id)) {
    return json(400, { error: "Invalid dump id" });
  }

  return new Response(null, {
    status: 302,
    headers: {
      ...CORS_HEADERS,
      Location: publicDumpUrl(id),
    },
  });
}

function publicDumpUrl(id: string): string {
  return `https://cb.objz.dev/dump/?id=${encodeURIComponent(id)}`;
}

function parseRetentionDays(raw: string | null): number {
  if (!raw) {
    return DEFAULT_RETENTION_DAYS;
  }
  const parsed = Number.parseInt(raw, 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return DEFAULT_RETENTION_DAYS;
  }
  return Math.min(parsed, MAX_RETENTION_DAYS);
}

function randomId(): string {
  return crypto.randomUUID().replace(/-/g, "").slice(0, 16);
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isValidDumpId(id: string): boolean {
  return /^[a-zA-Z0-9_-]{8,64}$/.test(id);
}

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: JSON_HEADERS,
  });
}
