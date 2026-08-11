-- scenario-02: multiple reserves including one above typical authority limit
INSERT INTO CLAIM_RESERVE_T (CLAIM_ID, RESERVE_ID, RESERVE_STATUS, RESERVE_AMT, AUTHORITY_LIMIT) VALUES
 ('CLM0002001', 'RSV001', 'AP', 2500.00, 5000.00),
 ('CLM0002002', 'RSV002', 'AP', 75000.00, 10000.00),  -- exceeds typical authority
 ('CLM0002003', 'RSV003', 'AP', 500.00, 5000.00);
