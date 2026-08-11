-- WO-237: Verify RPT_RUN_LOG_T timing columns for instrumented batch programs.
-- Returns the most recent run per program with wall-clock duration in seconds.
-- Zero rows means no instrumented runs have been recorded yet.

SELECT
    PGM_NAME,
    RUN_DATE,
    START_TIMESTAMP,
    END_TIMESTAMP,
    TIMESTAMPDIFF(2, CHAR(END_TIMESTAMP - START_TIMESTAMP)) AS DURATION_SECONDS,
    REC_SELECTED,
    REC_UPDATED,
    REC_ERRORS,
    REC_DELINQUENT
FROM (
    SELECT
        R.*,
        ROW_NUMBER() OVER (
            PARTITION BY PGM_NAME
            ORDER BY END_TIMESTAMP DESC, RUN_LOG_ID DESC
        ) AS RN
    FROM RPT_RUN_LOG_T R
    WHERE PGM_NAME IN (
        'AUD002B',
        'BIL003B',
        'CLM006B',
        'CMM001B',
        'POL006B',
        'PRM005B'
    )
) AS RANKED
WHERE RN = 1
ORDER BY PGM_NAME;
