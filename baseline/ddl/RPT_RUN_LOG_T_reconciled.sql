-- =============================================================================
-- RPT_RUN_LOG_T — reconciled authoritative DDL (WO-237)
-- =============================================================================
-- DRIFT DOCUMENTATION
--
-- Prior PCIS_Database_Design.md defined RPT_RUN_LOG_T with columns that do not
-- match batch instrumentation needs or any shipped COBOL INSERT:
--   RUN_ID, PROGRAM_NAME, RUN_DATE, ROWS_PROCESSED, RUN_STATUS,
--   CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP
--
-- Shipped COBOL batch programs (AUD002B, BIL003B, CLM006B, CMM001B, POL006B,
-- PRM005B) previously had NO 8000-WRITE-RUN-LOG paragraph and performed NO
-- INSERT into RPT_RUN_LOG_T — they only DISPLAY completion counters at finalize.
--
-- This DDL is the authoritative target schema for Phase 0 batch-window timing:
--   RUN_LOG_ID (generated identity PK)
--   PGM_NAME / RUN_DATE / REC_SELECTED / REC_UPDATED / REC_ERRORS
--   REC_DELINQUENT (nullable; used only by PRM005B)
--   START_TIMESTAMP / END_TIMESTAMP (TIMESTAMP(6) wall-clock timing)
--   CRT_TIMESTAMP
--
-- Apply this DDL before compiling/promoting the instrumented batch programs.
-- =============================================================================

CREATE TABLE RPT_RUN_LOG_T (
    RUN_LOG_ID        BIGINT GENERATED ALWAYS AS IDENTITY
                      (START WITH 1 INCREMENT BY 1)
                      PRIMARY KEY,
    PGM_NAME          VARCHAR(10)     NOT NULL,
    RUN_DATE          DATE            NOT NULL,
    REC_SELECTED      INTEGER         NOT NULL DEFAULT 0,
    REC_UPDATED       INTEGER         NOT NULL DEFAULT 0,
    REC_ERRORS        INTEGER         NOT NULL DEFAULT 0,
    REC_DELINQUENT    INTEGER,
    START_TIMESTAMP   TIMESTAMP(6)    NOT NULL,
    END_TIMESTAMP     TIMESTAMP(6)    NOT NULL,
    CRT_TIMESTAMP     TIMESTAMP       NOT NULL DEFAULT CURRENT TIMESTAMP
);
