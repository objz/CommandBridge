// Checks the /api/pair relay channel against `npx wrangler dev --port 8787`.
// Run: node tools/cloudflare-dump-worker/test/pair-relay.mjs [base]
import { randomBytes } from "node:crypto";
import http from "node:http";

const base = process.argv[2] || "http://localhost:8787";
const wsBase = base.replace(/^http/, "ws");
const auth = { Authorization: "Bearer cb-dump-upload-v1" };
let failed = 0;

const channel = () => randomBytes(32).toString("hex");
const path = (id, role) => `/api/pair/${id}/ws?role=${role}`;

function open(id, role) {
  const ws = new WebSocket(wsBase + path(id, role), role === "proxy" ? { headers: auth } : undefined);
  return new Promise((resolve, reject) => {
    ws.addEventListener("open", () => resolve(ws), { once: true });
    ws.addEventListener("error", () => reject(new Error(`${role} socket failed`)), { once: true });
  });
}

function next(ws) {
  return new Promise((resolve) => ws.addEventListener("message", (e) => resolve(String(e.data)), { once: true }));
}

// fetch() refuses an Upgrade header, so the refusals are read with a raw request.
function upgrade(id, role, headers = {}) {
  return new Promise((resolve, reject) => {
    const req = http.get(base + path(id, role), {
      headers: { Upgrade: "websocket", Connection: "Upgrade", "Sec-WebSocket-Version": "13", "Sec-WebSocket-Key": randomBytes(16).toString("base64"), ...headers },
    });
    req.on("upgrade", (res, socket) => {
      socket.destroy();
      resolve({ status: 101, json: async () => ({}) });
    });
    req.on("response", (res) => {
      let body = "";
      res.on("data", (c) => (body += c));
      res.on("end", () => resolve({ status: res.statusCode, json: async () => JSON.parse(body) }));
    });
    req.on("error", reject);
  });
}

async function check(name, fn) {
  try {
    await fn();
    console.log(`PASS ${name}`);
  } catch (e) {
    failed++;
    console.log(`FAIL ${name}: ${e.message}`);
  }
}

function expect(cond, msg) {
  if (!cond) throw new Error(msg);
}

await check("server without a proxy is refused", async () => {
  const id = channel();
  const res = await upgrade(id, "server");
  expect(res.status === 404, `status ${res.status}`);
  const body = await res.json();
  expect(body.error === "No setup is waiting for this code", JSON.stringify(body));
  let opened = true;
  await open(id, "server").catch(() => (opened = false));
  expect(!opened, "socket opened");
});

await check("messages pass both ways", async () => {
  const id = channel();
  const proxy = await open(id, "proxy");
  const server = await open(id, "server");
  const atProxy = next(proxy);
  server.send("a");
  expect((await atProxy) === "a", "proxy did not get a");
  const atServer = next(server);
  proxy.send("b");
  expect((await atServer) === "b", "server did not get b");
  proxy.close();
  server.close();
});

await check("a second server is refused", async () => {
  const id = channel();
  const proxy = await open(id, "proxy");
  const server = await open(id, "server");
  const res = await upgrade(id, "server");
  expect(res.status === 409, `status ${res.status}`);
  proxy.close();
  server.close();
});

await check("a proxy without the token is refused", async () => {
  const res = await upgrade(channel(), "proxy", {});
  expect(res.status === 401, `status ${res.status}`);
});

process.exit(failed ? 1 : 0);
