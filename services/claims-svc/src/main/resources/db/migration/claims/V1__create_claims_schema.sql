-- PCIS Claims Domain V1 Schema Migration
-- All monetary columns use NUMERIC(11,2) — no FLOAT, DOUBLE, or REAL types.
-- Uses IF NOT EXISTS for idempotent re-run after partial failure.
-- Cross-domain references (pol_nbr → POLICY_HEADER_T, cust_id → CUSTOMER_T) are
-- not enforced as FK constraints here since those tables reside in separate domains.

-- -------------------------------------------------------------------------
-- claim_adjuster must be created before claim_payment (FK dependency)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim_adjuster (
    adjuster_id       VARCHAR(10)    NOT NULL,
    adjuster_name     VARCHAR(60)    NOT NULL,
    authority_limit   NUMERIC(11,2)  NOT NULL,
    crt_user          VARCHAR(10)    NOT NULL,
    crt_timestamp     TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user          VARCHAR(10),
    upd_timestamp     TIMESTAMP,
    CONSTRAINT pk_claim_adjuster PRIMARY KEY (adjuster_id)
);

-- -------------------------------------------------------------------------
-- claim — root aggregate, business key claim_nbr
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim (
    claim_nbr     VARCHAR(12)  NOT NULL,
    pol_nbr       VARCHAR(12)  NOT NULL,
    cust_id       INTEGER      NOT NULL,
    loss_date     DATE         NOT NULL,
    claim_type    VARCHAR(3)   NOT NULL,
    claim_status  CHAR(1)      NOT NULL DEFAULT 'O',
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_claim PRIMARY KEY (claim_nbr)
);

CREATE INDEX IF NOT EXISTS idx_claim_pol_nbr   ON claim (pol_nbr);
CREATE INDEX IF NOT EXISTS idx_claim_cust_id   ON claim (cust_id);
CREATE INDEX IF NOT EXISTS idx_claim_status    ON claim (claim_status);

-- -------------------------------------------------------------------------
-- claim_reserve
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim_reserve (
    reserve_id      BIGINT GENERATED ALWAYS AS IDENTITY,
    claim_nbr       VARCHAR(12)    NOT NULL,
    reserve_type    VARCHAR(3)     NOT NULL,
    approved_amt    NUMERIC(11,2)  NOT NULL,
    paid_to_date    NUMERIC(11,2)  NOT NULL DEFAULT 0.00,
    reserve_status  CHAR(1)        NOT NULL DEFAULT 'O',
    crt_user        VARCHAR(10)    NOT NULL,
    crt_timestamp   TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user        VARCHAR(10),
    upd_timestamp   TIMESTAMP,
    CONSTRAINT pk_claim_reserve     PRIMARY KEY (reserve_id),
    CONSTRAINT fk_reserve_claim_nbr FOREIGN KEY (claim_nbr) REFERENCES claim (claim_nbr)
);

CREATE INDEX IF NOT EXISTS idx_claim_reserve_claim_nbr     ON claim_reserve (claim_nbr);
CREATE INDEX IF NOT EXISTS idx_claim_reserve_status        ON claim_reserve (reserve_status);

-- -------------------------------------------------------------------------
-- approval — must exist before claim_payment (FK dependency)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS approval (
    approval_id     BIGINT GENERATED ALWAYS AS IDENTITY,
    claim_nbr       VARCHAR(12)  NOT NULL,
    reserve_id      BIGINT       NOT NULL,
    approver_id     VARCHAR(10)  NOT NULL,
    approval_status CHAR(1)      NOT NULL DEFAULT 'P',
    approval_date   TIMESTAMP,
    crt_user        VARCHAR(10)  NOT NULL,
    crt_timestamp   TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user        VARCHAR(10),
    upd_timestamp   TIMESTAMP,
    CONSTRAINT pk_approval          PRIMARY KEY (approval_id),
    CONSTRAINT fk_approval_claim    FOREIGN KEY (claim_nbr)   REFERENCES claim (claim_nbr),
    CONSTRAINT fk_approval_reserve  FOREIGN KEY (reserve_id)  REFERENCES claim_reserve (reserve_id)
);

CREATE INDEX IF NOT EXISTS idx_approval_claim_nbr ON approval (claim_nbr);
CREATE INDEX IF NOT EXISTS idx_approval_status    ON approval (approval_status);

-- -------------------------------------------------------------------------
-- claim_payment
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim_payment (
    payment_id      BIGINT GENERATED ALWAYS AS IDENTITY,
    claim_nbr       VARCHAR(12)    NOT NULL,
    payment_amt     NUMERIC(11,2)  NOT NULL,
    payment_status  CHAR(1)        NOT NULL DEFAULT 'P',
    payee_id        INTEGER,
    approval_id     BIGINT,
    adjuster_id     VARCHAR(10),
    crt_user        VARCHAR(10)    NOT NULL,
    crt_timestamp   TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user        VARCHAR(10),
    upd_timestamp   TIMESTAMP,
    CONSTRAINT pk_claim_payment           PRIMARY KEY (payment_id),
    CONSTRAINT fk_payment_claim           FOREIGN KEY (claim_nbr)    REFERENCES claim (claim_nbr),
    CONSTRAINT fk_payment_approval        FOREIGN KEY (approval_id)  REFERENCES approval (approval_id),
    CONSTRAINT fk_payment_adjuster        FOREIGN KEY (adjuster_id)  REFERENCES claim_adjuster (adjuster_id)
);

CREATE INDEX IF NOT EXISTS idx_claim_payment_claim_nbr ON claim_payment (claim_nbr);
CREATE INDEX IF NOT EXISTS idx_claim_payment_status    ON claim_payment (payment_status);

-- -------------------------------------------------------------------------
-- claim_note
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS claim_note (
    note_id       BIGINT GENERATED ALWAYS AS IDENTITY,
    claim_nbr     VARCHAR(12)  NOT NULL,
    note_text     TEXT         NOT NULL,
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_claim_note        PRIMARY KEY (note_id),
    CONSTRAINT fk_note_claim_nbr    FOREIGN KEY (claim_nbr) REFERENCES claim (claim_nbr)
);

CREATE INDEX IF NOT EXISTS idx_claim_note_claim_nbr ON claim_note (claim_nbr);

-- -------------------------------------------------------------------------
-- recovery
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recovery (
    recovery_id     BIGINT GENERATED ALWAYS AS IDENTITY,
    claim_nbr       VARCHAR(12)    NOT NULL,
    recovery_amt    NUMERIC(11,2)  NOT NULL,
    recovery_type   VARCHAR(3)     NOT NULL,
    crt_user        VARCHAR(10)    NOT NULL,
    crt_timestamp   TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user        VARCHAR(10),
    upd_timestamp   TIMESTAMP,
    CONSTRAINT pk_recovery          PRIMARY KEY (recovery_id),
    CONSTRAINT fk_recovery_claim    FOREIGN KEY (claim_nbr) REFERENCES claim (claim_nbr)
);

CREATE INDEX IF NOT EXISTS idx_recovery_claim_nbr ON recovery (claim_nbr);

-- -------------------------------------------------------------------------
-- outbox_events — transactional outbox for claims domain events
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(50)  NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    payload         TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

-- Partial index for the relay poller — only un-published events need scanning
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_events (created_at)
    WHERE published = FALSE;
