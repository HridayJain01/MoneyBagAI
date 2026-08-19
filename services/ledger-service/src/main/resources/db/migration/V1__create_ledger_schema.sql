CREATE TABLE ledger_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(160) NOT NULL,
    account_type VARCHAR(24) NOT NULL,
    normal_side VARCHAR(8) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ledger_accounts_code UNIQUE (code),
    CONSTRAINT chk_ledger_accounts_balance_non_null CHECK (balance IS NOT NULL),
    CONSTRAINT chk_ledger_accounts_type CHECK (account_type IN ('ASSET','LIABILITY','INCOME','EXPENSE','CLEARING')),
    CONSTRAINT chk_ledger_accounts_side CHECK (normal_side IN ('DEBIT','CREDIT'))
);

CREATE TABLE ledger_journal_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    journal_reference VARCHAR(100) NOT NULL,
    transaction_id VARCHAR(64),
    journal_type VARCHAR(40) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(16) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    total_debit DECIMAL(19,4) NOT NULL,
    total_credit DECIMAL(19,4) NOT NULL,
    reversal_of_journal_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    posted_at DATETIME(6),
    created_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_journal_entries_reference UNIQUE (journal_reference),
    CONSTRAINT uk_journal_entries_reversal UNIQUE (reversal_of_journal_id),
    CONSTRAINT fk_journal_entries_reversal FOREIGN KEY (reversal_of_journal_id) REFERENCES ledger_journal_entries(id),
    CONSTRAINT chk_journal_entries_status CHECK (status IN ('DRAFT','POSTED','REVERSED')),
    CONSTRAINT chk_journal_entries_totals CHECK (total_debit > 0 AND total_debit = total_credit)
);

CREATE TABLE ledger_journal_lines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    journal_entry_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    ledger_account_id BIGINT NOT NULL,
    ledger_code VARCHAR(32) NOT NULL,
    customer_account_id BIGINT,
    side VARCHAR(8) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_journal_lines_entry FOREIGN KEY (journal_entry_id) REFERENCES ledger_journal_entries(id),
    CONSTRAINT fk_journal_lines_account FOREIGN KEY (ledger_account_id) REFERENCES ledger_accounts(id),
    CONSTRAINT uk_journal_lines_number UNIQUE (journal_entry_id, line_number),
    CONSTRAINT chk_journal_lines_amount CHECK (amount > 0),
    CONSTRAINT chk_journal_lines_side CHECK (side IN ('DEBIT','CREDIT'))
);

CREATE INDEX idx_journal_entries_transaction ON ledger_journal_entries(transaction_id, created_at);
CREATE INDEX idx_journal_lines_customer_account ON ledger_journal_lines(customer_account_id, created_at);
CREATE INDEX idx_journal_lines_ledger_code ON ledger_journal_lines(ledger_code, created_at);
