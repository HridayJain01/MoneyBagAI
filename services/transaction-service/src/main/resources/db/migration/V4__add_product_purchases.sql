CREATE TABLE product_purchases (
    purchase_id             VARCHAR(36)   NOT NULL,
    transaction_id          VARCHAR(36)   NOT NULL,
    owner_account_id        VARCHAR(64)   NOT NULL,
    product_code            VARCHAR(30)   NOT NULL,
    product_name            VARCHAR(120)  NOT NULL,
    product_type            VARCHAR(30)   NOT NULL,
    product_version_id      BIGINT        NULL,
    product_version_number  INT           NULL,
    principal_amount        DECIMAL(19,4) NOT NULL,
    currency                CHAR(3)       NOT NULL,
    interest_rate           DECIMAL(8,4)  NOT NULL,
    tenure_months           INT           NOT NULL,
    purchased_on            DATE          NOT NULL,
    maturity_date           DATE          NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    reversal_transaction_id VARCHAR(36)   NULL,
    version                 BIGINT        NOT NULL DEFAULT 0,
    created_at              TIMESTAMP(6)  NOT NULL,
    updated_at              TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (purchase_id),
    CONSTRAINT uk_product_purchase_transaction UNIQUE (transaction_id),
    CONSTRAINT uk_product_purchase_reversal UNIQUE (reversal_transaction_id),
    CONSTRAINT fk_product_purchase_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    CONSTRAINT chk_product_purchase_amount CHECK (principal_amount > 0),
    CONSTRAINT chk_product_purchase_status CHECK (status IN ('PENDING','ACTIVE','CANCELLED','REVERSED'))
);

CREATE INDEX idx_product_purchase_account ON product_purchases(owner_account_id, purchased_on);
CREATE INDEX idx_product_purchase_product ON product_purchases(product_code, status);
