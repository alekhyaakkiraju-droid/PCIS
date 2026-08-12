-- Extended demo policy set — 13 new policies across the 6 new customers (30001-30006)
-- plus Alice Johnson (100001), alongside the original Customer 360 demo policies
-- for Marta Field. Idempotent repeatable migration.

INSERT INTO coverage_type (cov_type, cov_desc, active_flag, crt_user, crt_timestamp)
VALUES
    ('HO-3', 'Homeowners Special Form', 'Y', 'JPARK', NOW()),
    ('AUTO', 'Personal Auto Liability & Collision', 'Y', 'JPARK', NOW()),
    ('GL', 'Commercial General Liability', 'Y', 'JPARK', NOW())
ON CONFLICT (cov_type) DO NOTHING;

INSERT INTO policy (
    pol_nbr, cust_id, agt_id, policy_type, pol_status, eff_date, exp_date,
    prem_annual, bill_freq, crt_user, crt_timestamp)
VALUES
    ('POL000004001', 100001, 'AGT90001', 'AUTO', 'ACTV', DATE '2026-02-01', DATE '2027-02-01', 980.00, 'M', 'JPARK', NOW()),
    ('POL000004002', 30001, 'AGT90002', 'HO-3', 'ACTV', DATE '2026-01-15', DATE '2027-01-15', 1560.00, 'M', 'JPARK', NOW()),
    ('POL000004003', 30001, 'AGT90002', 'AUTO', 'ACTV', DATE '2026-03-01', DATE '2027-03-01', 1120.00, 'M', 'JPARK', NOW()),
    ('POL000004004', 30002, 'AGT90001', 'HO-1', 'ACTV', DATE '2026-02-10', DATE '2027-02-10', 1340.00, 'M', 'JPARK', NOW()),
    ('POL000004005', 30002, 'AGT90001', 'UM-1', 'ACTV', DATE '2026-02-10', DATE '2027-02-10', 890.00, 'M', 'JPARK', NOW()),
    ('POL000004006', 30003, 'AGT90003', 'GL', 'ACTV', DATE '2025-12-01', DATE '2026-12-01', 4200.00, 'Q', 'JPARK', NOW()),
    ('POL000004007', 30003, 'AGT90003', 'AUTO', 'ACTV', DATE '2025-12-01', DATE '2026-12-01', 3100.00, 'Q', 'JPARK', NOW()),
    ('POL000004008', 30004, 'AGT90002', 'HO-3', 'ACTV', DATE '2026-04-01', DATE '2027-04-01', 1750.00, 'M', 'JPARK', NOW()),
    ('POL000004009', 30004, 'AGT90002', 'AUTO', 'CANC', DATE '2025-06-01', DATE '2026-06-01', 1050.00, 'M', 'JPARK', NOW()),
    ('POL000004010', 30005, 'AGT90001', 'HO-1', 'ACTV', DATE '2026-05-01', DATE '2027-05-01', 1290.00, 'M', 'JPARK', NOW()),
    ('POL000004011', 30005, 'AGT90001', 'UM-1', 'ACTV', DATE '2026-05-01', DATE '2027-05-01', 780.00, 'M', 'JPARK', NOW()),
    ('POL000004012', 30006, 'AGT90003', 'AUTO', 'ACTV', DATE '2026-06-15', DATE '2027-06-15', 1400.00, 'M', 'JPARK', NOW()),
    ('POL000004013', 30006, 'AGT90003', 'HO-3', 'EXPD', DATE '2025-01-01', DATE '2026-01-01', 1600.00, 'M', 'JPARK', NOW())
ON CONFLICT (pol_nbr) DO UPDATE SET
    cust_id = EXCLUDED.cust_id, prem_annual = EXCLUDED.prem_annual, pol_status = EXCLUDED.pol_status,
    upd_user = 'JPARK', upd_timestamp = NOW();

INSERT INTO billing_plan (pol_nbr, bill_freq, nbr_installments, installment_fee, active_flag, crt_user, crt_timestamp)
VALUES
    ('POL000004001', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004002', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004003', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004004', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004005', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004006', 'Q', 4, 5.00, 'Y', 'JPARK', NOW()),
    ('POL000004007', 'Q', 4, 5.00, 'Y', 'JPARK', NOW()),
    ('POL000004008', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004009', 'M', 12, 0.00, 'N', 'JPARK', NOW()),
    ('POL000004010', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004011', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004012', 'M', 12, 0.00, 'Y', 'JPARK', NOW()),
    ('POL000004013', 'M', 12, 0.00, 'N', 'JPARK', NOW())
ON CONFLICT (pol_nbr) DO NOTHING;

INSERT INTO coverage (coverage_id, pol_nbr, cov_type, limit_amt, ded_amt, cov_premium, crt_user, crt_timestamp)
VALUES
    ('COV30001000001', 'POL000004002', 'HO-3', 420000.00, 1500.00, 1560.00, 'JPARK', NOW()),
    ('COV30001000002', 'POL000004003', 'AUTO', 300000.00, 500.00, 1120.00, 'JPARK', NOW()),
    ('COV30002000001', 'POL000004004', 'HO-1', 380000.00, 1000.00, 1340.00, 'JPARK', NOW()),
    ('COV30002000002', 'POL000004005', 'UM-1', 1000000.00, 0.00, 890.00, 'JPARK', NOW()),
    ('COV30003000001', 'POL000004006', 'GL', 2000000.00, 0.00, 4200.00, 'JPARK', NOW()),
    ('COV30003000002', 'POL000004007', 'AUTO', 500000.00, 500.00, 3100.00, 'JPARK', NOW()),
    ('COV30004000001', 'POL000004008', 'HO-3', 450000.00, 1500.00, 1750.00, 'JPARK', NOW()),
    ('COV30004000002', 'POL000004009', 'AUTO', 250000.00, 500.00, 1050.00, 'JPARK', NOW()),
    ('COV30005000001', 'POL000004010', 'HO-1', 350000.00, 1000.00, 1290.00, 'JPARK', NOW()),
    ('COV30005000002', 'POL000004011', 'UM-1', 1000000.00, 0.00, 780.00, 'JPARK', NOW()),
    ('COV30006000001', 'POL000004012', 'AUTO', 300000.00, 500.00, 1400.00, 'JPARK', NOW()),
    ('COV30006000002', 'POL000004013', 'HO-3', 400000.00, 1500.00, 1600.00, 'JPARK', NOW()),
    ('COV1000010001', 'POL000004001', 'AUTO', 300000.00, 500.00, 980.00, 'JPARK', NOW())
ON CONFLICT (coverage_id) DO NOTHING;
