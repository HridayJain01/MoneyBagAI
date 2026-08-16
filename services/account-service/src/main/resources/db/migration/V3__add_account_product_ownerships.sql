CREATE TABLE account_product_ownerships (
    ownership_id            VARCHAR(64)   NOT NULL,
    owner_account_id        VARCHAR(36)   NOT NULL,
    product_code            VARCHAR(30)   NOT NULL,
    product_name            VARCHAR(120)  NOT NULL,
    product_type            VARCHAR(30)   NOT NULL,
    product_version_id      BIGINT        NULL,
    product_version_number  INT           NULL,
    acquisition_type        VARCHAR(30)   NOT NULL,
    principal_amount        DECIMAL(19,4) NULL,
    currency                CHAR(3)       NOT NULL,
    interest_rate           DECIMAL(8,4)  NOT NULL,
    tenure_months           INT           NULL,
    acquired_on             DATE          NOT NULL,
    maturity_date           DATE          NULL,
    status                  VARCHAR(20)   NOT NULL,
    purchase_transaction_id VARCHAR(36)   NULL,
    reversal_transaction_id VARCHAR(36)   NULL,
    version                 BIGINT        NOT NULL DEFAULT 0,
    created_at              DATETIME(6)   NOT NULL,
    updated_at              DATETIME(6)   NOT NULL,
    PRIMARY KEY (ownership_id),
    CONSTRAINT uk_owned_product_purchase UNIQUE (purchase_transaction_id),
    CONSTRAINT uk_owned_product_reversal UNIQUE (reversal_transaction_id),
    CONSTRAINT fk_owned_product_account FOREIGN KEY (owner_account_id) REFERENCES accounts(account_id),
    KEY idx_owned_products_account (owner_account_id, acquired_on),
    KEY idx_owned_products_product (product_code, status),
    CONSTRAINT chk_owned_product_acquisition CHECK (
        acquisition_type IN ('ACCOUNT_OPENING','TRANSACTION_PURCHASE')),
    CONSTRAINT chk_owned_product_status CHECK (
        status IN ('PENDING','ACTIVE','REVERSED','MATURED','CLOSED')),
    CONSTRAINT chk_owned_product_principal CHECK (
        principal_amount IS NULL OR principal_amount >= 0)
) ENGINE = InnoDB;

-- Existing accounts retain their actual snapshotted rate and tenure. Their version is
-- intentionally NULL because older account rows did not record an exact catalogue version.
INSERT INTO account_product_ownerships (
    ownership_id, owner_account_id, product_code, product_name, product_type,
    product_version_id, product_version_number, acquisition_type, principal_amount,
    currency, interest_rate, tenure_months, acquired_on, maturity_date, status,
    purchase_transaction_id, reversal_transaction_id, version, created_at, updated_at)
SELECT CONCAT('BASE-', account_id), account_id, product_code,
       CASE product_code
           WHEN 'SAV-REG' THEN 'Regular Savings Account'
           WHEN 'SAV-SENIOR' THEN 'Senior Citizen Savings'
           WHEN 'CUR-BASIC' THEN 'Basic Current Account'
           WHEN 'FD-12M' THEN '12-Month Fixed Deposit'
       END,
       CASE product_code
           WHEN 'SAV-REG' THEN 'SAVINGS'
           WHEN 'SAV-SENIOR' THEN 'SAVINGS'
           WHEN 'CUR-BASIC' THEN 'CURRENT'
           WHEN 'FD-12M' THEN 'TERM_DEPOSIT'
       END,
       NULL, NULL, 'ACCOUNT_OPENING', NULL, currency, interest_rate, tenure_months,
       opened_on, maturity_date,
       CASE
           WHEN status = 'PENDING_ACTIVATION' THEN 'PENDING'
           WHEN status = 'MATURED' THEN 'MATURED'
           WHEN status = 'CLOSED' THEN 'CLOSED'
           ELSE 'ACTIVE'
       END,
       NULL, NULL, 0, created_at, updated_at
  FROM accounts
 WHERE product_code IN ('SAV-REG','SAV-SENIOR','CUR-BASIC','FD-12M');
