-- Adjuster assignment on claim + append-only reserve ledger (BR-03).

ALTER TABLE claim
    ADD COLUMN IF NOT EXISTS assigned_adjuster_id VARCHAR(10);

ALTER TABLE claim
    ADD CONSTRAINT fk_claim_assigned_adjuster
    FOREIGN KEY (assigned_adjuster_id) REFERENCES claim_adjuster (adjuster_id);

CREATE TABLE IF NOT EXISTS claim_reserve_ledger (
    ledger_id     BIGINT GENERATED ALWAYS AS IDENTITY,
    claim_nbr     VARCHAR(12)    NOT NULL,
    reserve_id    BIGINT,
    event_date    DATE           NOT NULL,
    reason        VARCHAR(200)   NOT NULL,
    amount        NUMERIC(11,2)  NOT NULL,
    balance_after NUMERIC(11,2)  NOT NULL,
    actor_id      VARCHAR(10)    NOT NULL,
    event_type    VARCHAR(4)     NOT NULL,
    crt_timestamp TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_claim_reserve_ledger PRIMARY KEY (ledger_id),
    CONSTRAINT fk_ledger_claim   FOREIGN KEY (claim_nbr)  REFERENCES claim (claim_nbr),
    CONSTRAINT fk_ledger_reserve FOREIGN KEY (reserve_id) REFERENCES claim_reserve (reserve_id)
);

CREATE INDEX IF NOT EXISTS idx_reserve_ledger_claim ON claim_reserve_ledger (claim_nbr);
