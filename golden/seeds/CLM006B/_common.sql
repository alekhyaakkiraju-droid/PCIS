-- Common truncate/setup for CLM006B golden seeds
-- Target: Db2 for i (compatible subset also runs on PostgreSQL for CI seed validation)
DELETE FROM CLAIM_PAYMENT_T;
DELETE FROM CLAIM_RESERVE_T;
DELETE FROM RPT_RUN_LOG_T WHERE PROGRAM_NAME = 'CLM006B';
