INSERT INTO ledger_accounts(
    code, name, account_type, normal_side, balance, currency_code,
    active, created_at, updated_at, version)
VALUES (
    '210100', 'Term Deposit Control', 'LIABILITY', 'CREDIT', 0, 'INR',
    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
