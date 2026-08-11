-- PCIS Policy Domain V2 Schema Migration
-- Derived from shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql (POLICY_* tables).

CREATE SEQUENCE IF NOT EXISTS seq_policy_nbr
    START WITH 10000000 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999999 NO CYCLE CACHE 100;

CREATE SEQUENCE IF NOT EXISTS seq_coverage_id
    START WITH 10000000 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999999 NO CYCLE CACHE 100;

CREATE TABLE IF NOT EXISTS coverage_type (
    cov_type      CHAR(4)      NOT NULL,
    cov_desc      VARCHAR(60),
    active_flag   CHAR(1)      NOT NULL DEFAULT 'Y',
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_coverage_type PRIMARY KEY (cov_type)
);

CREATE TABLE IF NOT EXISTS policy (
    pol_nbr         VARCHAR(12)    NOT NULL,
    cust_id         INTEGER        NOT NULL,
    agt_id          VARCHAR(8)     NOT NULL,
    policy_type     CHAR(4)        NOT NULL,
    pol_status      CHAR(4)        NOT NULL,
    eff_date        DATE           NOT NULL,
    exp_date        DATE           NOT NULL,
    prem_annual     NUMERIC(13,2)  NOT NULL,
    renewal_of_pol  VARCHAR(12),
    bill_freq       CHAR(1)        NOT NULL,
    crt_user        VARCHAR(10)    NOT NULL,
    crt_timestamp   TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user        VARCHAR(10),
    upd_timestamp   TIMESTAMP,
    CONSTRAINT pk_policy PRIMARY KEY (pol_nbr)
);

CREATE INDEX IF NOT EXISTS idx_policy_cust_id   ON policy (cust_id);
CREATE INDEX IF NOT EXISTS idx_policy_agt_id    ON policy (agt_id);
CREATE INDEX IF NOT EXISTS idx_policy_status    ON policy (pol_status);
CREATE INDEX IF NOT EXISTS idx_policy_exp_date  ON policy (exp_date);

CREATE TABLE IF NOT EXISTS coverage (
    coverage_id   VARCHAR(14)    NOT NULL,
    pol_nbr       VARCHAR(12)    NOT NULL,
    cov_type      CHAR(4)        NOT NULL,
    limit_amt     NUMERIC(13,2)  NOT NULL,
    ded_amt       NUMERIC(11,2)  NOT NULL DEFAULT 0.00,
    cov_premium   NUMERIC(13,2)  NOT NULL,
    crt_user      VARCHAR(10)    NOT NULL,
    crt_timestamp TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_coverage PRIMARY KEY (coverage_id),
    CONSTRAINT fk_coverage_policy FOREIGN KEY (pol_nbr) REFERENCES policy (pol_nbr),
    CONSTRAINT fk_coverage_type   FOREIGN KEY (cov_type) REFERENCES coverage_type (cov_type)
);

CREATE INDEX IF NOT EXISTS idx_coverage_pol_nbr ON coverage (pol_nbr);

CREATE TABLE IF NOT EXISTS deductible (
    deduct_id     BIGINT GENERATED ALWAYS AS IDENTITY,
    coverage_id   VARCHAR(14)    NOT NULL,
    ded_amt       NUMERIC(11,2)  NOT NULL,
    ded_type      CHAR(4)        NOT NULL,
    crt_user      VARCHAR(10)    NOT NULL,
    crt_timestamp TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_deductible PRIMARY KEY (deduct_id),
    CONSTRAINT fk_deductible_coverage FOREIGN KEY (coverage_id) REFERENCES coverage (coverage_id)
);

CREATE INDEX IF NOT EXISTS idx_deductible_coverage_id ON deductible (coverage_id);

CREATE TABLE IF NOT EXISTS policy_history (
    hist_id       BIGINT GENERATED ALWAYS AS IDENTITY,
    pol_nbr       VARCHAR(12)  NOT NULL,
    event_code    CHAR(10)     NOT NULL,
    event_date    DATE         NOT NULL,
    event_desc    VARCHAR(100),
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_policy_history PRIMARY KEY (hist_id),
    CONSTRAINT fk_policy_history_policy FOREIGN KEY (pol_nbr) REFERENCES policy (pol_nbr)
);

CREATE INDEX IF NOT EXISTS idx_policy_history_pol_nbr ON policy_history (pol_nbr);

CREATE TABLE IF NOT EXISTS policy_property (
    property_id   BIGINT GENERATED ALWAYS AS IDENTITY,
    pol_nbr       VARCHAR(12)  NOT NULL,
    prop_type     CHAR(4)      NOT NULL,
    addr_line1    VARCHAR(50)  NOT NULL,
    state_code    CHAR(2)      NOT NULL,
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_policy_property PRIMARY KEY (property_id),
    CONSTRAINT fk_policy_property_policy FOREIGN KEY (pol_nbr) REFERENCES policy (pol_nbr)
);

CREATE INDEX IF NOT EXISTS idx_policy_property_pol_nbr ON policy_property (pol_nbr);

CREATE TABLE IF NOT EXISTS policy_vehicle (
    vehicle_id    BIGINT GENERATED ALWAYS AS IDENTITY,
    pol_nbr       VARCHAR(12)  NOT NULL,
    vin           VARCHAR(17),
    model_year    INTEGER,
    make          VARCHAR(30),
    model         VARCHAR(30),
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_policy_vehicle PRIMARY KEY (vehicle_id),
    CONSTRAINT fk_policy_vehicle_policy FOREIGN KEY (pol_nbr) REFERENCES policy (pol_nbr)
);

CREATE INDEX IF NOT EXISTS idx_policy_vehicle_pol_nbr ON policy_vehicle (pol_nbr);

CREATE TABLE IF NOT EXISTS endorsement (
    endorse_id    BIGINT GENERATED ALWAYS AS IDENTITY,
    pol_nbr       VARCHAR(12)    NOT NULL,
    end_type      CHAR(4)        NOT NULL,
    eff_date      DATE           NOT NULL,
    prem_chg      NUMERIC(11,2)  NOT NULL DEFAULT 0.00,
    crt_user      VARCHAR(10)    NOT NULL,
    crt_timestamp TIMESTAMP      NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_endorsement PRIMARY KEY (endorse_id),
    CONSTRAINT fk_endorsement_policy FOREIGN KEY (pol_nbr) REFERENCES policy (pol_nbr)
);

CREATE INDEX IF NOT EXISTS idx_endorsement_pol_nbr ON endorsement (pol_nbr);
