-- PCIS Customer Domain V2 Schema Migration
CREATE TABLE IF NOT EXISTS customer (
    cust_id       INTEGER GENERATED ALWAYS AS IDENTITY,
    tax_id        VARCHAR(11),
    cust_name     VARCHAR(60)  NOT NULL,
    cust_type     VARCHAR(1)   NOT NULL,
    cust_status   VARCHAR(1)   NOT NULL DEFAULT 'A',
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_customer PRIMARY KEY (cust_id),
    CONSTRAINT chk_customer_type CHECK (cust_type IN ('I', 'B')),
    CONSTRAINT chk_customer_status CHECK (cust_status IN ('A', 'I', 'S'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_tax_id ON customer (tax_id) WHERE tax_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_customer_status ON customer (cust_status);
CREATE INDEX IF NOT EXISTS idx_customer_name ON customer (cust_name);

CREATE TABLE IF NOT EXISTS customer_address (
    addr_id       BIGINT GENERATED ALWAYS AS IDENTITY,
    cust_id       INTEGER      NOT NULL,
    address_line1 VARCHAR(40)  NOT NULL,
    address_line2 VARCHAR(40),
    city          VARCHAR(30)  NOT NULL,
    state_code    VARCHAR(2)   NOT NULL,
    zip_code      VARCHAR(10)  NOT NULL,
    addr_type     VARCHAR(3)   NOT NULL DEFAULT 'PRM',
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_customer_address PRIMARY KEY (addr_id),
    CONSTRAINT fk_customer_address_cust FOREIGN KEY (cust_id) REFERENCES customer (cust_id)
);
CREATE INDEX IF NOT EXISTS idx_customer_address_cust_id ON customer_address (cust_id);

CREATE TABLE IF NOT EXISTS customer_contact (
    contact_id    BIGINT GENERATED ALWAYS AS IDENTITY,
    cust_id       INTEGER      NOT NULL,
    first_name    VARCHAR(30)  NOT NULL,
    last_name     VARCHAR(30)  NOT NULL,
    phone_nbr     VARCHAR(20),
    email_addr    VARCHAR(100),
    contact_type  VARCHAR(3),
    crt_user      VARCHAR(10)  NOT NULL,
    crt_timestamp TIMESTAMP    NOT NULL DEFAULT NOW(),
    upd_user      VARCHAR(10),
    upd_timestamp TIMESTAMP,
    CONSTRAINT pk_customer_contact PRIMARY KEY (contact_id),
    CONSTRAINT fk_customer_contact_cust FOREIGN KEY (cust_id) REFERENCES customer (cust_id)
);
CREATE INDEX IF NOT EXISTS idx_customer_contact_cust_id ON customer_contact (cust_id);

CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    VARCHAR(50)  NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    payload         TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished ON outbox_events (created_at) WHERE published = FALSE;
