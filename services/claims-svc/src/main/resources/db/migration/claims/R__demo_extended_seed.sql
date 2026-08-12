-- Extended demo claim set — 12 new claims across the new policies/customers
-- (POL000004001-013, customers 30001-30006 and 100001), alongside the original
-- Customer 360 demo claims for Marta Field. Idempotent repeatable migration.

DELETE FROM claim_reserve_ledger WHERE claim_nbr LIKE 'CLM00000490%' OR claim_nbr LIKE 'CLM00000491%';
DELETE FROM claim_payment WHERE claim_nbr LIKE 'CLM00000490%' OR claim_nbr LIKE 'CLM00000491%';
DELETE FROM approval WHERE claim_nbr LIKE 'CLM00000490%' OR claim_nbr LIKE 'CLM00000491%';
DELETE FROM claim_note WHERE claim_nbr LIKE 'CLM00000490%' OR claim_nbr LIKE 'CLM00000491%';
DELETE FROM claim_reserve WHERE claim_nbr LIKE 'CLM00000490%' OR claim_nbr LIKE 'CLM00000491%';
DELETE FROM claim WHERE claim_nbr LIKE 'CLM00000490%' OR claim_nbr LIKE 'CLM00000491%';

INSERT INTO claim_adjuster (adjuster_id, adjuster_name, authority_limit, crt_user, crt_timestamp)
VALUES
    ('ADJ90002', 'T. Nguyen', 20000.00, 'JPARK', NOW()),
    ('ADJ90003', 'R. Patel', 30000.00, 'JPARK', NOW())
ON CONFLICT (adjuster_id) DO UPDATE SET
    adjuster_name = EXCLUDED.adjuster_name,
    authority_limit = EXCLUDED.authority_limit;

INSERT INTO claim (claim_nbr, pol_nbr, cust_id, loss_date, claim_type, claim_status, assigned_adjuster_id, crt_user, crt_timestamp)
VALUES
    ('CLM000004900', 'POL000004001', 100001, DATE '2026-07-20', 'AUT', 'O', 'ADJ90002', 'JPARK', NOW()),
    ('CLM000004901', 'POL000004002', 30001, DATE '2026-05-14', 'PRP', 'C', 'ADJ90001', 'JPARK', NOW()),
    ('CLM000004902', 'POL000004003', 30001, DATE '2026-07-02', 'AUT', 'O', 'ADJ90002', 'JPARK', NOW()),
    ('CLM000004903', 'POL000004004', 30002, DATE '2026-06-18', 'PRP', 'O', 'ADJ90003', 'JPARK', NOW()),
    ('CLM000004904', 'POL000004006', 30003, DATE '2026-04-10', 'GL',  'O', 'ADJ90001', 'JPARK', NOW()),
    ('CLM000004905', 'POL000004007', 30003, DATE '2026-03-22', 'AUT', 'C', 'ADJ90002', 'JPARK', NOW()),
    ('CLM000004906', 'POL000004008', 30004, DATE '2026-05-02', 'FIR', 'O', 'ADJ90003', 'JPARK', NOW()),
    ('CLM000004907', 'POL000004010', 30005, DATE '2026-02-14', 'PRP', 'C', 'ADJ90001', 'JPARK', NOW()),
    ('CLM000004908', 'POL000004011', 30005, DATE '2026-07-28', 'AUT', 'O', 'ADJ90002', 'JPARK', NOW()),
    ('CLM000004909', 'POL000004012', 30006, DATE '2026-06-05', 'AUT', 'O', 'ADJ90003', 'JPARK', NOW()),
    ('CLM000004910', 'POL000004002', 30001, DATE '2026-01-09', 'PRP', 'C', 'ADJ90001', 'JPARK', NOW()),
    ('CLM000004911', 'POL000004006', 30003, DATE '2026-07-15', 'GL',  'O', 'ADJ90002', 'JPARK', NOW());

INSERT INTO claim_reserve (claim_nbr, reserve_type, approved_amt, paid_to_date, reserve_status, crt_user, crt_timestamp)
VALUES
    ('CLM000004900', 'COL', 4200.00,  0.00,     'O', 'JPARK', NOW()),
    ('CLM000004901', 'PRO', 9800.00,  9800.00,  'C', 'JPARK', NOW()),
    ('CLM000004902', 'COL', 3100.00,  0.00,     'O', 'JPARK', NOW()),
    ('CLM000004903', 'PRO', 5600.00,  2000.00,  'O', 'JPARK', NOW()),
    ('CLM000004904', 'LIA', 85000.00, 25000.00, 'O', 'JPARK', NOW()),
    ('CLM000004905', 'COL', 18500.00, 18500.00, 'C', 'JPARK', NOW()),
    ('CLM000004906', 'PRO', 62000.00, 15000.00, 'O', 'JPARK', NOW()),
    ('CLM000004907', 'PRO', 8200.00,  8200.00,  'C', 'JPARK', NOW()),
    ('CLM000004908', 'COL', 12000.00, 0.00,     'O', 'JPARK', NOW()),
    ('CLM000004909', 'COL', 4800.00,  0.00,     'O', 'JPARK', NOW()),
    ('CLM000004910', 'PRO', 0.00,     0.00,     'C', 'JPARK', NOW()),
    ('CLM000004911', 'LIA', 15000.00, 0.00,     'O', 'JPARK', NOW());

INSERT INTO claim_reserve_ledger (claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT 'CLM000004903', reserve_id, DATE '2026-06-18', 'Initial FNOL reserve — wind damage to roof and fence', 5600.00, 5600.00, 'ADJ90003', 'SET', TIMESTAMP '2026-06-18 08:40:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004903';
INSERT INTO claim_reserve_ledger (claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT 'CLM000004903', reserve_id, DATE '2026-07-05', 'Partial payment — roof tarp and fence repair', -2000.00, 3600.00, 'ADJ90003', 'DRAW', TIMESTAMP '2026-07-05 11:00:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004903';

INSERT INTO claim_reserve_ledger (claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT 'CLM000004904', reserve_id, DATE '2026-04-10', 'Initial FNOL reserve — slip and fall liability', 85000.00, 85000.00, 'ADJ90001', 'SET', TIMESTAMP '2026-04-10 15:20:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004904';
INSERT INTO claim_reserve_ledger (claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT 'CLM000004904', reserve_id, DATE '2026-06-01', 'Initial settlement payment to claimant', -25000.00, 60000.00, 'ADJ90001', 'DRAW', TIMESTAMP '2026-06-01 09:00:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004904';

INSERT INTO claim_reserve_ledger (claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT 'CLM000004906', reserve_id, DATE '2026-05-02', 'Initial FNOL reserve — kitchen fire, smoke damage throughout first floor', 62000.00, 62000.00, 'ADJ90003', 'SET', TIMESTAMP '2026-05-02 13:10:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004906';
INSERT INTO claim_reserve_ledger (claim_nbr, reserve_id, event_date, reason, amount, balance_after, actor_id, event_type, crt_timestamp)
SELECT 'CLM000004906', reserve_id, DATE '2026-06-10', 'Emergency mitigation and smoke remediation vendor payment', -15000.00, 47000.00, 'ADJ90003', 'DRAW', TIMESTAMP '2026-06-10 10:45:00'
FROM claim_reserve WHERE claim_nbr = 'CLM000004906';

INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004901', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-05-30 12:00:00', 'JPARK', NOW() FROM claim_reserve WHERE claim_nbr = 'CLM000004901';
INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004903', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-07-05 11:00:00', 'JPARK', NOW() FROM claim_reserve WHERE claim_nbr = 'CLM000004903';
INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004904', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-06-01 09:00:00', 'JPARK', NOW() FROM claim_reserve WHERE claim_nbr = 'CLM000004904';
INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004905', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-04-05 09:00:00', 'JPARK', NOW() FROM claim_reserve WHERE claim_nbr = 'CLM000004905';
INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004906', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-06-10 10:45:00', 'JPARK', NOW() FROM claim_reserve WHERE claim_nbr = 'CLM000004906';
INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004907', reserve_id, 'SUP90001', 'A', TIMESTAMP '2026-03-01 09:00:00', 'JPARK', NOW() FROM claim_reserve WHERE claim_nbr = 'CLM000004907';
INSERT INTO approval (claim_nbr, reserve_id, approver_id, approval_status, approval_date, crt_user, crt_timestamp)
SELECT 'CLM000004910', reserve_id, 'SUP90001', 'D', TIMESTAMP '2026-01-25 09:00:00', 'JPARK', NOW() FROM claim_reserve WHERE claim_nbr = 'CLM000004910';

INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT 'CLM000004901', 9800.00, 'P', 30001, a.approval_id, 'ADJ90001', 'JPARK', NOW() FROM approval a WHERE a.claim_nbr = 'CLM000004901';
INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT 'CLM000004903', 2000.00, 'P', 30002, a.approval_id, 'ADJ90003', 'JPARK', NOW() FROM approval a WHERE a.claim_nbr = 'CLM000004903';
INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT 'CLM000004904', 25000.00, 'P', 30003, a.approval_id, 'ADJ90001', 'JPARK', NOW() FROM approval a WHERE a.claim_nbr = 'CLM000004904';
INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT 'CLM000004905', 18500.00, 'P', 30003, a.approval_id, 'ADJ90002', 'JPARK', NOW() FROM approval a WHERE a.claim_nbr = 'CLM000004905';
INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT 'CLM000004906', 15000.00, 'P', 30004, a.approval_id, 'ADJ90003', 'JPARK', NOW() FROM approval a WHERE a.claim_nbr = 'CLM000004906';
INSERT INTO claim_payment (claim_nbr, payment_amt, payment_status, payee_id, approval_id, adjuster_id, crt_user, crt_timestamp)
SELECT 'CLM000004907', 8200.00, 'P', 30005, a.approval_id, 'ADJ90001', 'JPARK', NOW() FROM approval a WHERE a.claim_nbr = 'CLM000004907';

INSERT INTO claim_note (claim_nbr, note_text, crt_user, crt_timestamp)
VALUES
    ('CLM000004900', 'Rear-end collision at intersection; other driver cited. Estimate pending body shop inspection.', 'JPARK', TIMESTAMP '2026-07-20 09:00:00'),
    ('CLM000004901', 'Basement flooding from sump pump failure; mitigation and drywall replacement completed.', 'JPARK', TIMESTAMP '2026-05-14 10:00:00'),
    ('CLM000004902', 'Side-swipe in parking lot; bodywork estimate submitted by insured''s shop.', 'JPARK', TIMESTAMP '2026-07-02 14:20:00'),
    ('CLM000004903', 'Wind damage to roof shingles and rear fence during storm; tarp installed pending full repair.', 'JPARK', TIMESTAMP '2026-06-18 08:40:00'),
    ('CLM000004904', 'Customer visiting Riverside Auto Group dealership slipped on wet floor near service bay; ER visit for fractured wrist reported by claimant.', 'JPARK', TIMESTAMP '2026-04-10 15:20:00'),
    ('CLM000004905', 'Fleet delivery van totaled in highway collision; total loss settlement issued.', 'JPARK', TIMESTAMP '2026-03-22 11:00:00'),
    ('CLM000004906', 'Grease fire originated in kitchen; extensive smoke damage to first floor. Restoration contractor scoping repairs.', 'JPARK', TIMESTAMP '2026-05-02 13:10:00'),
    ('CLM000004907', 'Jewelry and electronics theft reported after break-in; police report filed, itemized loss list submitted.', 'JPARK', TIMESTAMP '2026-02-14 09:30:00'),
    ('CLM000004908', 'Struck by uninsured motorist while stopped at red light; insured seeking medical treatment for whiplash.', 'JPARK', TIMESTAMP '2026-07-28 16:00:00'),
    ('CLM000004909', 'Hailstorm caused windshield cracking and body dents; estimate from insured''s preferred shop pending.', 'JPARK', TIMESTAMP '2026-06-05 12:00:00'),
    ('CLM000004910', 'Claimed roof leak found to be pre-existing wear and tear, outside coverage per inspection; claim denied.', 'JPARK', TIMESTAMP '2026-01-25 09:00:00'),
    ('CLM000004911', 'Customer vehicle damaged by falling shop equipment during service; liability claim opened pending investigation.', 'JPARK', TIMESTAMP '2026-07-15 10:30:00');
