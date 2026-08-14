-- Notification schema.
--
-- Communication preferences are deliberately NOT duplicated here: customer-service
-- already owns email/sms/push opt-outs on the customers table. This service reads them
-- and marks a notification SUPPRESSED rather than keeping a second copy that drifts.

CREATE TABLE notification_templates (
    template_code    VARCHAR(60)  NOT NULL,
    channel          VARCHAR(10)  NOT NULL,
    subject_template VARCHAR(255),
    body_template    TEXT         NOT NULL,
    locale           VARCHAR(10)  NOT NULL DEFAULT 'en-IN',
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (template_code),
    CONSTRAINT chk_template_channel CHECK (channel IN ('EMAIL','SMS','PUSH'))
) ENGINE = InnoDB;

CREATE TABLE notifications (
    notification_id VARCHAR(36)  NOT NULL,
    -- The producer's Idempotency-Key. UNIQUE, so a retried delivery from an outbox
    -- cannot send the customer the same message twice.
    dedup_key       VARCHAR(160) NOT NULL,
    channel         VARCHAR(10)  NOT NULL,
    recipient       VARCHAR(255) NOT NULL,
    template_code   VARCHAR(60),
    subject         VARCHAR(255),
    body            TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6)  NOT NULL,
    last_error      VARCHAR(500),
    source_service  VARCHAR(48),
    correlation_id  VARCHAR(64),
    cif_no          VARCHAR(30),
    created_at      DATETIME(6)  NOT NULL,
    sent_at         DATETIME(6),
    PRIMARY KEY (notification_id),
    CONSTRAINT uk_notification_dedup UNIQUE (dedup_key),
    KEY idx_notifications_dispatch (status, next_attempt_at),
    KEY idx_notifications_cif (cif_no, created_at),
    KEY idx_notifications_recipient (recipient),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING','SENT','FAILED','SUPPRESSED'))
) ENGINE = InnoDB;

CREATE TABLE delivery_attempts (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    notification_id VARCHAR(36)  NOT NULL,
    attempt_no      INT          NOT NULL,
    outcome         VARCHAR(16)  NOT NULL,
    detail          VARCHAR(500),
    attempted_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_attempts_notification (notification_id, attempt_no),
    CONSTRAINT fk_attempts_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(notification_id)
) ENGINE = InnoDB;
