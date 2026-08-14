-- The six approved products from the revised specification.
-- account-service snapshots these terms onto an account at opening, so the codes here
-- are a cross-service contract. See docs/SEED_FIXTURES.md.

INSERT INTO products (product_code, product_name, product_type, description, currency,
                      interest_rate, min_balance, min_opening_deposit, max_withdrawal_per_day,
                      free_txn_per_month, tenure_months, allows_overdraft, requires_funding,
                      min_age, status, effective_from, created_at, updated_at) VALUES
    ('SAV-REG',    'Regular Savings Account',    'SAVINGS',           'Standard savings account for individuals',
     'INR', 3.5000,  1000.00,  1000.00,  50000.00, 5,  NULL, FALSE, FALSE, 18, 'ACTIVE', '2026-01-01', NOW(6), NOW(6)),
    ('SAV-SENIOR', 'Senior Citizen Savings',     'SAVINGS',           'Savings account with preferential rate for seniors',
     'INR', 4.2500,   500.00,   500.00,  50000.00, 10, NULL, FALSE, FALSE, 60, 'ACTIVE', '2026-01-01', NOW(6), NOW(6)),
    ('CUR-BASIC',  'Basic Current Account',      'CURRENT',           'Current account for businesses',
     'INR', 0.0000,  5000.00,  5000.00, 200000.00, 50, NULL, TRUE,  FALSE, 18, 'ACTIVE', '2026-01-01', NOW(6), NOW(6)),
    ('FD-12M',     '12-Month Fixed Deposit',     'TERM_DEPOSIT',      'Fixed deposit with a twelve month tenure',
     'INR', 6.7500,     0.00,  5000.00,      0.00, 0,    12, FALSE, TRUE,  18, 'ACTIVE', '2026-01-01', NOW(6), NOW(6)),
    ('FD-24M',     '24-Month Fixed Deposit',     'TERM_DEPOSIT',      'Fixed deposit with a twenty-four month tenure',
     'INR', 7.1000,     0.00,  5000.00,      0.00, 0,    24, FALSE, TRUE,  18, 'ACTIVE', '2026-01-01', NOW(6), NOW(6)),
    ('RD-12M',     '12-Month Recurring Deposit', 'RECURRING_DEPOSIT', 'Recurring deposit with a twelve month tenure',
     'INR', 6.5000,     0.00,   500.00,      0.00, 0,    12, FALSE, TRUE,  18, 'ACTIVE', '2026-01-01', NOW(6), NOW(6));

INSERT INTO product_charges (product_code, charge_type, amount, frequency) VALUES
    ('SAV-REG',   'ATM_WITHDRAWAL',    10.00, 'PER_TRANSACTION'),
    ('SAV-REG',   'SMS_ALERT',         15.00, 'MONTHLY'),
    ('SAV-SENIOR','SMS_ALERT',          0.00, 'MONTHLY'),
    ('CUR-BASIC', 'MAINTENANCE',      500.00, 'MONTHLY'),
    ('FD-12M',    'PREMATURE_CLOSURE',250.00, 'ONE_TIME'),
    ('FD-24M',    'PREMATURE_CLOSURE',250.00, 'ONE_TIME'),
    ('RD-12M',    'MISSED_INSTALMENT', 50.00, 'PER_TRANSACTION');

INSERT INTO product_rules (product_code, rule_key, rule_value, data_type) VALUES
    ('SAV-REG',    'DAY_COUNT_BASIS',      '365',      'INTEGER'),
    ('SAV-REG',    'INTEREST_PAYOUT',      'QUARTERLY','STRING'),
    ('SAV-SENIOR', 'DAY_COUNT_BASIS',      '365',      'INTEGER'),
    ('SAV-SENIOR', 'INTEREST_PAYOUT',      'QUARTERLY','STRING'),
    ('SAV-SENIOR', 'MIN_AGE_YEARS',        '60',       'INTEGER'),
    ('CUR-BASIC',  'OVERDRAFT_LIMIT',      '25000.00', 'DECIMAL'),
    ('FD-12M',     'PREMATURE_PENALTY_PCT','1.0',      'DECIMAL'),
    ('FD-24M',     'PREMATURE_PENALTY_PCT','1.0',      'DECIMAL'),
    ('RD-12M',     'INSTALMENT_FREQUENCY', 'MONTHLY',  'STRING');
