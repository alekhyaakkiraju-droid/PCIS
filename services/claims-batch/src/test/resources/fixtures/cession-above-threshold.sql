-- Reserve above reinsurance cession threshold (100000.00)
INSERT INTO claim_adjuster (adjuster_id, adjuster_name, authority_limit, crt_user, crt_timestamp)
VALUES ('BATCH_SVC', 'Batch Service Account', 999999999.99, 'seed', NOW())
ON CONFLICT (adjuster_id) DO NOTHING;

INSERT INTO claim_adjuster (adjuster_id, adjuster_name, authority_limit, crt_user, crt_timestamp)
VALUES ('SUP002', 'Supervisor Two', 999999999.99, 'seed', NOW())
ON CONFLICT (adjuster_id) DO NOTHING;

INSERT INTO claim (claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, version, crt_user, crt_timestamp)
VALUES ('CLM000000701', 'POL000000701', 1007, '2026-03-01', 'PRP', 'O', 0, 'seed', NOW())
ON CONFLICT (claim_nbr) DO NOTHING;

INSERT INTO claim_reserve (claim_nbr, reserve_type, approved_amt, paid_to_date, reserve_status, crt_user, crt_timestamp)
VALUES ('CLM000000701', 'PRO', 100000.01, 0.00, 'O', 'seed', NOW());

INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000000701', reserve_id, 'SUP002', 'A', NOW(), 'seed', NOW()
FROM claim_reserve WHERE claim_nbr = 'CLM000000701' LIMIT 1;
