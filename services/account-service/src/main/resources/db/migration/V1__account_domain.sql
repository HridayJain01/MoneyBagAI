-- Account service schema.
--
-- Identifier contract (do not drift):
--   account_id  = UUID string. transaction-service's source_account_id is VARCHAR(64)
--                 and its path variables are String.
--   cif_no      = accountHolderId = customerId. transaction-service compares it against
--                 the card context; statement-reporting compares it against X-Customer-Id.
--   branch_code = the value carried as BOTH X-Branch-Code and X-Branch-Id.
--
-- available_balance is deliberately NOT stored. It is derived in one place
-- (BalanceCalculator) as ledger_balance - held_amount - min_balance + overdraft_limit.
-- A stored copy is a second source of truth that will drift from the first.

CREATE TABLE accounts (
    account_id            VARCHAR(36)   NOT NULL,
    account_number        VARCHAR(20)   NOT NULL,
    masked_account_number VARCHAR(24)   NOT NULL,
    account_name          VARCHAR(150)  NOT NULL,
    cif_no                VARCHAR(30)   NOT NULL,   -- logical -> customer-service
    product_code          VARCHAR(30)   NOT NULL,   -- logical -> product-service
    branch_code           VARCHAR(20)   NOT NULL,   -- logical -> branch-employee-service
    currency              CHAR(3)       NOT NULL,
    status                VARCHAR(24)   NOT NULL,
    ledger_balance        DECIMAL(19,4) NOT NULL DEFAULT 0,
    held_amount           DECIMAL(19,4) NOT NULL DEFAULT 0,
    -- Product terms are SNAPSHOTTED at opening, not referenced, so a later rate change
    -- cannot silently rewrite the terms of an account already open.
    min_balance           DECIMAL(19,4) NOT NULL DEFAULT 0,
    overdraft_limit       DECIMAL(19,4) NOT NULL DEFAULT 0,
    interest_rate         DECIMAL(8,4)  NOT NULL DEFAULT 0,
    tenure_months         INT           NULL,
    maturity_date         DATE          NULL,
    opened_on             DATE          NOT NULL,
    closed_on             DATE          NULL,
    dormant_since         DATE          NULL,
    last_activity_at      DATETIME(6)   NULL,
    application_id        VARCHAR(36)   NULL,
    -- JPA @Version. Mapped as a primitive long in AccountContext, so it must never be null.
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    PRIMARY KEY (account_id),
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    KEY idx_accounts_cif (cif_no),
    KEY idx_accounts_branch (branch_code),
    KEY idx_accounts_product_status (product_code, status),
    KEY idx_accounts_status (status),
    CONSTRAINT chk_accounts_held_non_negative CHECK (held_amount >= 0),
    CONSTRAINT chk_accounts_status CHECK (status IN
        ('PENDING_ACTIVATION','ACTIVE','DORMANT','FROZEN','BLOCKED',
         'CLOSURE_REQUESTED','MATURED','CLOSED'))
) ENGINE = InnoDB;

CREATE TABLE account_holders (
    holder_id       VARCHAR(36) NOT NULL,
    account_id      VARCHAR(36) NOT NULL,
    cif_no          VARCHAR(30) NOT NULL,
    holder_role     VARCHAR(16) NOT NULL,
    holder_sequence INT         NOT NULL,
    status          VARCHAR(16) NOT NULL,
    added_at        DATETIME(6) NOT NULL,
    removed_at      DATETIME(6) NULL,
    PRIMARY KEY (holder_id),
    CONSTRAINT uk_holders_account_cif UNIQUE (account_id, cif_no),
    KEY idx_holders_account (account_id),
    KEY idx_holders_cif (cif_no),
    CONSTRAINT fk_holders_account FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT chk_holder_role CHECK (holder_role IN ('PRIMARY','JOINT'))
) ENGINE = InnoDB;

CREATE TABLE account_applications (
    application_id           VARCHAR(36)   NOT NULL,
    application_reference    VARCHAR(40)   NOT NULL,
    cif_no                   VARCHAR(30)   NOT NULL,
    product_code             VARCHAR(30)   NOT NULL,
    branch_code              VARCHAR(20)   NOT NULL,
    currency                 CHAR(3)       NOT NULL,
    account_name             VARCHAR(150)  NOT NULL,
    requested_initial_deposit DECIMAL(19,4) NOT NULL DEFAULT 0,
    status                   VARCHAR(24)   NOT NULL,
    maker_employee_id        VARCHAR(64)   NOT NULL,
    checker_employee_id      VARCHAR(64)   NULL,
    rejection_reason         VARCHAR(500)  NULL,
    product_snapshot         TEXT          NULL,
    -- UNIQUE, so one application can never produce two accounts even under a retry.
    created_account_id       VARCHAR(36)   NULL,
    correlation_id           VARCHAR(64)   NULL,
    version                  BIGINT        NOT NULL DEFAULT 0,
    created_at               DATETIME(6)   NOT NULL,
    updated_at               DATETIME(6)   NOT NULL,
    PRIMARY KEY (application_id),
    CONSTRAINT uk_applications_reference UNIQUE (application_reference),
    CONSTRAINT uk_applications_account UNIQUE (created_account_id),
    KEY idx_applications_branch_status (branch_code, status),
    KEY idx_applications_cif (cif_no),
    CONSTRAINT chk_application_status CHECK (status IN
        ('DRAFT','SUBMITTED','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED'))
) ENGINE = InnoDB;

CREATE TABLE account_approvals (
    approval_id    VARCHAR(36)  NOT NULL,
    application_id VARCHAR(36)  NOT NULL,
    account_id     VARCHAR(36)  NULL,
    emp_id         VARCHAR(64)  NOT NULL,
    decision       VARCHAR(16)  NOT NULL,
    remarks        VARCHAR(500),
    decided_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (approval_id),
    KEY idx_approvals_application (application_id),
    CONSTRAINT fk_approvals_application
        FOREIGN KEY (application_id) REFERENCES account_applications(application_id),
    CONSTRAINT chk_approval_decision CHECK (decision IN ('APPROVED','REJECTED'))
) ENGINE = InnoDB;

CREATE TABLE account_funds_holds (
    hold_id        VARCHAR(36)   NOT NULL,
    account_id     VARCHAR(36)   NOT NULL,
    transaction_id VARCHAR(36)   NULL,
    amount         DECIMAL(19,4) NOT NULL,
    currency       CHAR(3)       NOT NULL,
    reason         VARCHAR(64)   NOT NULL,
    hold_type      VARCHAR(16)   NOT NULL,
    status         VARCHAR(16)   NOT NULL,
    placed_by      VARCHAR(64)   NOT NULL,
    expires_at     DATETIME(6)   NULL,
    created_at     DATETIME(6)   NOT NULL,
    consumed_at    DATETIME(6)   NULL,
    released_at    DATETIME(6)   NULL,
    release_reason VARCHAR(120)  NULL,
    PRIMARY KEY (hold_id),
    -- Second safety net beneath the idempotency table: one hold per transaction.
    CONSTRAINT uk_funds_holds_transaction UNIQUE (transaction_id),
    KEY idx_holds_account_status (account_id, status),
    CONSTRAINT fk_holds_account FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    CONSTRAINT chk_hold_amount CHECK (amount > 0),
    CONSTRAINT chk_hold_status CHECK (status IN ('HELD','CONSUMED','RELEASED','EXPIRED')),
    CONSTRAINT chk_hold_type CHECK (hold_type IN ('TRANSACTION','LIEN','MANUAL'))
) ENGINE = InnoDB;

CREATE TABLE balance_history (
    history_id            BIGINT        NOT NULL AUTO_INCREMENT,
    account_id            VARCHAR(36)   NOT NULL,
    event_id              VARCHAR(36)   NULL,
    transaction_id        VARCHAR(36)   NULL,
    transaction_reference VARCHAR(64)   NULL,
    direction             VARCHAR(8)    NOT NULL,
    amount                DECIMAL(19,4) NOT NULL,
    ledger_balance_before DECIMAL(19,4) NOT NULL,
    ledger_balance_after  DECIMAL(19,4) NOT NULL,
    held_before           DECIMAL(19,4) NOT NULL,
    held_after            DECIMAL(19,4) NOT NULL,
    business_date         DATE          NOT NULL,
    created_at            DATETIME(6)   NOT NULL,
    PRIMARY KEY (history_id),
    KEY idx_balance_history_account_created (account_id, created_at),
    KEY idx_balance_history_account_date (account_id, business_date),
    CONSTRAINT chk_balance_history_direction CHECK (direction IN ('DEBIT','CREDIT'))
) ENGINE = InnoDB;

CREATE TABLE account_status_history (
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    account_id             VARCHAR(36) NOT NULL,
    from_status            VARCHAR(24) NULL,
    to_status              VARCHAR(24) NOT NULL,
    reason                 VARCHAR(500),
    changed_by_employee_id VARCHAR(64),
    source                 VARCHAR(24) NOT NULL,
    changed_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_status_history_account (account_id, changed_at)
) ENGINE = InnoDB;

CREATE TABLE interest_accruals (
    accrual_id            VARCHAR(36)   NOT NULL,
    account_id            VARCHAR(36)   NOT NULL,
    accrual_date          DATE          NOT NULL,
    principal_base        DECIMAL(19,4) NOT NULL,
    rate                  DECIMAL(8,4)  NOT NULL,
    day_count_basis       INT           NOT NULL,
    accrued_amount        DECIMAL(19,6) NOT NULL,
    posted                BOOLEAN       NOT NULL DEFAULT FALSE,
    posted_transaction_id VARCHAR(36)   NULL,
    created_at            DATETIME(6)   NOT NULL,
    PRIMARY KEY (accrual_id),
    -- Makes the EOD job idempotent: one accrual per account per business date.
    CONSTRAINT uk_accrual_account_date UNIQUE (account_id, accrual_date),
    KEY idx_accruals_account (account_id)
) ENGINE = InnoDB;

-- Inbox for /internal/v1/account-projections.
-- Two keys, not one: event_id guards logical replay, dedup_key guards a retried HTTP
-- call. transaction-service's publisher retries up to 10 times with backoff.
CREATE TABLE projection_inbox (
    event_id       VARCHAR(36)   NOT NULL,
    dedup_key      VARCHAR(160)  NOT NULL,
    transaction_id VARCHAR(36)   NULL,
    account_id     VARCHAR(36)   NOT NULL,
    direction      VARCHAR(8)    NOT NULL,
    amount         DECIMAL(19,4) NOT NULL,
    currency       CHAR(3)       NOT NULL,
    event_type     VARCHAR(64)   NULL,
    hold_id        VARCHAR(36)   NULL,
    request_hash   CHAR(64)      NOT NULL,
    correlation_id VARCHAR(64)   NULL,
    outcome        VARCHAR(16)   NOT NULL,
    applied_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (event_id),
    CONSTRAINT uk_projection_dedup UNIQUE (dedup_key),
    KEY idx_projection_account (account_id, applied_at)
) ENGINE = InnoDB;

CREATE TABLE account_idempotency_records (
    idempotency_id  VARCHAR(36)  NOT NULL,
    caller_scope    VARCHAR(128) NOT NULL,
    operation       VARCHAR(80)  NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    request_hash    CHAR(64)     NOT NULL,
    resource_id     VARCHAR(36)  NULL,
    state           VARCHAR(20)  NOT NULL,
    response_code   INT          NULL,
    response_body   TEXT         NULL,
    created_at      DATETIME(6)  NOT NULL,
    completed_at    DATETIME(6)  NULL,
    PRIMARY KEY (idempotency_id),
    CONSTRAINT uk_idempotency_scope UNIQUE (caller_scope, operation, idempotency_key)
) ENGINE = InnoDB;

-- Outbox to statement-reporting-service (and audit).
CREATE TABLE account_outbox (
    event_id        VARCHAR(36)  NOT NULL,
    aggregate_type  VARCHAR(32)  NOT NULL,
    aggregate_id    VARCHAR(36)  NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    destination     VARCHAR(48)  NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6)  NOT NULL,
    last_error      VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    published_at    DATETIME(6)  NULL,
    PRIMARY KEY (event_id),
    KEY idx_outbox_dispatch (status, next_attempt_at)
) ENGINE = InnoDB;

CREATE TABLE account_limits (
    account_id              VARCHAR(36)   NOT NULL,
    per_transaction_limit   DECIMAL(19,4) NOT NULL,
    daily_withdrawal_limit  DECIMAL(19,4) NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (account_id),
    CONSTRAINT fk_limits_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE = InnoDB;

-- Serves transaction-service's card-service contract. A card is a secondary product
-- linked to an account, so it lives here rather than in a fourteenth module that
-- SERVICE_SCHEMA_DIVISION.md explicitly rules out.
CREATE TABLE linked_cards (
    card_id    VARCHAR(36) NOT NULL,
    account_id VARCHAR(36) NOT NULL,
    cif_no     VARCHAR(30) NOT NULL,
    masked_pan VARCHAR(19) NOT NULL,
    card_type  VARCHAR(16) NOT NULL,
    status     VARCHAR(16) NOT NULL,
    currency   CHAR(3)     NOT NULL,
    issued_on  DATE        NOT NULL,
    expires_on DATE        NOT NULL,
    PRIMARY KEY (card_id),
    KEY idx_cards_account (account_id),
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
) ENGINE = InnoDB;
