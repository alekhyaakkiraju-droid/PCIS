INSERT INTO customer (tax_id, cust_name, cust_type, cust_status, crt_user, crt_timestamp)
VALUES ('123456789', 'Acme Insurance LLC', 'B', 'A', 'seed', NOW());
INSERT INTO customer (tax_id, cust_name, cust_type, cust_status, crt_user, crt_timestamp)
VALUES ('987654321', 'Jane Doe', 'I', 'A', 'seed', NOW());
INSERT INTO customer (tax_id, cust_name, cust_type, cust_status, crt_user, crt_timestamp)
VALUES ('555443333', 'Inactive Customer', 'I', 'I', 'seed', NOW());
INSERT INTO customer (cust_name, cust_type, cust_status, crt_user, crt_timestamp)
VALUES ('No Tax Customer', 'I', 'A', 'seed', NOW());
INSERT INTO customer_address (cust_id, address_line1, address_line2, city, state_code, zip_code, addr_type, crt_user, crt_timestamp)
SELECT cust_id, '100 Main St', NULL, 'Austin', 'TX', '78701', 'PRM', 'seed', NOW() FROM customer WHERE tax_id = '123456789';
INSERT INTO customer_contact (cust_id, first_name, last_name, phone_nbr, email_addr, contact_type, crt_user, crt_timestamp)
SELECT cust_id, 'Jane', 'Doe', '5125550100', 'jane.doe@example.com', 'PRM', 'seed', NOW() FROM customer WHERE tax_id = '987654321';
