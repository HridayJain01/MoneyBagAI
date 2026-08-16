-- Append-only product history. The existing products/charges/rules tables remain the
-- current catalogue projection; these tables preserve the complete terms that were
-- effective for each published version.

CREATE TABLE product_versions (
    product_version_id       BIGINT        NOT NULL AUTO_INCREMENT,
    product_code             VARCHAR(30)   NOT NULL,
    version_number           INT           NOT NULL,
    product_name             VARCHAR(120)  NOT NULL,
    product_type             VARCHAR(30)   NOT NULL,
    description              VARCHAR(500),
    currency                 CHAR(3)       NOT NULL,
    interest_rate            DECIMAL(8,4)  NOT NULL,
    min_balance              DECIMAL(19,2) NOT NULL,
    min_opening_deposit      DECIMAL(19,2) NOT NULL,
    max_withdrawal_per_day   DECIMAL(19,2) NOT NULL,
    free_txn_per_month       INT           NOT NULL,
    tenure_months            INT           NULL,
    allows_overdraft         BOOLEAN       NOT NULL,
    requires_funding         BOOLEAN       NOT NULL,
    min_age                  INT           NULL,
    status                   VARCHAR(20)   NOT NULL,
    effective_from           DATE          NOT NULL,
    effective_to             DATE          NULL,
    recorded_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (product_version_id),
    CONSTRAINT uk_product_version_number UNIQUE (product_code, version_number),
    CONSTRAINT fk_product_versions_product
        FOREIGN KEY (product_code) REFERENCES products(product_code),
    CONSTRAINT chk_product_version_dates
        CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE = InnoDB;

CREATE INDEX idx_product_versions_effective
    ON product_versions (product_code, effective_from, effective_to, version_number);

CREATE TABLE product_version_charges (
    version_charge_id  BIGINT        NOT NULL AUTO_INCREMENT,
    product_version_id BIGINT        NOT NULL,
    charge_type        VARCHAR(50)   NOT NULL,
    amount             DECIMAL(19,2) NOT NULL,
    frequency          VARCHAR(30)   NOT NULL,
    PRIMARY KEY (version_charge_id),
    CONSTRAINT uk_product_version_charge UNIQUE (product_version_id, charge_type),
    CONSTRAINT fk_version_charges_version
        FOREIGN KEY (product_version_id) REFERENCES product_versions(product_version_id)
) ENGINE = InnoDB;

CREATE TABLE product_version_rules (
    version_rule_id    BIGINT       NOT NULL AUTO_INCREMENT,
    product_version_id BIGINT       NOT NULL,
    rule_key           VARCHAR(60)  NOT NULL,
    rule_value         VARCHAR(255) NOT NULL,
    data_type          VARCHAR(20)  NOT NULL,
    active             BOOLEAN      NOT NULL,
    PRIMARY KEY (version_rule_id),
    CONSTRAINT uk_product_version_rule UNIQUE (product_version_id, rule_key),
    CONSTRAINT fk_version_rules_version
        FOREIGN KEY (product_version_id) REFERENCES product_versions(product_version_id)
) ENGINE = InnoDB;

-- The four products in the agreed scope become version 1 exactly as they exist now.
-- No current catalogue value is updated or deleted by this migration.
INSERT INTO product_versions (
    product_code, version_number, product_name, product_type, description, currency,
    interest_rate, min_balance, min_opening_deposit, max_withdrawal_per_day,
    free_txn_per_month, tenure_months, allows_overdraft, requires_funding, min_age,
    status, effective_from, effective_to, recorded_at)
SELECT
    product_code, 1, product_name, product_type, description, currency,
    interest_rate, min_balance, min_opening_deposit, max_withdrawal_per_day,
    free_txn_per_month, tenure_months, allows_overdraft, requires_funding, min_age,
    status, effective_from, effective_to, updated_at
FROM products
WHERE product_code IN ('SAV-REG', 'SAV-SENIOR', 'CUR-BASIC', 'FD-12M');

INSERT INTO product_version_charges (product_version_id, charge_type, amount, frequency)
SELECT pv.product_version_id, pc.charge_type, pc.amount, pc.frequency
FROM product_versions pv
JOIN product_charges pc ON pc.product_code = pv.product_code
WHERE pv.version_number = 1
  AND pv.product_code IN ('SAV-REG', 'SAV-SENIOR', 'CUR-BASIC', 'FD-12M');

INSERT INTO product_version_rules (product_version_id, rule_key, rule_value, data_type, active)
SELECT pv.product_version_id, pr.rule_key, pr.rule_value, pr.data_type, pr.active
FROM product_versions pv
JOIN product_rules pr ON pr.product_code = pv.product_code
WHERE pv.version_number = 1
  AND pv.product_code IN ('SAV-REG', 'SAV-SENIOR', 'CUR-BASIC', 'FD-12M');
