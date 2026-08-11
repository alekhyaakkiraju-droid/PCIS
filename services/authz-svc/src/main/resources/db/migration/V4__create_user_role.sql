-- V4: user_role assignment table (SEC domain)
CREATE TABLE user_role (
    user_role_id BIGINT GENERATED ALWAYS AS IDENTITY,
    principal_id VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    crt_user VARCHAR(10) NOT NULL,
    crt_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user VARCHAR(10) NOT NULL,
    upd_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_role_id),
    CONSTRAINT uq_user_role_principal_role UNIQUE (principal_id, role_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles (role_id)
);

CREATE INDEX idx_user_role_principal_id ON user_role (principal_id);
