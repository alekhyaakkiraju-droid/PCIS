CREATE SCHEMA IF NOT EXISTS legacy_snapshot;

CREATE TABLE IF NOT EXISTS legacy_snapshot.billing_schedule_snapshot (
    pol_nbr         VARCHAR(12) NOT NULL,
    installment_nbr INTEGER     NOT NULL,
    amt_due         NUMERIC(11,2) NOT NULL,
    sched_status    CHAR(1)     NOT NULL,
    business_date   DATE        NOT NULL,
    PRIMARY KEY (pol_nbr, installment_nbr, business_date)
);

CREATE TABLE IF NOT EXISTS billing_schedule_t (
    bill_sched_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pol_nbr         VARCHAR(12) NOT NULL,
    bill_plan_id    BIGINT      NOT NULL DEFAULT 1,
    installment_nbr INTEGER     NOT NULL,
    due_date        DATE        NOT NULL DEFAULT CURRENT_DATE,
    amt_due         NUMERIC(11,2) NOT NULL,
    amt_paid        NUMERIC(11,2),
    sched_status    CHAR(1)     NOT NULL,
    crt_user        VARCHAR(10),
    crt_timestamp   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_billing_schedule_pol_inst
    ON billing_schedule_t (pol_nbr, installment_nbr);

CREATE TABLE IF NOT EXISTS payment_application_t (
    payment_app_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id     BIGINT NOT NULL DEFAULT 1,
    invoice_id     BIGINT NOT NULL DEFAULT 1,
    applied_amt    NUMERIC(11,2) NOT NULL DEFAULT 0,
    crt_user       VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS invoice_line_t (
    invoice_line_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id      BIGINT NOT NULL DEFAULT 1,
    line_nbr        INTEGER NOT NULL DEFAULT 1,
    line_desc       VARCHAR(60),
    line_amt        NUMERIC(11,2) NOT NULL DEFAULT 0,
    crt_user        VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS invoice_t (
    invoice_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    crt_user   VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS billing_plan_t (
    bill_plan_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pol_nbr      VARCHAR(12) NOT NULL,
    crt_user     VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS claim_payment (
    claim_payment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_nbr        VARCHAR(12) NOT NULL,
    payment_amt      NUMERIC(11,2),
    crt_user         VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS claim (
    claim_nbr VARCHAR(12) PRIMARY KEY,
    crt_user  VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS customer_t (
    cust_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    crt_user VARCHAR(10)
);
