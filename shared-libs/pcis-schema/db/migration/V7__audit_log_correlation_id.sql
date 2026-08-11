-- V5: Add correlation_id to AUDIT_LOG_T for distributed trace correlation (WO-222)
-- Propagates X-Correlation-ID / pcis-correlation-id from HTTP and batch jobs into legacy audit rows.

ALTER TABLE AUDIT_LOG_T ADD COLUMN IF NOT EXISTS CORRELATION_ID VARCHAR(128);

COMMENT ON COLUMN AUDIT_LOG_T.CORRELATION_ID IS
    'Correlation token from X-Correlation-ID or pcis-correlation-id Kafka header';

CREATE INDEX IF NOT EXISTS idx_audit_log_t_correlation_id
    ON AUDIT_LOG_T (CORRELATION_ID)
    WHERE CORRELATION_ID IS NOT NULL;
