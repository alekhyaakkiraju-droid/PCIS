-- Policy domain seed data for downstream REST/batch tests (WO-193)
INSERT INTO coverage_type (cov_type, cov_desc, active_flag, crt_user, crt_timestamp)
VALUES
  ('HO-1', 'Homeowners Dwelling', 'Y', 'SEED', NOW()),
  ('AU-1', 'Auto Liability', 'Y', 'SEED', NOW())
ON CONFLICT (cov_type) DO NOTHING;

INSERT INTO policy (
    pol_nbr, cust_id, agt_id, policy_type, pol_status, eff_date, exp_date,
    prem_annual, bill_freq, crt_user, crt_timestamp
) VALUES
  ('POL10000001', 1001, 'AGT00001', 'HO-1', 'ACTV', DATE '2026-01-01', DATE '2027-01-01', 2400.00, 'M', 'SEED', NOW()),
  ('POL10000002', 1002, 'AGT00002', 'AU-1', 'ACTV', DATE '2026-02-01', DATE '2027-02-01', 1200.00, 'Q', 'SEED', NOW())
ON CONFLICT (pol_nbr) DO NOTHING;

INSERT INTO billing_plan (pol_nbr, bill_freq, nbr_installments, installment_fee, active_flag, crt_user, crt_timestamp)
VALUES
  ('POL10000001', 'M', 12, 0.00, 'Y', 'SEED', NOW()),
  ('POL10000002', 'Q', 4, 5.00, 'Y', 'SEED', NOW())
ON CONFLICT (pol_nbr) DO NOTHING;

INSERT INTO coverage (
    coverage_id, pol_nbr, cov_type, limit_amt, ded_amt, cov_premium, crt_user, crt_timestamp
) VALUES
  ('COV100000000001', 'POL10000001', 'HO-1', 500000.00, 1000.00, 2400.00, 'SEED', NOW()),
  ('COV100000000002', 'POL10000002', 'AU-1', 300000.00, 500.00, 1200.00, 'SEED', NOW())
ON CONFLICT (coverage_id) DO NOTHING;
