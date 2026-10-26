// Checks how the /api/editor relay treats pages, against `npx wrangler dev --port 8787`.
// Run: node tools/cloudflare-dump-worker/test/editor-relay.mjs [base]
import { randomBytes } from "node:crypto";

const base = (process.argv[2] || "http://localhost:8787").replace(/^http/, "ws");
const auth = { Authorization: "Bearer cb-dump-upload-v1" };
let failed = 0;
const session = () => randomBytes(16).toString("hex");
const url = (id, role) => `${base}/api/editor/${id}/ws?role=${role}`;
const open = (id, role) => {
  const ws = new WebSocket(url(id, role), role === "proxy" ? { headers: auth } : undefined);
  return new Promise((ok, no) => { ws.onopen = () => ok(ws); ws.onerror = () => no(new Error(role + " failed")); });
};
const closed = (ws) => new Promise((r) => ws.addEventListener("close", (e) => r(e), { once: true }));
const next = (ws) => new Promise((r) => ws.addEventListener("message", (e) => r(String(e.data)), { once: true }));
const expect = (c, m) => { if (!c) throw new Error(m); };
async function check(name, fn) {
  try { await fn(); console.log("PASS " + name); } catch (e) { failed++; console.log("FAIL " + name + ": " + e.message); }
}

await check("a page on an ended session is told why", async () => {
  const page = new WebSocket(url(session(), "editor"));
  const e = await closed(page);
  expect(e.code === 4404, "code " + e.code);
  expect(/run the command on your proxy again/i.test(e.reason), "reason " + e.reason);
});

await check("a second tab takes over from the first", async () => {
  const id = session();
  const proxy = await open(id, "proxy");
  const first = await open(id, "editor");
  const firstClosed = closed(first);
  const second = await open(id, "editor");
  const e = await firstClosed;
  expect(e.code === 4001, "first closed with " + e.code);
  const got = next(second);
  proxy.send("hi");
  expect((await got) === "hi", "second tab got nothing");
  proxy.close();
});

process.exit(failed ? 1 : 0);
