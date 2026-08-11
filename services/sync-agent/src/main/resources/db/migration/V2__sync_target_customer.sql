-- Target landing table for customer domain sync (PostgreSQL side)
CREATE TABLE sync_customer (
    cust_id       INTEGER      NOT NULL,
    cust_name     VARCHAR(60)  NOT NULL,
    cust_status   VARCHAR(1)   NOT NULL,
    upd_timestamp TIMESTAMP    NOT NULL,
    synced_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_sync_customer PRIMARY KEY (cust_id)
);

CREATE INDEX idx_sync_customer_upd_timestamp ON sync_customer (upd_timestamp);
