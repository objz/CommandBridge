# Cloudflare Dump Worker

This worker stores `/cb dump` payloads in Cloudflare D1 and serves them back to a dedicated viewer page.

## Endpoints

- `POST /api/dump/upload`
  - Requires `Authorization: Bearer cb-dump-upload-v1`
  - Body: JSON dump payload
  - Response: `{ id, url, createdAt, expiresAt }` where `url` points to `/dump/?id=<id>`
- `GET /api/dump/:id`
  - Response: dump envelope
- `GET /api/dump/view/:id`
  - Compatibility redirect to `/dump/?id=<id>`

- `GET /api/editor/:id/ws?role=proxy|editor` (WebSocket)
  - Live editor relay, one Durable Object per session
  - `proxy` requires the same bearer token; `editor` can only join while the proxy is connected
  - One connection per role, forwarded as-is; sessions close after 1 hour or when the proxy leaves
  - Messages are signed end to end (`HMAC-SHA256(secret, role \n seq \n body)`), the relay never sees the secret

## Database

Apply schema to D1:

```bash
wrangler d1 execute commandbridge-dumps --remote --file schema.sql
```

## Deploy

Run from this directory:

```bash
wrangler deploy
```
