-- WO-205: batch run-log enablement tunable

INSERT INTO config_tunable_t (
    tunable_key, domain_cd, value_type, value_text, description,
    effective_from, version_no, crt_user, crt_timestamp
) VALUES (
    'batch.runLog.enabled',
    'BAT',
    'B',
    'true',
    'Enable RPT_RUN_LOG_T writes at batch job finalize',
    CURRENT_DATE,
    1,
    'SYSTEM',
    CURRENT_TIMESTAMP
);
