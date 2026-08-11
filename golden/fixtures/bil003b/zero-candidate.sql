-- zero-candidate: unrecognised billing frequency → zero installments generated
INSERT INTO POLICY_T (POLICY_ID, BILLING_FREQ, ANNUAL_PREMIUM, STATUS)
VALUES ('POLBILZER', NULL, 1800.00, 'A');
