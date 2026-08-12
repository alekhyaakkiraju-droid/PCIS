-- Demo customers for wireframe Customer 360 (Marta Field 19284, Alice Johnson 100001).
-- Idempotent repeatable migration for local and integration demos.

DELETE FROM customer_contact WHERE cust_id IN (19284, 100001);
DELETE FROM customer_address WHERE cust_id IN (19284, 100001);
DELETE FROM customer WHERE cust_id IN (19284, 100001);

INSERT INTO customer (cust_id, tax_id, cust_name, cust_type, cust_status, crt_user, crt_timestamp)
OVERRIDING SYSTEM VALUE
VALUES
    (19284, '512444821', 'Marta Field', 'B', 'A', 'DEMO', NOW()),
    (100001, '987654321', 'Alice Johnson', 'I', 'A', 'DEMO', NOW());

INSERT INTO customer_address (cust_id, address_line1, address_line2, city, state_code, zip_code, addr_type, crt_user, crt_timestamp)
VALUES
    (19284, '228 Larkspur Lane', NULL, 'Ann Arbor', 'MI', '48104', 'PRM', 'DEMO', NOW()),
    (19284, 'PO Box 8841', NULL, 'Ann Arbor', 'MI', '48105', 'BIL', 'DEMO', NOW()),
    (100001, '100 Main St', NULL, 'Austin', 'TX', '78701', 'PRM', 'DEMO', NOW());

INSERT INTO customer_contact (cust_id, first_name, last_name, phone_nbr, email_addr, contact_type, crt_user, crt_timestamp)
VALUES
    (19284, 'Marta', 'Field', '734-555-8842', 'marta.field@example.com', 'OWN', 'DEMO', NOW()),
    (100001, 'Alice', 'Johnson', '5125550100', 'alice.johnson@example.com', 'PRM', 'DEMO', NOW());

SELECT setval(
    pg_get_serial_sequence('customer', 'cust_id'),
    GREATEST((SELECT MAX(cust_id) FROM customer), 100001)
);
