-- Append-only audit event store.
--
-- There is no UPDATE or DELETE path in this service by design: an audit trail that can
-- be rewritten is not an audit trail. Retention is a separate archival concern.
--
-- event_id is supplied by the producer and is the idempotency key, so a retried
-- delivery from an outbox publisher cannot double-record an event.

CREATE TABLE audit_events (
    event_id          VARCHAR(36)  NOT NULL,
    source_service    VARCHAR(48)  NOT NULL,
    event_type        VARCHAR(80)  NOT NULL,
    aggregate_type    VARCHAR(48),
    aggregate_id      VARCHAR(64),
    actor_user_id     VARCHAR(64),
    actor_employee_id VARCHAR(64),
    branch_code       VARCHAR(20),
    correlation_id    VARCHAR(64),
    http_method       VARCHAR(10),
    http_path         VARCHAR(255),
    http_status       INT,
    occurred_at       DATETIME(6)  NOT NULL,
    ingested_at       DATETIME(6)  NOT NULL,
    payload           TEXT,
    PRIMARY KEY (event_id),
    -- Cross-service tracing is the main reason this service exists without a broker.
    KEY idx_audit_correlation (correlation_id, occurred_at),
    KEY idx_audit_aggregate (aggregate_type, aggregate_id, occurred_at),
    KEY idx_audit_service (source_service, occurred_at),
    KEY idx_audit_actor (actor_employee_id, occurred_at),
    KEY idx_audit_occurred (occurred_at)
) ENGINE = InnoDB;

-- Payloads that could not be parsed are parked rather than dropped: losing an audit
-- record silently is worse than storing one that needs manual inspection.
CREATE TABLE audit_ingest_failures (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    source_service VARCHAR(48),
    raw_payload    TEXT,
    failure_reason VARCHAR(500) NOT NULL,
    received_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_failures_received (received_at)
) ENGINE = InnoDB;
