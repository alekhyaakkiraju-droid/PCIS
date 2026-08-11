-- BIL003B known starting state (WO-176)
-- Reference date: 2024-06-15 — loadable into PostgreSQL / H2 for golden capture.
-- Multi-scenario seeds remain under golden/seeds/BIL003B/ (WO-148).

DELETE FROM BILLING_INSTALLMENT_T;
DELETE FROM POLICY_T WHERE POLICY_ID LIKE 'POLBIL%';
DELETE FROM RPT_RUN_LOG_T WHERE PROGRAM_NAME = 'BIL003B';

-- monthly even division: 1200.00 / 12 = 100.00
INSERT INTO POLICY_T (POLICY_ID, BILLING_FREQ, ANNUAL_PREMIUM, STATUS)
VALUES ('POLBIL0001', 'M', 1200.00, 'A');
