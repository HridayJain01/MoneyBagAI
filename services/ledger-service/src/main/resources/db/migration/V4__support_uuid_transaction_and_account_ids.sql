ALTER TABLE journal_entries MODIFY COLUMN transaction_id VARCHAR(64) NULL;
ALTER TABLE journal_lines MODIFY COLUMN customer_account_id VARCHAR(64) NULL;
