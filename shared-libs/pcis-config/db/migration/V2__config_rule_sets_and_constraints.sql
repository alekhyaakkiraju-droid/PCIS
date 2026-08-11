-- WO-173: CONFIG_RULE_SET_T, effective-date constraints, history FK, batch actor and rule seeds

-- Support multiple effective-dated rows per tunable key (TunableRepository queries by date range).
ALTER TABLE config_tunable_t DROP CONSTRAINT config_tunable_t_pkey;
ALTER TABLE config_tunable_t ADD COLUMN tunable_row_id BIGINT GENERATED ALWAYS AS IDENTITY;
ALTER TABLE config_tunable_t ADD CONSTRAINT config_tunable_t_pkey PRIMARY KEY (tunable_row_id);
ALTER TABLE config_tunable_t ADD CONSTRAINT uq_config_tunable_key_version UNIQUE (tunable_key, version_no);

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE config_tunable_t ADD CONSTRAINT ex_config_tunable_effective_dates
    EXCLUDE USING gist (
        tunable_key WITH =,
        daterange(effective_from, COALESCE(effective_to, 'infinity'::date), '[]') WITH &&
    );

ALTER TABLE config_tunable_history_t
    ADD CONSTRAINT fk_config_tunable_history_tunable
    FOREIGN KEY (tunable_key, version_no)
    REFERENCES config_tunable_t (tunable_key, version_no);

CREATE TABLE config_rule_set_t (
    rule_set_key VARCHAR(60) NOT NULL,
    version_no INTEGER NOT NULL,
    payload JSONB NOT NULL,
    description VARCHAR(200) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status_cd CHAR(1) NOT NULL DEFAULT 'A',
    crt_user VARCHAR(10),
    crt_timestamp TIMESTAMP,
    upd_user VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT config_rule_set_t_pkey PRIMARY KEY (rule_set_key, version_no),
    CONSTRAINT uq_config_rule_set_key_version UNIQUE (rule_set_key, version_no)
);

INSERT INTO config_tunable_t (
    tunable_key, domain_cd, value_type, value_text, description,
    effective_from, version_no, crt_user, crt_timestamp
) VALUES
    ('batch.actor.audit', 'BAT', 'S', 'BATCH_AUD', 'Audit batch job actor principal', CURRENT_DATE, 1, 'SYSTEM', CURRENT_TIMESTAMP),
    ('batch.actor.billing', 'BAT', 'S', 'BATCH_BIL', 'Billing batch job actor principal', CURRENT_DATE, 1, 'SYSTEM', CURRENT_TIMESTAMP),
    ('batch.actor.commission', 'BAT', 'S', 'BATCH_CMM', 'Commission batch job actor principal', CURRENT_DATE, 1, 'SYSTEM', CURRENT_TIMESTAMP),
    ('batch.actor.premium', 'BAT', 'S', 'BATCH_PRM', 'Premium batch job actor principal', CURRENT_DATE, 1, 'SYSTEM', CURRENT_TIMESTAMP),
    ('batch.actor.claims', 'BAT', 'S', 'BATCH_CLM', 'Claims batch job actor principal', CURRENT_DATE, 1, 'SYSTEM', CURRENT_TIMESTAMP),
    ('batch.actor.renewal', 'BAT', 'S', 'BATCH_REN', 'Renewal batch job actor principal', CURRENT_DATE, 1, 'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO config_rule_set_t (
    rule_set_key, version_no, payload, description, effective_from, status_cd, crt_user, crt_timestamp
) VALUES (
    'billing-frequency-interval',
    1,
    '{
      "mappings": [
        {"frequency": "M", "intervalMonths": 1},
        {"frequency": "Q", "intervalMonths": 3},
        {"frequency": "S", "intervalMonths": 6},
        {"frequency": "A", "intervalMonths": 12}
      ],
      "defaultIntervalMonths": 12
    }'::jsonb,
    'BIL003B billing frequency to interval mapping in months',
    CURRENT_DATE,
    'A',
    'SYSTEM',
    CURRENT_TIMESTAMP
);
