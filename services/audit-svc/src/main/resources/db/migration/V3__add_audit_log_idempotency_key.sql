-- V3: idempotency tracking for at-least-once outbox relay consumption (WO-169)
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS idempotency_key UUID;

CREATE INDEX IF NOT EXISTS idx_audit_log_idempotency_key
    ON audit_log (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS audit_ingestion_idempotency (
    idempotency_key UUID PRIMARY KEY,
    audit_log_id BIGINT NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_ingestion_idempotency_log
    ON audit_ingestion_idempotency (audit_log_id, event_timestamp);
