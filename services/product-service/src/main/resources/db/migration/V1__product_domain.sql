-- Product master schema.
-- product_code is the stable business key every other service stores as a logical
-- reference (accounts.product_code), so it is the primary key rather than a surrogate id.

CREATE TABLE products (
    product_code            VARCHAR(30)   NOT NULL,
    product_name            VARCHAR(120)  NOT NULL,
    product_type            VARCHAR(30)   NOT NULL,
    description             VARCHAR(500),
    currency                CHAR(3)       NOT NULL DEFAULT 'INR',
    interest_rate           DECIMAL(8,4)  NOT NULL DEFAULT 0,
    min_balance             DECIMAL(19,2) NOT NULL DEFAULT 0,
    min_opening_deposit     DECIMAL(19,2) NOT NULL DEFAULT 0,
    max_withdrawal_per_day  DECIMAL(19,2) NOT NULL DEFAULT 0,
    free_txn_per_month      INT           NOT NULL DEFAULT 0,
    -- NULL for demand deposits; required for FD/RD.
    tenure_months           INT           NULL,
    allows_overdraft        BOOLEAN       NOT NULL DEFAULT FALSE,
    -- When true the account is created PENDING_ACTIVATION until it is funded.
    requires_funding        BOOLEAN       NOT NULL DEFAULT FALSE,
    min_age                 INT           NULL,
    status                  VARCHAR(20)   NOT NULL,
    effective_from          DATE          NOT NULL,
    effective_to            DATE          NULL,
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (product_code),
    KEY idx_products_type_status (product_type, status),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT chk_products_rate CHECK (interest_rate >= 0),
    -- Term products are meaningless without a tenure; demand deposits must not carry one.
    CONSTRAINT chk_products_tenure CHECK (
        (product_type IN ('TERM_DEPOSIT','RECURRING_DEPOSIT') AND tenure_months IS NOT NULL)
        OR (product_type NOT IN ('TERM_DEPOSIT','RECURRING_DEPOSIT') AND tenure_months IS NULL))
) ENGINE = InnoDB;

CREATE TABLE product_charges (
    charge_id    BIGINT        NOT NULL AUTO_INCREMENT,
    product_code VARCHAR(30)   NOT NULL,
    charge_type  VARCHAR(50)   NOT NULL,
    amount       DECIMAL(19,2) NOT NULL,
    frequency    VARCHAR(30)   NOT NULL,
    PRIMARY KEY (charge_id),
    CONSTRAINT uk_product_charge UNIQUE (product_code, charge_type),
    CONSTRAINT fk_product_charges_product
        FOREIGN KEY (product_code) REFERENCES products(product_code),
    CONSTRAINT chk_product_charge_amount CHECK (amount >= 0)
) ENGINE = InnoDB;

CREATE TABLE product_rules (
    rule_id      BIGINT      NOT NULL AUTO_INCREMENT,
    product_code VARCHAR(30) NOT NULL,
    rule_key     VARCHAR(60) NOT NULL,
    rule_value   VARCHAR(255) NOT NULL,
    data_type    VARCHAR(20) NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (rule_id),
    CONSTRAINT uk_product_rule UNIQUE (product_code, rule_key),
    CONSTRAINT fk_product_rules_product
        FOREIGN KEY (product_code) REFERENCES products(product_code)
) ENGINE = InnoDB;
