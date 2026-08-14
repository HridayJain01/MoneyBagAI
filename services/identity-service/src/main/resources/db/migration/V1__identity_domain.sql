-- Identity and Access schema.
-- Owns users, roles, permissions and opaque sessions. Employee and branch are
-- denormalised onto users so the gateway can resolve a session in ONE call
-- rather than chaining identity -> branch-employee on every request.

CREATE TABLE users (
    user_id             BIGINT       NOT NULL AUTO_INCREMENT,
    username            VARCHAR(80)  NOT NULL,
    email               VARCHAR(150) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(150) NOT NULL,
    mobile              VARCHAR(20),
    status              VARCHAR(20)  NOT NULL,
    failed_attempts     INT          NOT NULL DEFAULT 0,
    -- Logical references to branch-employee-service. Denormalised deliberately.
    employee_id         VARCHAR(64)  NULL,
    branch_code         VARCHAR(20)  NULL,
    last_login_at       DATETIME(6),
    password_changed_at DATETIME(6),
    locked_until        DATETIME(6),
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    KEY idx_users_employee (employee_id),
    KEY idx_users_branch (branch_code),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','LOCKED','DISABLED'))
) ENGINE = InnoDB;

CREATE TABLE roles (
    role_id     BIGINT      NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (role_id),
    CONSTRAINT uk_roles_name UNIQUE (role_name)
) ENGINE = InnoDB;

CREATE TABLE permissions (
    permission_id   BIGINT      NOT NULL AUTO_INCREMENT,
    permission_code VARCHAR(60) NOT NULL,
    description     VARCHAR(255),
    service_name    VARCHAR(60),
    action          VARCHAR(20),
    PRIMARY KEY (permission_id),
    CONSTRAINT uk_permissions_code UNIQUE (permission_code)
) ENGINE = InnoDB;

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id),
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(permission_id)
) ENGINE = InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id)
) ENGINE = InnoDB;

-- VARCHAR(36) rather than BINARY(16): the session id travels as a bearer string,
-- so hex round-tripping on every gateway hop would be pure friction. VARCHAR over
-- CHAR because CHAR's trailing-space semantics buy nothing for a fixed-width UUID
-- and Hibernate's schema validator expects VARCHAR for a plain String field.
CREATE TABLE user_sessions (
    session_id         VARCHAR(36)  NOT NULL,
    user_id            BIGINT       NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    issued_at          DATETIME(6)  NOT NULL,
    last_activity_at   DATETIME(6)  NOT NULL,
    expires_at         DATETIME(6)  NOT NULL,
    revoked_at         DATETIME(6),
    ip_address         VARCHAR(45),
    user_agent         VARCHAR(255),
    PRIMARY KEY (session_id),
    KEY idx_sessions_user_status (user_id, status),
    KEY idx_sessions_expiry (expires_at),
    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_sessions_status CHECK (status IN ('ACTIVE','REVOKED','EXPIRED'))
) ENGINE = InnoDB;

CREATE TABLE login_audit (
    audit_id       BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NULL,
    username       VARCHAR(80) NOT NULL,
    event_type     VARCHAR(30) NOT NULL,
    event_time     DATETIME(6) NOT NULL,
    ip_address     VARCHAR(45),
    device_info    VARCHAR(255),
    failure_reason VARCHAR(255),
    outcome        VARCHAR(20) NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_login_audit_user_time (user_id, event_time),
    KEY idx_login_audit_username (username)
) ENGINE = InnoDB;
