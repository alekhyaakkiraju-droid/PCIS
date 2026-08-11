-- Watermark state and run log for polling sync agent
CREATE TABLE sync_watermark_state (
    domain_name      VARCHAR(64)  NOT NULL,
    source_table     VARCHAR(128) NOT NULL,
    watermark_column VARCHAR(128) NOT NULL,
    watermark_value  VARCHAR(256) NOT NULL DEFAULT '0',
    last_run_at      TIMESTAMPTZ,
    last_run_status  VARCHAR(32),
    rows_extracted   BIGINT       NOT NULL DEFAULT 0,
    rows_upserted    BIGINT       NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_sync_watermark_state PRIMARY KEY (domain_name)
);

CREATE TABLE sync_run_log (
    run_id           BIGSERIAL    PRIMARY KEY,
    domain_name      VARCHAR(64)  NOT NULL,
    started_at       TIMESTAMPTZ  NOT NULL,
    finished_at      TIMESTAMPTZ,
    status           VARCHAR(32)  NOT NULL,
    rows_extracted   BIGINT       NOT NULL DEFAULT 0,
    rows_upserted    BIGINT       NOT NULL DEFAULT 0,
    error_message    TEXT
);

CREATE INDEX idx_sync_run_log_domain ON sync_run_log (domain_name, started_at DESC);
