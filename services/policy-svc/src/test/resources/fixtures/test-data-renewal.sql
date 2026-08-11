-- WO-195 policy renewal integration fixtures (5 eligible policies)
DELETE FROM batch_exceptions;
DELETE FROM outbox_events WHERE aggregate_id LIKE 'POLR%';
DELETE FROM policy_history WHERE pol_nbr LIKE 'POLR%';
DELETE FROM deductible WHERE coverage_id IN (
    SELECT coverage_id FROM coverage WHERE pol_nbr LIKE 'POLR%');
DELETE FROM coverage WHERE pol_nbr LIKE 'POLR%';
DELETE FROM billing_plan WHERE pol_nbr LIKE 'POLR%';
DELETE FROM policy_property WHERE pol_nbr LIKE 'POLR%';
DELETE FROM policy WHERE pol_nbr LIKE 'POLR%';
DELETE FROM RPT_RUN_LOG_T WHERE PGM_NAME = 'POL006B';

INSERT INTO coverage_type (cov_type, cov_desc, active_flag, crt_user, crt_timestamp)
VALUES ('HO-1', 'Homeowners Dwelling', 'Y', 'TEST', NOW())
ON CONFLICT (cov_type) DO NOTHING;

INSERT INTO policy (
    pol_nbr, cust_id, agt_id, policy_type, pol_status, eff_date, exp_date,
    prem_annual, bill_freq, crt_user, crt_timestamp)
VALUES
  ('POLR00000001', 2001, 'AGT00001', 'HO-1', 'ACTV', DATE '2025-08-11', DATE '2026-09-01', 1200.00, 'M', 'TEST', NOW()),
  ('POLR00000002', 2002, 'AGT00001', 'HO-1', 'ACTV', DATE '2025-08-11', DATE '2026-09-10', 1300.00, 'M', 'TEST', NOW()),
  ('POLR00000003', 2003, 'AGT00001', 'HO-1', 'ACTV', DATE '2025-08-11', DATE '2026-09-15', 1400.00, 'Q', 'TEST', NOW()),
  ('POLR00000004', 2004, 'AGT00001', 'HO-1', 'ACTV', DATE '2025-08-11', DATE '2026-09-20', 1500.00, 'M', 'TEST', NOW()),
  ('POLR00000005', 2005, 'AGT00001', 'HO-1', 'ACTV', DATE '2025-08-11', DATE '2026-09-25', 1600.00, 'M', 'TEST', NOW());

INSERT INTO billing_plan (pol_nbr, bill_freq, nbr_installments, installment_fee, active_flag, crt_user, crt_timestamp)
SELECT pol_nbr, bill_freq,
       CASE bill_freq WHEN 'Q' THEN 4 ELSE 12 END,
       0.00, 'Y', 'TEST', NOW()
FROM policy
WHERE pol_nbr LIKE 'POLR%';

INSERT INTO policy_property (pol_nbr, prop_type, addr_line1, state_code, crt_user, crt_timestamp)
SELECT pol_nbr, 'HOME', '100 Main St', 'TX', 'TEST', NOW()
FROM policy
WHERE pol_nbr LIKE 'POLR%';

INSERT INTO coverage (
    coverage_id, pol_nbr, cov_type, limit_amt, ded_amt, cov_premium, crt_user, crt_timestamp)
VALUES
  ('COVR0000000001', 'POLR00000001', 'HO-1', 300000.00, 1000.00, 1200.00, 'TEST', NOW()),
  ('COVR0000000002', 'POLR00000002', 'HO-1', 310000.00, 1000.00, 1300.00, 'TEST', NOW()),
  ('COVR0000000003', 'POLR00000003', 'HO-1', 320000.00, 1000.00, 1400.00, 'TEST', NOW()),
  ('COVR0000000004', 'POLR00000004', 'HO-1', 330000.00, 1000.00, 1500.00, 'TEST', NOW()),
  ('COVR0000000005', 'POLR00000005', 'HO-1', 340000.00, 1000.00, 1600.00, 'TEST', NOW());

INSERT INTO deductible (coverage_id, ded_amt, ded_type, crt_user, crt_timestamp)
VALUES
  ('COVR0000000001', 500.00, 'FLAT', 'TEST', NOW()),
  ('COVR0000000001', 1000.00, 'WIND', 'TEST', NOW()),
  ('COVR0000000003', 750.00, 'FLAT', 'TEST', NOW()),
  ('COVR0000000003', 1500.00, 'HAIL', 'TEST', NOW()),
  ('COVR0000000003', 250.00, 'THEF', 'TEST', NOW());
