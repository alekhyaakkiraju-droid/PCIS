-- Customer linkage for Customer 360 billing summary aggregation.

ALTER TABLE INVOICE_T
    ADD COLUMN IF NOT EXISTS CUST_ID INTEGER;

CREATE INDEX IF NOT EXISTS idx_invoice_t_cust_id ON INVOICE_T (CUST_ID);
