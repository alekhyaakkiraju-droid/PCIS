-- Billing domain shadow write cleanup (WO-217)
DELETE FROM payment_application_t WHERE crt_user = 'SHADOW_SYNC';
DELETE FROM invoice_line_t WHERE crt_user = 'SHADOW_SYNC';
DELETE FROM invoice_t WHERE crt_user = 'SHADOW_SYNC';
DELETE FROM billing_schedule_t WHERE crt_user = 'SHADOW_SYNC';
DELETE FROM billing_plan_t WHERE crt_user = 'SHADOW_SYNC';
