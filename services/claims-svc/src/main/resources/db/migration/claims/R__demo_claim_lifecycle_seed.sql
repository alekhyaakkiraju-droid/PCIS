-- Full wireframe claim lifecycle demo (CLM000004821 = UI CLM-0004821).
-- FNOL → reserve → payment → inquiry → approval queue scenarios.

DELETE FROM claim_reserve_ledger WHERE claim_nbr IN ('CLM000004821', 'CLM000004798', 'CLM000004770', 'CLM000000001');
DELETE FROM claim_payment WHERE claim_nbr IN ('CLM000004821', 'CLM000004798', 'CLM000004770', 'CLM000000001');
DELETE FROM approval WHERE claim_nbr IN ('CLM000004821', 'CLM000004798', 'CLM000004770', 'CLM000000001');
DELETE FROM claim_note WHERE claim_nbr IN ('CLM000004821', 'CLM000004798', 'CLM000004770', 'CLM000000001');
DELETE FROM claim_reserve WHERE claim_nbr IN ('CLM000004821', 'CLM000004798', 'CLM000004770', 'CLM000000001');
DELETE FROM claim WHERE claim_nbr IN ('CLM000004821', 'CLM000004798', 'CLM000004770', 'CLM000000001');

INSERT INTO claim_adjuster (adjuster_id, adjuster_name, authority_limit, crt_user, crt_timestamp)
VALUES
    ('ADJ90001', 'K. Alvarez', 25000.00, 'DEMO', NOW()),
    ('SUP90001', 'M. Kowalski', 100000.00, 'DEMO', NOW())
ON CONFLICT (adjuster_id) DO UPDATE SET
    adjuster_name = EXCLUDED.adjuster_name,
    authority_limit = EXCLUDED.authority_limit;

-- Primary wireframe claim: water damage, $28k reserve remaining after $20k payment
INSERT INTO claim (
    claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, assigned_adjuster_id, crt_user, crt_timestamp)
VALUES
    ('CLM000004821', 'POL000003001', 19284, DATE '2026-06-02', 'PRP', 'O', 'ADJ90001', 'DEMO', NOW());

INSERT INTO claim_reserve (
    claim_nbr, reserve_type, approved_amt, paid_to_date, reserve_status, crt_user, crt_timestamp)
VALUES
    ('CLM000004821', 'PRO', 48000.00, 20000.00, 'O', 'DEMO', NOW());

INSERT INTO claim_reserve_ledger (
    claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT
    'CLM000004821', reserve_id, DATE '2026-06-02', 'Initial FNOL reserve', 10000.00, 10000.00, 'ADJ90001', 'SET', TIMESTAMP '2026-06-02 09:15:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004821' LIMIT 1;

INSERT INTO claim_reserve_ledger (
    claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT
    'CLM000004821', reserve_id, DATE '2026-06-20', 'Increase — engineer report received', 38000.00, 48000.00, 'ADJ90001', 'INCR', TIMESTAMP '2026-06-20 14:30:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004821' LIMIT 1;

INSERT INTO claim_reserve_ledger (
    claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT
    'CLM000004821', reserve_id, DATE '2026-07-14', 'Drawdown on payment CLM-PMT-0231', -20000.00, 28000.00, 'ADJ90001', 'DRAW', TIMESTAMP '2026-07-14 10:00:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004821' LIMIT 1;

INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004821', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-07-14 10:00:00', 'DEMO', NOW()
FROM claim_reserve WHERE claim_nbr = 'CLM000004821' LIMIT 1;

INSERT INTO claim_payment (
    claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT
    'CLM000004821', 20000.00, 'P', 19284, a.approval_id, 'ADJ90001', 'DEMO', NOW()
FROM approval a WHERE a.claim_nbr = 'CLM000004821' LIMIT 1;

INSERT INTO claim_note (claim_nbr, note_text, crt_user, crt_timestamp)
VALUES
    ('CLM000004821', 'Insured reports upstairs pipe burst overnight; water damage to kitchen ceiling and hallway flooring. Emergency mitigation vendor dispatched.', 'DEMO', TIMESTAMP '2026-06-02 09:15:00'),
    ('CLM000004821', 'Structural engineer report received; reserve increased to reflect subfloor replacement scope.', 'DEMO', TIMESTAMP '2026-06-20 14:30:00');

-- Second open claim (inquiry open tab)
INSERT INTO claim (
    claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, assigned_adjuster_id, crt_user, crt_timestamp)
VALUES
    ('CLM000004798', 'POL000003001', 100001, DATE '2026-07-11', 'PRP', 'O', 'ADJ90001', 'DEMO', NOW());

INSERT INTO claim_reserve (
    claim_nbr, reserve_type, approved_amt, paid_to_date, reserve_status, crt_user, crt_timestamp)
VALUES
    ('CLM000004798', 'PRO', 6200.00, 0.00, 'O', 'DEMO', NOW());

INSERT INTO claim_reserve_ledger (
    claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT
    'CLM000004798', reserve_id, DATE '2026-07-11', 'Initial FNOL reserve', 6200.00, 6200.00, 'ADJ90001', 'SET', NOW()
FROM claim_reserve WHERE claim_nbr = 'CLM000004798' LIMIT 1;

INSERT INTO claim_note (claim_nbr, note_text, crt_user, crt_timestamp)
VALUES
    ('CLM000004798', 'Minor hail damage to roof shingles reported.', 'DEMO', NOW());

-- Closed claim (inquiry closed tab)
INSERT INTO claim (
    claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, assigned_adjuster_id, crt_user, crt_timestamp)
VALUES
    ('CLM000004770', 'POL000003001', 19284, DATE '2026-04-19', 'PRP', 'C', 'ADJ90001', 'DEMO', NOW());

INSERT INTO claim_reserve (
    claim_nbr, reserve_type, approved_amt, paid_to_date, reserve_status, crt_user, crt_timestamp)
VALUES
    ('CLM000004770', 'PRO', 14500.00, 14500.00, 'C', 'DEMO', NOW());

INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004770', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-05-01 12:00:00', 'DEMO', NOW()
FROM claim_reserve WHERE claim_nbr = 'CLM000004770' LIMIT 1;

INSERT INTO claim_payment (
    claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT
    'CLM000004770', 14500.00, 'P', 19284, a.approval_id, 'ADJ90001', 'DEMO', NOW()
FROM approval a WHERE a.claim_nbr = 'CLM000004770' LIMIT 1;
