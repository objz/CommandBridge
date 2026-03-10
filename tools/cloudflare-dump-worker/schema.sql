CREATE TABLE IF NOT EXISTS dumps (
  id TEXT PRIMARY KEY,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  payload TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_dumps_expires_at ON dumps(expires_at);
