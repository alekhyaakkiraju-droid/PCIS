-- Claims domain shadow write cleanup (WO-217)
DELETE FROM claim_payment WHERE crt_user = 'SHADOW_SYNC';
DELETE FROM claim WHERE crt_user = 'SHADOW_SYNC';
