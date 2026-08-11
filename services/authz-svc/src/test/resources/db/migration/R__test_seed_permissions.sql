-- Test seed: permissions and role_permission mappings for integration tests
INSERT INTO permissions (permission_code, resource, operation, description, crt_user, crt_timestamp, upd_user, upd_timestamp)
VALUES
    ('claim:read', 'claim', 'read', 'Read claim records', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('claim:pay', 'claim', 'pay', 'Authorize claim payment', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('customer:read', 'customer', 'read', 'Read customer records', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('customer:write', 'customer', 'write', 'Mutate customer records', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP),
    ('batch:execute', 'batch', 'execute', 'Execute batch jobs', 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP)
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id, crt_user, crt_timestamp, upd_user, upd_timestamp)
SELECT r.role_id, p.permission_id, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP
FROM roles r
JOIN permissions p ON (
    (r.role_code = 'ADJUSTER' AND p.permission_code IN ('claim:read', 'claim:pay'))
    OR (r.role_code = 'SUPERVISOR' AND p.permission_code IN ('claim:read', 'claim:pay', 'customer:read'))
    OR (r.role_code = 'CSR' AND p.permission_code IN ('customer:read', 'customer:write'))
    OR (r.role_code = 'BATCH' AND p.permission_code = 'batch:execute')
)
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO user_role (principal_id, role_id, crt_user, crt_timestamp, upd_user, upd_timestamp)
SELECT seed.principal_id, r.role_id, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP
FROM (
    VALUES
        ('adjuster-001', 'ADJUSTER'),
        ('supervisor-001', 'SUPERVISOR'),
        ('csr-001', 'CSR'),
        ('batch-renewal', 'BATCH')
) AS seed(principal_id, role_code)
JOIN roles r ON r.role_code = seed.role_code
ON CONFLICT (principal_id, role_id) DO NOTHING;
