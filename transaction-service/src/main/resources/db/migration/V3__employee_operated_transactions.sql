ALTER TABLE transactions RENAME COLUMN customer_id TO account_holder_id;
ALTER TABLE transactions RENAME COLUMN maker_user_id TO maker_employee_id;
ALTER TABLE transactions RENAME COLUMN checker_user_id TO checker_employee_id;

CREATE INDEX idx_transactions_account_holder ON transactions(account_holder_id, created_at);
