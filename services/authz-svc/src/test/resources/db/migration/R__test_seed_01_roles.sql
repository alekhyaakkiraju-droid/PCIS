-- Test seed: roles covering adjuster, supervisor, CSR, batch, and unassigned principals
INSERT INTO roles (role_code, role_name, description, active, crt_user, crt_timestamp, upd_user, upd_timestamp)
VALUES
    ('ADJUSTER', 'Claims Adjuster', 'Field adjuster with limited claim authority', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('SUPERVISOR', 'Claims Supervisor', 'Supervisory claims authority', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('CSR', 'Customer Service Representative', 'Customer service read/write access', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('BATCH', 'Batch Operations', 'Batch service principal role', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('UNASSIGNED', 'Unassigned Principal', 'Placeholder for principals with no effective grants', TRUE, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP)
ON CONFLICT (role_code) DO NOTHING;
