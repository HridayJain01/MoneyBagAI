MERGE INTO products t USING (
  SELECT 'SAV-REG' code, 'Regular Savings Account' name, 'SAVINGS' type, 'Standard savings account for individuals' description, 3.5 rate, 1000 min_balance, 1000 opening_deposit, 50000 max_daily, 5 free_txn, NULL tenure, 0 overdraft, 0 funding, 18 min_age FROM dual UNION ALL
  SELECT 'SAV-SENIOR', 'Senior Citizen Savings', 'SAVINGS', 'Savings account with preferential rate for seniors', 4.25, 500, 500, 50000, 10, NULL, 0, 0, 60 FROM dual UNION ALL
  SELECT 'CUR-BASIC', 'Basic Current Account', 'CURRENT', 'Current account for businesses', 0, 5000, 5000, 200000, 50, NULL, 1, 0, 18 FROM dual UNION ALL
  SELECT 'FD-12M', '12-Month Fixed Deposit', 'TERM_DEPOSIT', 'Fixed deposit with a twelve month tenure', 6.75, 0, 5000, 0, 0, 12, 0, 1, 18 FROM dual UNION ALL
  SELECT 'FD-24M', '24-Month Fixed Deposit', 'TERM_DEPOSIT', 'Fixed deposit with a twenty-four month tenure', 7.10, 0, 5000, 0, 0, 24, 0, 1, 18 FROM dual UNION ALL
  SELECT 'RD-12M', '12-Month Recurring Deposit', 'RECURRING_DEPOSIT', 'Recurring deposit with a twelve month tenure', 6.50, 0, 500, 0, 0, 12, 0, 1, 18 FROM dual
) s ON (t.product_code = s.code)
WHEN NOT MATCHED THEN INSERT
  (product_code, product_name, product_type, description, currency, interest_rate, min_balance,
   min_opening_deposit, max_withdrawal_per_day, free_txn_per_month, tenure_months, allows_overdraft,
   requires_funding, min_age, status, effective_from, created_at, updated_at)
VALUES (s.code, s.name, s.type, s.description, 'INR', s.rate, s.min_balance, s.opening_deposit,
        s.max_daily, s.free_txn, s.tenure, s.overdraft, s.funding, s.min_age, 'ACTIVE',
        DATE '2026-01-01', SYSTIMESTAMP, SYSTIMESTAMP);

MERGE INTO product_charges t USING (
  SELECT 'SAV-REG' code, 'ATM_WITHDRAWAL' charge_type, 10 amount, 'PER_TRANSACTION' frequency FROM dual UNION ALL
  SELECT 'SAV-REG','SMS_ALERT',15,'MONTHLY' FROM dual UNION ALL SELECT 'SAV-SENIOR','SMS_ALERT',0,'MONTHLY' FROM dual UNION ALL
  SELECT 'CUR-BASIC','MAINTENANCE',500,'MONTHLY' FROM dual UNION ALL SELECT 'FD-12M','PREMATURE_CLOSURE',250,'ONE_TIME' FROM dual UNION ALL
  SELECT 'FD-24M','PREMATURE_CLOSURE',250,'ONE_TIME' FROM dual UNION ALL SELECT 'RD-12M','MISSED_INSTALMENT',50,'PER_TRANSACTION' FROM dual
) s ON (t.product_code = s.code AND t.charge_type = s.charge_type)
WHEN NOT MATCHED THEN INSERT (product_code, charge_type, amount, frequency)
VALUES (s.code, s.charge_type, s.amount, s.frequency);

MERGE INTO product_rules t USING (
  SELECT 'SAV-REG' code, 'DAY_COUNT_BASIS' rule_key, '365' rule_value, 'INTEGER' data_type FROM dual UNION ALL
  SELECT 'SAV-REG','INTEREST_PAYOUT','QUARTERLY','STRING' FROM dual UNION ALL SELECT 'SAV-SENIOR','DAY_COUNT_BASIS','365','INTEGER' FROM dual UNION ALL
  SELECT 'SAV-SENIOR','INTEREST_PAYOUT','QUARTERLY','STRING' FROM dual UNION ALL SELECT 'SAV-SENIOR','MIN_AGE_YEARS','60','INTEGER' FROM dual UNION ALL
  SELECT 'CUR-BASIC','OVERDRAFT_LIMIT','25000.00','DECIMAL' FROM dual UNION ALL SELECT 'FD-12M','PREMATURE_PENALTY_PCT','1.0','DECIMAL' FROM dual UNION ALL
  SELECT 'FD-24M','PREMATURE_PENALTY_PCT','1.0','DECIMAL' FROM dual UNION ALL SELECT 'RD-12M','INSTALMENT_FREQUENCY','MONTHLY','STRING' FROM dual
) s ON (t.product_code = s.code AND t.rule_key = s.rule_key)
WHEN NOT MATCHED THEN INSERT (product_code, rule_key, rule_value, data_type, active)
VALUES (s.code, s.rule_key, s.rule_value, s.data_type, 1);

MERGE INTO product_versions t USING (
  SELECT p.* FROM products p WHERE p.product_code IN ('SAV-REG','SAV-SENIOR','CUR-BASIC','FD-12M')
) s ON (t.product_code = s.product_code AND t.version_number = 1)
WHEN NOT MATCHED THEN INSERT
  (product_code, version_number, product_name, product_type, description, currency, interest_rate,
   min_balance, min_opening_deposit, max_withdrawal_per_day, free_txn_per_month, tenure_months,
   allows_overdraft, requires_funding, min_age, status, effective_from, effective_to, recorded_at)
VALUES (s.product_code, 1, s.product_name, s.product_type, s.description, s.currency, s.interest_rate,
        s.min_balance, s.min_opening_deposit, s.max_withdrawal_per_day, s.free_txn_per_month,
        s.tenure_months, s.allows_overdraft, s.requires_funding, s.min_age, s.status,
        s.effective_from, s.effective_to, s.updated_at);

MERGE INTO product_version_charges t USING (
  SELECT pv.product_version_id,pc.charge_type,pc.amount,pc.frequency
  FROM product_versions pv JOIN product_charges pc ON pc.product_code=pv.product_code
  WHERE pv.version_number=1 AND pv.product_code IN ('SAV-REG','SAV-SENIOR','CUR-BASIC','FD-12M')
) s ON (t.product_version_id=s.product_version_id AND t.charge_type=s.charge_type)
WHEN NOT MATCHED THEN INSERT (product_version_id,charge_type,amount,frequency)
VALUES (s.product_version_id,s.charge_type,s.amount,s.frequency);

MERGE INTO product_version_rules t USING (
  SELECT pv.product_version_id,pr.rule_key,pr.rule_value,pr.data_type
  FROM product_versions pv JOIN product_rules pr ON pr.product_code=pv.product_code
  WHERE pv.version_number=1 AND pv.product_code IN ('SAV-REG','SAV-SENIOR','CUR-BASIC','FD-12M')
) s ON (t.product_version_id=s.product_version_id AND t.rule_key=s.rule_key)
WHEN NOT MATCHED THEN INSERT (product_version_id,rule_key,rule_value,data_type,active)
VALUES (s.product_version_id,s.rule_key,s.rule_value,s.data_type,1);
