-- Demo policy data for wireframe Customer 360 (Marta Field cust_id 19284).

INSERT INTO coverage_type (cov_type, cov_desc, active_flag, crt_user, crt_timestamp)
VALUES
    ('HO-1', 'Homeowners Dwelling', 'Y', 'DEMO', NOW()),
    ('UM-1', 'Umbrella Liability', 'Y', 'DEMO', NOW())
ON CONFLICT (cov_type) DO NOTHING;

INSERT INTO policy (
    pol_nbr, cust_id, agt_id, policy_type, pol_status, eff_date, exp_date,
    prem_annual, bill_freq, crt_user, crt_timestamp)
VALUES
    ('POL000003001', 19284, 'AGT90001', 'HO-1', 'ACTV', DATE '2026-01-01', DATE '2027-01-01', 1420.50, 'M', 'DEMO', NOW()),
    ('POL000003008', 19284, 'AGT90001', 'UM-1', 'ACTV', DATE '2025-01-01', DATE '2026-01-01', 1240.00, 'M', 'DEMO', NOW())
ON CONFLICT (pol_nbr) DO UPDATE SET
    cust_id = EXCLUDED.cust_id,
    prem_annual = EXCLUDED.prem_annual,
    pol_status = EXCLUDED.pol_status,
    upd_user = 'DEMO',
    upd_timestamp = NOW();

INSERT INTO billing_plan (pol_nbr, bill_freq, nbr_installments, installment_fee, active_flag, crt_user, crt_timestamp)
VALUES
    ('POL000003001', 'M', 12, 0.00, 'Y', 'DEMO', NOW()),
    ('POL000003008', 'M', 12, 0.00, 'Y', 'DEMO', NOW())
ON CONFLICT (pol_nbr) DO NOTHING;

INSERT INTO coverage (
    coverage_id, pol_nbr, cov_type, limit_amt, ded_amt, cov_premium, crt_user, crt_timestamp)
VALUES
    ('COV19284000001', 'POL000003001', 'HO-1', 500000.00, 1000.00, 1420.50, 'DEMO', NOW()),
    ('COV19284000002', 'POL000003008', 'UM-1', 1000000.00, 0.00, 1240.00, 'DEMO', NOW())
ON CONFLICT (coverage_id) DO NOTHING;
