-- Rating pipeline seed data (WO-189)

INSERT INTO rate_table_t (policy_type, territory, base_rate, eff_date, crt_user, crt_timestamp)
VALUES ('HOME', 'TX', 1200.00, DATE '2025-01-01', 'TEST', CURRENT_TIMESTAMP);

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'AGE', 1.0000, 'TEST', CURRENT_TIMESTAMP
FROM rate_table_t WHERE policy_type = 'HOME' AND territory = 'TX';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'TERR', 1.0500, 'TEST', CURRENT_TIMESTAMP
FROM rate_table_t WHERE policy_type = 'HOME' AND territory = 'TX';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'CLAIMS', 1.0000, 'TEST', CURRENT_TIMESTAMP
FROM rate_table_t WHERE policy_type = 'HOME' AND territory = 'TX';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'RISK-TIER', 1.0000, 'TEST', CURRENT_TIMESTAMP
FROM rate_table_t WHERE policy_type = 'HOME' AND territory = 'TX';

INSERT INTO discount_rule_t (
    disc_code, disc_pct, eff_date, policy_type, stacking_group, discount_type, max_combined_pct,
    eligibility_code, crt_user, crt_timestamp)
VALUES
    ('MULTI', 0.0500, DATE '2025-01-01', 'HOM', 'LOYALTY', 'MULTIPLICATIVE', 0.2500, 'MULTI_POLICY', 'TEST', CURRENT_TIMESTAMP),
    ('SAFE', 0.0300, DATE '2025-01-01', 'HOM', 'SAFETY', 'MULTIPLICATIVE', 0.2500, 'CLAIMS_FREE', 'TEST', CURRENT_TIMESTAMP),
    ('FLAT10', 0.0000, DATE '2025-01-01', 'HOM', 'FLAT', 'FLAT', 0.2500, NULL, 'TEST', CURRENT_TIMESTAMP);

UPDATE discount_rule_t SET discount_amt = 10.00 WHERE disc_code = 'FLAT10';

INSERT INTO surcharge_rule_t (
    surch_code, surch_pct, eff_date, policy_type, surcharge_type, calc_type, max_combined_surcharge_pct,
    crt_user, crt_timestamp)
VALUES
    ('WIND', 0.0500, DATE '2025-01-01', 'HOM', 'MANDATORY', 'MULTIPLICATIVE', 0.5000, 'TEST', CURRENT_TIMESTAMP),
    ('POOL', 0.0200, DATE '2025-01-01', 'HOM', 'DISCRETIONARY', 'MULTIPLICATIVE', 0.5000, 'TEST', CURRENT_TIMESTAMP);

INSERT INTO tax_table_t (
    state, tax_pct, eff_date, tax_type, flat_fee, compound_flag, calc_sequence, crt_user, crt_timestamp)
VALUES
    ('TX', 0.0200, DATE '2025-01-01', 'STATE', 0.00, FALSE, 1, 'TEST', CURRENT_TIMESTAMP),
    ('TX', 0.0100, DATE '2025-01-01', 'LOCAL', 5.00, TRUE, 2, 'TEST', CURRENT_TIMESTAMP);

INSERT INTO uw_rule_t (
    rule_code, rule_text, policy_type, rule_type, condition_field, condition_operator, condition_value, outcome,
    crt_user, crt_timestamp)
VALUES
    ('HS-LIMIT', 'Coverage limit exceeds hard stop', 'HOM', 'HARD_STOP', 'LIMIT', 'GT', 500000.00, 'DECLINE', 'TEST', CURRENT_TIMESTAMP),
    ('HS-CLAIMS', 'Claims count exceeds referral threshold', 'HOM', 'THRESHOLD', 'CLAIMS_COUNT', 'GT', 2.00, 'REFER', 'TEST', CURRENT_TIMESTAMP);
