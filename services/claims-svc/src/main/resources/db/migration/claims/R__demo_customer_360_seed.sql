-- Customer 360 claims summary points at CLM000004821 (primary open claim for cust 19284).

DELETE FROM claim_payment WHERE claim_nbr = 'CLM000000001';
DELETE FROM approval WHERE claim_nbr = 'CLM000000001';
DELETE FROM claim_note WHERE claim_nbr = 'CLM000000001';
DELETE FROM claim_reserve WHERE claim_nbr = 'CLM000000001';
DELETE FROM claim WHERE claim_nbr = 'CLM000000001';
