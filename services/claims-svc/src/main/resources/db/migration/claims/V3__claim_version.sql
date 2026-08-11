-- WO-197: optimistic locking for concurrent claim updates
ALTER TABLE claim ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
