-- V1: roles table (SEC domain)
CREATE TABLE roles (
    role_id BIGINT GENERATED ALWAYS AS IDENTITY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    crt_user VARCHAR(10) NOT NULL,
    crt_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user VARCHAR(10) NOT NULL,
    upd_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id),
    CONSTRAINT uq_roles_role_code UNIQUE (role_code)
);
