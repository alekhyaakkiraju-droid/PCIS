-- Extended demo customer set (6 new customers, 30001-30006) alongside the original
-- Customer 360 demo customers (19284, 100001). Idempotent repeatable migration.

DELETE FROM customer_contact WHERE cust_id IN (30001, 30002, 30003, 30004, 30005, 30006);
DELETE FROM customer_address WHERE cust_id IN (30001, 30002, 30003, 30004, 30005, 30006);
DELETE FROM customer WHERE cust_id IN (30001, 30002, 30003, 30004, 30005, 30006);

INSERT INTO customer (cust_id, tax_id, cust_name, cust_type, cust_status, crt_user, crt_timestamp)
OVERRIDING SYSTEM VALUE
VALUES
    (30001, '410221190', 'David Chen', 'I', 'A', 'JPARK', NOW()),
    (30002, '410221191', 'Sarah Martinez', 'I', 'A', 'JPARK', NOW()),
    (30003, '410221192', 'Riverside Auto Group LLC', 'B', 'A', 'JPARK', NOW()),
    (30004, '410221193', 'James Okafor', 'I', 'A', 'JPARK', NOW()),
    (30005, '410221194', 'Priya Sharma', 'I', 'A', 'JPARK', NOW()),
    (30006, '410221195', 'Nora Kim', 'I', 'A', 'JPARK', NOW());

INSERT INTO customer_address (cust_id, address_line1, address_line2, city, state_code, zip_code, addr_type, crt_user, crt_timestamp)
VALUES
    (30001, '412 Birchwood Ave', NULL, 'Grand Rapids', 'MI', '49503', 'PRM', 'JPARK', NOW()),
    (30002, '77 Sunset Blvd', 'Apt 4B', 'Tucson', 'AZ', '85701', 'PRM', 'JPARK', NOW()),
    (30003, '900 Industrial Pkwy', NULL, 'Columbus', 'OH', '43215', 'PRM', 'JPARK', NOW()),
    (30004, '58 Maplewood Dr', NULL, 'Charlotte', 'NC', '28202', 'PRM', 'JPARK', NOW()),
    (30005, '215 Cedar Ridge Rd', NULL, 'Naperville', 'IL', '60540', 'PRM', 'JPARK', NOW()),
    (30006, '33 Harbor View Ct', NULL, 'Tacoma', 'WA', '98402', 'PRM', 'JPARK', NOW());

INSERT INTO customer_contact (cust_id, first_name, last_name, phone_nbr, email_addr, contact_type, crt_user, crt_timestamp)
VALUES
    (30001, 'David', 'Chen', '616-555-0142', 'david.chen@example.com', 'OWN', 'JPARK', NOW()),
    (30002, 'Sarah', 'Martinez', '520-555-0198', 'sarah.martinez@example.com', 'OWN', 'JPARK', NOW()),
    (30003, 'Marcus', 'Diallo', '614-555-0173', 'ops@riversideautogroup.example.com', 'PRM', 'JPARK', NOW()),
    (30004, 'James', 'Okafor', '704-555-0156', 'james.okafor@example.com', 'OWN', 'JPARK', NOW()),
    (30005, 'Priya', 'Sharma', '630-555-0187', 'priya.sharma@example.com', 'OWN', 'JPARK', NOW()),
    (30006, 'Nora', 'Kim', '253-555-0164', 'nora.kim@example.com', 'OWN', 'JPARK', NOW());

SELECT setval(
    pg_get_serial_sequence('customer', 'cust_id'),
    GREATEST((SELECT MAX(cust_id) FROM customer), 30006)
);
