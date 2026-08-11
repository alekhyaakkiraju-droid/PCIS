-- WO-195 fault injection fixture (2 eligible policies)
DELETE FROM batch_exceptions;
DELETE FROM outbox_events WHERE aggregate_id LIKE 'POLF%';
DELETE FROM policy_history WHERE pol_nbr LIKE 'POLF%';
DELETE FROM deductible WHERE coverage_id IN (
    SELECT coverage_id FROM coverage WHERE pol_nbr LIKE 'POLF%');
DELETE FROM coverage WHERE pol_nbr LIKE 'POLF%';
DELETE FROM billing_plan WHERE pol_nbr LIKE 'POLF%';
DELETE FROM policy_property WHERE pol_nbr LIKE 'POLF%';
DELETE FROM policy WHERE pol_nbr LIKE 'POLF%';
DELETE FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'POL006B';

INSERT INTO coverage_type (cov_type, cov_desc, active_flag, crt_user, crt_timestamp)
VALUES ('HO-1', 'Homeowners Dwelling', 'Y', 'TEST', NOW())
ON CONFLICT (cov_type) DO NOTHING;

INSERT INTO policy (
    pol_nbr, cust_id, agt_id, policy_type, pol_status, eff_date, exp_date,
    prem_annual, bill_freq, crt_user, crt_timestamp)
VALUES
  ('POLF00000001', 3001, 'AGT00001', 'HO-1', 'ACTV', DATE '2025-08-11', DATE '2026-09-01', 1200.00, 'M', 'TEST', NOW()),
  ('POLF00000002', 3002, 'AGT00001', 'HO-1', 'ACTV', DATE '2025-08-11', DATE '2026-09-15', 1300.00, 'M', 'TEST', NOW());

INSERT INTO billing_plan (pol_nbr, bill_freq, nbr_installments, installment_fee, active_flag, crt_user, crt_timestamp)
VALUES
  ('POLF00000001', 'M', 12, 0.00, 'Y', 'TEST', NOW()),
  ('POLF00000002', 'M', 12, 0.00, 'Y', 'TEST', NOW());

INSERT INTO policy_property (pol_nbr, prop_type, addr_line1, state_code, crt_user, crt_timestamp)
VALUES
  ('POLF00000001', 'HOME', '1 Fault St', 'TX', 'TEST', NOW()),
  ('POLF00000002', 'HOME', '2 Fault St', 'TX', 'TEST', NOW());

INSERT INTO coverage (
    coverage_id, pol_nbr, cov_type, limit_amt, ded_amt, cov_premium, crt_user, crt_timestamp)
VALUES
  ('COVF0000000001', 'POLF00000001', 'HO-1', 300000.00, 1000.00, 1200.00, 'TEST', NOW()),
  ('COVF0000000002', 'POLF00000002', 'HO-1', 310000.00, 1000.00, 1300.00, 'TEST', NOW());
