-- Rate lookup fixtures for BaseRateService integration tests (WO-187)
INSERT INTO rate_table_t (policy_type, territory, base_rate, eff_date, crt_user, crt_timestamp)
VALUES ('HOM', 'TX', 1000.00, DATE '2025-01-01', 'TEST', CURRENT_TIMESTAMP);

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'AGE', 1.1000, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'HOM';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'TERR', 1.0500, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'HOM';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'CLAIMS', 0.9500, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'HOM';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'RISK-TIER', 1.0200, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'HOM';

INSERT INTO rate_table_t (policy_type, territory, base_rate, eff_date, crt_user, crt_timestamp)
VALUES ('AUT', 'CA', 800.00, DATE '2025-01-01', 'TEST', CURRENT_TIMESTAMP);

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'AGE', 1.2000, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'AUT' AND territory = 'CA';

INSERT INTO rate_table_t (policy_type, territory, base_rate, eff_date, crt_user, crt_timestamp)
VALUES ('CML', 'NY', 5000.00, DATE '2025-01-01', 'TEST', CURRENT_TIMESTAMP);

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'TERR', 1.1000, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'CML' AND territory = 'NY';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'CLAIMS', 0.9000, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'CML' AND territory = 'NY';

INSERT INTO rate_factor_t (rate_table_id, factor_code, factor_value, crt_user, crt_timestamp)
SELECT rate_table_id, 'OCCUPANCY', 1.0500, 'TEST', CURRENT_TIMESTAMP FROM rate_table_t WHERE policy_type = 'CML' AND territory = 'NY';
