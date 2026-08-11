DELETE FROM reconciliation_break;
DELETE FROM reconciliation_run_summary;
DELETE FROM legacy_snapshot.billing_schedule_snapshot;
DELETE FROM billing_schedule_t;

INSERT INTO legacy_snapshot.billing_schedule_snapshot
    (pol_nbr, installment_nbr, amt_due, sched_status, business_date)
VALUES
    ('POLRECON01', 1, 50.00, 'O', DATE '2026-08-11'),
    ('POLRECON01', 2, 50.00, 'O', DATE '2026-08-11');

INSERT INTO billing_schedule_t
    (pol_nbr, bill_plan_id, installment_nbr, due_date, amt_due, sched_status, crt_user)
VALUES
    ('POLRECON01', 1, 1, DATE '2026-08-01', 50.00, 'O', 'TEST'),
    ('POLRECON01', 1, 2, DATE '2026-09-01', 50.00, 'O', 'TEST');
