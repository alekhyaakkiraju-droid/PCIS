-- WO-194: optimistic locking for concurrent endorsement handling
ALTER TABLE policy ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
