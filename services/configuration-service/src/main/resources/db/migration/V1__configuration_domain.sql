-- Configuration schema, converted from the original Oracle model.
--
-- Boolean-ish columns are CHAR(1) 'Y'/'N' rather than BOOLEAN because the entities map
-- them as String with length=1; changing the Java type is out of scope for the port.
-- @Lob String maps to TEXT under MySQL.

CREATE TABLE config_entries (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    namespace      VARCHAR(50) NOT NULL,
    config_key     VARCHAR(100) NOT NULL,
    config_value   TEXT        NOT NULL,
    value_type     VARCHAR(20) NOT NULL,
    version        INT         NOT NULL,
    effective_from DATETIME(6) NOT NULL,
    effective_to   DATETIME(6),
    updated_by     BIGINT,
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    -- A namespace/key may be versioned over time, so uniqueness includes the version.
    CONSTRAINT uk_config_entry_version UNIQUE (namespace, config_key, version),
    KEY idx_config_entries_lookup (namespace, config_key, effective_from)
) ENGINE = InnoDB;

CREATE TABLE config_change_history (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(50)  NOT NULL,
    entity_key  VARCHAR(150) NOT NULL,
    old_value   TEXT,
    new_value   TEXT,
    changed_by  BIGINT,
    -- Entity maps this insertable=false/updatable=false, so the DB supplies it.
    changed_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_config_history_entity (entity_type, entity_key, changed_at)
) ENGINE = InnoDB;

CREATE TABLE channel_limits (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    channel        VARCHAR(20)   NOT NULL,
    limit_type     VARCHAR(20)   NOT NULL,
    max_amount     DECIMAL(18,2) NOT NULL,
    currency       VARCHAR(3)    NOT NULL,
    effective_from DATETIME(6)   NOT NULL,
    effective_to   DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_channel_limits_lookup (channel, limit_type, effective_from),
    CONSTRAINT chk_channel_limit_amount CHECK (max_amount >= 0)
) ENGINE = InnoDB;

CREATE TABLE feature_flags (
    flag_key       VARCHAR(255) NOT NULL,
    enabled        CHAR(1)      NOT NULL,
    description    VARCHAR(255),
    targeting_rule TEXT,
    updated_at     DATETIME(6),
    PRIMARY KEY (flag_key),
    CONSTRAINT chk_feature_flag_enabled CHECK (enabled IN ('Y','N'))
) ENGINE = InnoDB;

CREATE TABLE maintenance_windows (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    title     VARCHAR(200) NOT NULL,
    starts_at DATETIME(6)  NOT NULL,
    ends_at   DATETIME(6)  NOT NULL,
    status    VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_maintenance_window_span (starts_at, ends_at),
    CONSTRAINT chk_maintenance_window_span CHECK (ends_at > starts_at)
) ENGINE = InnoDB;

CREATE TABLE maker_checker_thresholds (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    action_type      VARCHAR(50)   NOT NULL,
    threshold_amount DECIMAL(18,2) NOT NULL,
    currency         VARCHAR(3)    NOT NULL,
    effective_from   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_maker_checker_lookup (action_type, effective_from),
    CONSTRAINT chk_maker_checker_amount CHECK (threshold_amount >= 0)
) ENGINE = InnoDB;

-- Policy tables are effective-dated: the newest effective_from wins, and older rows
-- are retained as history rather than updated in place.
CREATE TABLE password_policy (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    min_length      INT         NOT NULL,
    require_upper   CHAR(1),
    require_digit   CHAR(1),
    require_special CHAR(1),
    history_count   INT,
    max_age_days    INT,
    effective_from  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_password_policy_effective (effective_from)
) ENGINE = InnoDB;

CREATE TABLE session_policy (
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    idle_timeout_minutes     INT         NOT NULL,
    absolute_timeout_minutes INT         NOT NULL,
    max_concurrent_sessions  INT,
    effective_from           DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_session_policy_effective (effective_from)
) ENGINE = InnoDB;

CREATE TABLE otp_policy (
    id                      BIGINT      NOT NULL AUTO_INCREMENT,
    expiry_seconds          INT         NOT NULL,
    max_retries             INT         NOT NULL,
    resend_cooldown_seconds INT         NOT NULL,
    effective_from          DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_otp_policy_effective (effective_from)
) ENGINE = InnoDB;
