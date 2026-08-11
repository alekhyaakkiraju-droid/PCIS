-- remainder-loss: $1000.00 / 3 installments at scale-2 HALF_UP → $333.33 × 3 = $999.99
-- One cent is lost (not redistributed to the first installment).
INSERT INTO POLICY_T (POLICY_ID, BILLING_FREQ, ANNUAL_PREMIUM, STATUS)
VALUES ('POLBILREM', 'T', 1000.00, 'A');
