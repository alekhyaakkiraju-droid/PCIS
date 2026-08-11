-- single-reserve-payment: one AP reserve below authority limit → paid, status PD
INSERT INTO CLAIM_RESERVE_T (CLAIM_ID, RESERVE_ID, RESERVE_STATUS, RESERVE_AMT, AUTHORITY_LIMIT)
VALUES ('CLM0001001', 'RSV001', 'AP', 1500.00, 5000.00);
