-- WO-193: mandatory billing plan per policy (edge case I-02)
-- COBOL: BILLING_PLAN_T row missing on POL001A issuance

CREATE TABLE IF NOT EXISTS billing_plan (
    bill_plan_id      BIGINT GENERATED ALWAYS AS IDENTITY,
    pol_nbr           VARCHAR(12)    NOT NULL,
    bill_freq         CHAR(1)        NOT NULL,
    nbr_installments  SMALLINT       NOT NULL CHECK (nbr_installments > 0),
    installment_fee   NUMERIC(7,2)   NOT NULL DEFAULT 0.00,
    active_flag       CHAR(1)        NOT NULL DEFAULT 'Y',
    crt_user          VARCHAR(10)    NOT NULL,
    crt_timestamp     TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user          VARCHAR(10),
    upd_timestamp     TIMESTAMP,
    CONSTRAINT pk_billing_plan PRIMARY KEY (bill_plan_id),
    CONSTRAINT uq_billing_plan_pol_nbr UNIQUE (pol_nbr),
    CONSTRAINT fk_billing_plan_policy FOREIGN KEY (pol_nbr) REFERENCES policy (pol_nbr)
);

CREATE INDEX IF NOT EXISTS idx_billing_plan_pol_nbr ON billing_plan (pol_nbr);
