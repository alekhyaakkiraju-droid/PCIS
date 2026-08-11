-- WO-171: immutable purge evidence and S3 archive export registry

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'purge_type') THEN
        CREATE TYPE purge_type AS ENUM ('PARTITION_DROP', 'S3_KEY_DESTROY');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS purge_evidence (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purge_type purge_type NOT NULL,
    target_identifier VARCHAR(256) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    retention_days INTEGER NOT NULL,
    purge_timestamp TIMESTAMPTZ NOT NULL,
    actor VARCHAR(10) NOT NULL,
    evidence_hash VARCHAR(64) NOT NULL,
    scheduled_deletion_at TIMESTAMPTZ,
    crt_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_purge_evidence_timestamp ON purge_evidence (purge_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_purge_evidence_target ON purge_evidence (target_identifier);

COMMENT ON TABLE purge_evidence IS
    'Immutable record of all audit data purge operations. No UPDATE or DELETE permitted.';

CREATE TABLE IF NOT EXISTS audit_archive_export_t (
    export_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    s3_bucket VARCHAR(256) NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    kms_key_arn VARCHAR(512) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    partition_name VARCHAR(128),
    exported_at TIMESTAMPTZ NOT NULL,
    retention_days INTEGER NOT NULL,
    purge_scheduled BOOLEAN NOT NULL DEFAULT FALSE,
    crt_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_archive_export_exported ON audit_archive_export_t (exported_at);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'pcis_app_role') THEN
        CREATE ROLE pcis_app_role NOLOGIN;
    END IF;
END $$;

GRANT SELECT, INSERT ON purge_evidence TO pcis_app_role;
GRANT SELECT, INSERT, UPDATE ON audit_archive_export_t TO pcis_app_role;
REVOKE UPDATE, DELETE ON purge_evidence FROM pcis_app_role;
REVOKE DELETE ON purge_evidence FROM PUBLIC;
