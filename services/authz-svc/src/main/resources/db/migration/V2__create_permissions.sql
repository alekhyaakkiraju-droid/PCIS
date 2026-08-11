-- V2: permissions table (SEC domain)
CREATE TABLE permissions (
    permission_id BIGINT GENERATED ALWAYS AS IDENTITY,
    permission_code VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    crt_user VARCHAR(10) NOT NULL,
    crt_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user VARCHAR(10) NOT NULL,
    upd_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (permission_id),
    CONSTRAINT uq_permissions_permission_code UNIQUE (permission_code),
    CONSTRAINT uq_permissions_resource_operation UNIQUE (resource, operation)
);
