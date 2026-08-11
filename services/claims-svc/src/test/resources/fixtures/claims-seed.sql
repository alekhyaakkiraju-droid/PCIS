-- Claims domain seed data for downstream story tests.
-- Provides representative rows covering: open claim, adjuster, reserve, approval,
-- payment, note, recovery, and outbox event.
-- Apply after V1__create_claims_schema.sql migration.

-- Adjuster with authority limit
INSERT INTO claim_adjuster (adjuster_id, adjuster_name, authority_limit, crt_user, crt_timestamp)
VALUES ('ADJ001', 'Jane Smith', 25000.00, 'seed', NOW())
ON CONFLICT (adjuster_id) DO NOTHING;

INSERT INTO claim_adjuster (adjuster_id, adjuster_name, authority_limit, crt_user, crt_timestamp)
VALUES ('ADJ002', 'Robert Jones', 100000.00, 'seed', NOW())
ON CONFLICT (adjuster_id) DO NOTHING;

-- Open claim
INSERT INTO claim (claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, crt_user, crt_timestamp)
VALUES ('CLM000000001', 'POL000000001', 1001, '2026-03-15', 'PRP', 'O', 'seed', NOW())
ON CONFLICT (claim_nbr) DO NOTHING;

-- Closed claim (paid)
INSERT INTO claim (claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, crt_user, crt_timestamp)
VALUES ('CLM000000002', 'POL000000002', 1002, '2026-02-01', 'LIA', 'C', 'seed', NOW())
ON CONFLICT (claim_nbr) DO NOTHING;

-- Reserve for CLM000000001
INSERT INTO claim_reserve (claim_nbr, reserve_type, approved_amt, paid_to_date, reserve_status, crt_user, crt_timestamp)
VALUES ('CLM000000001', 'PRO', 15000.00, 0.00, 'O', 'seed', NOW());

-- Approval for the reserve above (use subquery for reserve_id since it's IDENTITY)
INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000000001', reserve_id, 'ADJ002', 'A', NOW(), 'seed', NOW()
FROM claim_reserve WHERE claim_nbr = 'CLM000000001' LIMIT 1;

-- Payment against CLM000000001 (full reserve payout — CLM006B parity pattern)
INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT 'CLM000000001', 15000.00, 'P', 1001, a.approval_id, 'ADJ001', 'seed', NOW()
FROM approval a WHERE a.claim_nbr = 'CLM000000001' LIMIT 1;

-- Note on CLM000000001
INSERT INTO claim_note (claim_nbr, note_text, crt_user, crt_timestamp)
VALUES ('CLM000000001', 'Initial FNOL assessment: property damage from storm event.', 'seed', NOW());

-- Recovery on CLM000000002
INSERT INTO recovery (claim_nbr, recovery_amt, recovery_type, crt_user, crt_timestamp)
VALUES ('CLM000000002', 2500.00, 'SUB', 'seed', NOW());

-- Outbox event (un-published) for CLM000000001 payment
INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, created_at, published)
VALUES (
    gen_random_uuid(),
    'Claim',
    'CLM000000001',
    'ClaimPaymentInitiated',
    '{"claimNbr":"CLM000000001","paymentAmt":15000.00,"paymentStatus":"P"}',
    NOW(),
    FALSE
);
