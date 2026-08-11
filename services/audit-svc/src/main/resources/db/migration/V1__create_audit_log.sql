-- V1: partitioned immutable audit_log table (audlog01-v1 unified schema)
-- Monthly range partitions on event_timestamp; application role is insert/select only.

CREATE TABLE audit_log (
    audit_log_id BIGINT GENERATED ALWAYS AS IDENTITY,
    action_cd VARCHAR(10) NOT NULL,
    old_value VARCHAR(100),
    new_value VARCHAR(100),
    key_value VARCHAR(40),
    field_name VARCHAR(30),
    correlation_id UUID NOT NULL,
    service_name VARCHAR(64) NOT NULL,
    program_name VARCHAR(10),
    actor VARCHAR(10) NOT NULL,
    resource_name VARCHAR(50) NOT NULL,
    operation VARCHAR(30) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (audit_log_id, event_timestamp)
) PARTITION BY RANGE (event_timestamp);

CREATE TABLE audit_log_default PARTITION OF audit_log DEFAULT;

CREATE TABLE audit_log_y2026m01 PARTITION OF audit_log
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE audit_log_y2026m02 PARTITION OF audit_log
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE audit_log_y2026m03 PARTITION OF audit_log
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE audit_log_y2026m04 PARTITION OF audit_log
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE audit_log_y2026m05 PARTITION OF audit_log
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE audit_log_y2026m06 PARTITION OF audit_log
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE audit_log_y2026m07 PARTITION OF audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE audit_log_y2026m08 PARTITION OF audit_log
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE audit_log_y2026m09 PARTITION OF audit_log
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE audit_log_y2026m10 PARTITION OF audit_log
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE audit_log_y2026m11 PARTITION OF audit_log
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE audit_log_y2026m12 PARTITION OF audit_log
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

CREATE INDEX idx_audit_log_correlation_id ON audit_log (correlation_id);
CREATE INDEX idx_audit_log_resource_key ON audit_log (resource_name, key_value);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pcis_audit_app') THEN
        CREATE ROLE pcis_audit_app LOGIN PASSWORD 'pcis_audit_app';
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO pcis_audit_app;
GRANT INSERT, SELECT ON audit_log TO pcis_audit_app;
REVOKE UPDATE, DELETE ON audit_log FROM pcis_audit_app;
REVOKE UPDATE, DELETE ON audit_log FROM PUBLIC;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    REVOKE UPDATE, DELETE ON TABLES FROM pcis_audit_app;
