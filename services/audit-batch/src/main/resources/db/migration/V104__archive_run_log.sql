-- WO-170: audit archive job run summary (distinct from generic RPT_RUN_LOG_T).
CREATE TABLE IF NOT EXISTS archive_run_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_name VARCHAR(64) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ,
    partitions_processed INTEGER NOT NULL DEFAULT 0,
    rows_archived BIGINT NOT NULL DEFAULT 0,
    verification_status VARCHAR(20),
    exit_code INTEGER,
    error_message TEXT,
    crt_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_archive_run_log_job ON archive_run_log (job_name, start_time DESC);
