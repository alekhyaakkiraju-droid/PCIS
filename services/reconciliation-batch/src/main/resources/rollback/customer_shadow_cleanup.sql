-- Customer domain shadow write cleanup (WO-217)
DELETE FROM customer_t WHERE crt_user = 'SHADOW_SYNC';
