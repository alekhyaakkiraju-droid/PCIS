-- PCIS V3 Data Classification Registry (WO-159)
-- Runtime table populated from config/pcis-data-classification.yaml at startup.

CREATE TABLE data_classification_tier (
    tier VARCHAR(20) PRIMARY KEY,
    retention_days INTEGER NOT NULL,
    storage_encryption VARCHAR(30) NOT NULL,
    access_control VARCHAR(30) NOT NULL,
    log_emission VARCHAR(30) NOT NULL,
    registry_version VARCHAR(20) NOT NULL,
    loaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE data_classification (
    entity_name VARCHAR(63) NOT NULL,
    column_name VARCHAR(63) NOT NULL,
    data_tier VARCHAR(20) NOT NULL,
    mask_strategy VARCHAR(30) NOT NULL,
    retention_days INTEGER NOT NULL,
    pii BOOLEAN NOT NULL DEFAULT FALSE,
    discriminator_column VARCHAR(63),
    rationale TEXT,
    registry_version VARCHAR(20) NOT NULL,
    loaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (entity_name, column_name),
    CONSTRAINT fk_data_classification_tier
        FOREIGN KEY (data_tier) REFERENCES data_classification_tier (tier)
);

CREATE INDEX idx_data_classification_data_tier ON data_classification (data_tier);

COMMENT ON TABLE data_classification IS
    'Column-level data classification loaded from pcis-data-classification.yaml (WO-159)';

COMMENT ON TABLE data_classification_tier IS
    'Per-tier handling rules (retention, encryption, access, log emission) from registry tier_handling section';
