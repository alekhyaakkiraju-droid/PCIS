-- Detached partitions at various ages for purge integration tests (WO-171)

DROP TABLE IF EXISTS audit_log_t_y2018m01;
DROP TABLE IF EXISTS audit_log_t_y2025m10;

CREATE TABLE audit_log_t_y2018m01 (
    log_id BIGINT GENERATED ALWAYS AS IDENTITY,
    program_name VARCHAR(10),
    action_code VARCHAR(10),
    table_name VARCHAR(30),
    record_key VARCHAR(40),
    user_id VARCHAR(10),
    log_timestamp TIMESTAMP,
    crt_timestamp TIMESTAMP NOT NULL DEFAULT TIMESTAMP '2018-01-15 00:00:00'
);

CREATE TABLE audit_log_t_y2025m10 (
    log_id BIGINT GENERATED ALWAYS AS IDENTITY,
    program_name VARCHAR(10),
    action_code VARCHAR(10),
    table_name VARCHAR(30),
    record_key VARCHAR(40),
    user_id VARCHAR(10),
    log_timestamp TIMESTAMP,
    crt_timestamp TIMESTAMP NOT NULL DEFAULT TIMESTAMP '2025-10-15 00:00:00'
);

DELETE FROM audit_archive_export_t;

INSERT INTO audit_archive_export_t (
    s3_bucket, s3_key, kms_key_arn, tier, partition_name, exported_at, retention_days, purge_scheduled)
VALUES
    ('pcis-audit-archive-test', 'archives/2018/part-001.parquet',
     'arn:aws:kms:us-east-1:123456789012:key/old-key', 'INTERNAL', 'audit_log_t_y2018m01',
     TIMESTAMPTZ '2018-06-01 00:00:00+00', 365, FALSE),
    ('pcis-audit-archive-test', 'archives/2025/part-recent.parquet',
     'arn:aws:kms:us-east-1:123456789012:key/recent-key', 'INTERNAL', 'audit_log_t_y2025m10',
     TIMESTAMPTZ '2025-10-20 00:00:00+00', 200, FALSE);
