interface Env {
  DUMP_DB: D1Database;
  DUMP_AUTH_TOKEN?: string;
}

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
