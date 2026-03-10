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
