-- WO-174: Delinquency transition rule set (CONFIG_RULE_SET_T from WO-173)

INSERT INTO config_rule_set_t (
    rule_set_key, version_no, payload, description, effective_from, status_cd, crt_user, crt_timestamp
) VALUES (
    'delinquency-status-transition',
    1,
    '{
      "transitions": [
        {"fromStatus": "O", "event": "LATE", "toStatus": "L"},
        {"fromStatus": "L", "event": "DELINQUENT", "toStatus": "D"},
        {"fromStatus": "D", "event": "PAID", "toStatus": "P"},
        {"fromStatus": "O", "event": "PAID", "toStatus": "P"},
        {"fromStatus": "O", "event": "VOID", "toStatus": "V"}
      ]
    }'::jsonb,
    'Billing schedule delinquency status transitions',
    CURRENT_DATE,
    'A',
    'SYSTEM',
    CURRENT_TIMESTAMP
);
