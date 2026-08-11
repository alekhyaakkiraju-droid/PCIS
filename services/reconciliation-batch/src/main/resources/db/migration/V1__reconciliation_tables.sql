-- WO-215: reconciliation break persistence and run summary for cutover gate scoring

CREATE TABLE reconciliation_break (
    break_id              BIGSERIAL PRIMARY KEY,
    run_id                BIGINT       NOT NULL,
    domain                VARCHAR(32)  NOT NULL,
    break_class           VARCHAR(32)  NOT NULL,
    entity_name           VARCHAR(64)  NOT NULL,
    business_key          VARCHAR(128) NOT NULL,
    column_name           VARCHAR(64),
    legacy_value          TEXT,
    target_value          TEXT,
    approved_decision_id  VARCHAR(64),
    first_seen_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_seen_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reconciliation_break_natural_key
        UNIQUE (domain, entity_name, business_key, column_name, break_class)
);

CREATE INDEX idx_reconciliation_break_run_id ON reconciliation_break (run_id);
CREATE INDEX idx_reconciliation_break_domain ON reconciliation_break (domain);
CREATE INDEX idx_reconciliation_break_unexplained
    ON reconciliation_break (domain)
    WHERE approved_decision_id IS NULL;

CREATE TABLE reconciliation_run_summary (
    run_id                    BIGSERIAL PRIMARY KEY,
    domain                    VARCHAR(32)  NOT NULL,
    business_date             DATE         NOT NULL,
    started_at                TIMESTAMPTZ  NOT NULL,
    completed_at              TIMESTAMPTZ,
    entity_count              INTEGER      NOT NULL DEFAULT 0,
    rows_compared             BIGINT       NOT NULL DEFAULT 0,
    break_count               BIGINT       NOT NULL DEFAULT 0,
    unexplained_break_count   BIGINT       NOT NULL DEFAULT 0,
    gate_verdict              VARCHAR(8),
    consecutive_clean_days    INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_reconciliation_run_domain_date UNIQUE (domain, business_date)
);

CREATE INDEX idx_reconciliation_run_summary_domain_completed
    ON reconciliation_run_summary (domain, completed_at DESC);
