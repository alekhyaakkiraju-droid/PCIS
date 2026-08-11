-- V3: role_permission join table (SEC domain)
CREATE TABLE role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    crt_user VARCHAR(10) NOT NULL,
    crt_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upd_user VARCHAR(10) NOT NULL,
    upd_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
);

CREATE INDEX idx_role_permission_permission_id ON role_permission (permission_id);
