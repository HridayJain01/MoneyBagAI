MERGE INTO ledger_accounts t USING (
  SELECT '110100' code, 'Cash and Settlement Asset' name, 'ASSET' account_type, 'DEBIT' normal_side FROM dual UNION ALL
  SELECT '210000', 'Customer Deposit Control', 'LIABILITY', 'CREDIT' FROM dual UNION ALL
  SELECT '210100', 'Term Deposit Control', 'LIABILITY', 'CREDIT' FROM dual UNION ALL
  SELECT '220100', 'Internal Payment Clearing', 'CLEARING', 'CREDIT' FROM dual UNION ALL
  SELECT '220200', 'External Clearing', 'CLEARING', 'CREDIT' FROM dual UNION ALL
  SELECT '410100', 'Payment Fee Income', 'INCOME', 'CREDIT' FROM dual
) s ON (t.code = s.code)
WHEN NOT MATCHED THEN INSERT
  (code, name, account_type, normal_side, balance, currency_code, active, created_at, updated_at, version)
VALUES (s.code, s.name, s.account_type, s.normal_side, 0, 'INR', 1, SYSTIMESTAMP, SYSTIMESTAMP, 0);
