MERGE INTO accounts t USING (
  SELECT 'a0000000-0000-0000-0000-000000000101' account_id, '510000000101' account_number,
         'XXXXXXXX0101' masked_number, 'Regular Savings - Vikram Rao' account_name,
         'CIF900101' cif_no, 'SAV-REG' product_code, 'BR001' branch_code, 50000 balance,
         1000 min_balance, 0 overdraft_limit, 3.5 interest_rate, DATE '2026-02-02' opened_on FROM dual UNION ALL
  SELECT 'a0000000-0000-0000-0000-000000000102', '510000000102', 'XXXXXXXX0102',
         'Business Current - Vikram Rao', 'CIF900101', 'CUR-BASIC', 'BR001', 120000, 5000, 25000, 0, DATE '2026-02-05' FROM dual UNION ALL
  SELECT 'a0000000-0000-0000-0000-000000000103', '520000000103', 'XXXXXXXX0103',
         'Senior Savings - Ananya Deshmukh', 'CIF900102', 'SAV-SENIOR', 'BR002', 8000, 500, 0, 4.25, DATE '2026-02-11' FROM dual
) s ON (t.account_id = s.account_id)
WHEN NOT MATCHED THEN INSERT
  (account_id, account_number, masked_account_number, account_name, cif_no, product_code, branch_code,
   currency, status, ledger_balance, held_amount, min_balance, overdraft_limit, interest_rate,
   tenure_months, maturity_date, opened_on, version, created_at, updated_at)
VALUES (s.account_id, s.account_number, s.masked_number, s.account_name, s.cif_no, s.product_code,
        s.branch_code, 'INR', 'ACTIVE', s.balance, 0, s.min_balance, s.overdraft_limit,
        s.interest_rate, NULL, NULL, s.opened_on, 0, SYSTIMESTAMP, SYSTIMESTAMP);

MERGE INTO account_holders t USING (
  SELECT 'h0000000-0000-0000-0000-000000000101' holder_id, 'a0000000-0000-0000-0000-000000000101' account_id, 'CIF900101' cif_no FROM dual UNION ALL
  SELECT 'h0000000-0000-0000-0000-000000000102', 'a0000000-0000-0000-0000-000000000102', 'CIF900101' FROM dual UNION ALL
  SELECT 'h0000000-0000-0000-0000-000000000103', 'a0000000-0000-0000-0000-000000000103', 'CIF900102' FROM dual
) s ON (t.holder_id = s.holder_id)
WHEN NOT MATCHED THEN INSERT (holder_id, account_id, cif_no, holder_role, holder_sequence, status, added_at)
VALUES (s.holder_id, s.account_id, s.cif_no, 'PRIMARY', 1, 'ACTIVE', SYSTIMESTAMP);

MERGE INTO account_status_history t USING (
  SELECT 'a0000000-0000-0000-0000-000000000101' account_id FROM dual UNION ALL
  SELECT 'a0000000-0000-0000-0000-000000000102' FROM dual UNION ALL
  SELECT 'a0000000-0000-0000-0000-000000000103' FROM dual
) s ON (t.account_id=s.account_id AND t.source='SEED' AND t.to_status='ACTIVE')
WHEN NOT MATCHED THEN INSERT (account_id,from_status,to_status,reason,source,changed_at)
VALUES (s.account_id,NULL,'ACTIVE','Seeded fixture account','SEED',SYSTIMESTAMP);

MERGE INTO linked_cards t USING (
  SELECT 'c0000000-0000-0000-0000-000000000101' card_id, 'a0000000-0000-0000-0000-000000000101' account_id,
         'CIF900101' cif_no, 'XXXXXXXXXXXX4321' masked_pan, DATE '2026-02-02' issued_on FROM dual UNION ALL
  SELECT 'c0000000-0000-0000-0000-000000000102', 'a0000000-0000-0000-0000-000000000102',
         'CIF900101', 'XXXXXXXXXXXX8765', DATE '2026-02-05' FROM dual
) s ON (t.card_id = s.card_id)
WHEN NOT MATCHED THEN INSERT
  (card_id, account_id, cif_no, masked_pan, card_type, status, currency, issued_on, expires_on)
VALUES (s.card_id, s.account_id, s.cif_no, s.masked_pan, 'DEBIT', 'ACTIVE', 'INR', s.issued_on, DATE '2030-02-28');

MERGE INTO account_limits t USING (
  SELECT 'a0000000-0000-0000-0000-000000000101' account_id, 50000 amount FROM dual UNION ALL
  SELECT 'a0000000-0000-0000-0000-000000000102', 200000 FROM dual UNION ALL
  SELECT 'a0000000-0000-0000-0000-000000000103', 50000 FROM dual
) s ON (t.account_id = s.account_id)
WHEN NOT MATCHED THEN INSERT (account_id, per_transaction_limit, daily_withdrawal_limit, updated_at)
VALUES (s.account_id, s.amount, s.amount, SYSTIMESTAMP);

MERGE INTO account_outbox t USING (
  SELECT 'e0000000-0000-0000-0000-000000000101' event_id, 'a0000000-0000-0000-0000-000000000101' account_id, 'CIF900101' cif_no, 'BR001' branch_code, 'XXXXXXXX0101' masked_no, 'Regular Savings - Vikram Rao' account_name, 50000 balance FROM dual UNION ALL
  SELECT 'e0000000-0000-0000-0000-000000000102','a0000000-0000-0000-0000-000000000102','CIF900101','BR001','XXXXXXXX0102','Business Current - Vikram Rao',120000 FROM dual UNION ALL
  SELECT 'e0000000-0000-0000-0000-000000000103','a0000000-0000-0000-0000-000000000103','CIF900102','BR002','XXXXXXXX0103','Senior Savings - Ananya Deshmukh',8000 FROM dual
) s ON (t.event_id=s.event_id)
WHEN NOT MATCHED THEN INSERT
  (event_id,aggregate_type,aggregate_id,event_type,destination,payload,status,attempts,next_attempt_at,created_at)
VALUES (s.event_id,'ACCOUNT',s.account_id,'ACCOUNT_OPENED','STATEMENT',
        '{"sourceEventId":"'||s.event_id||'","accountId":"'||s.account_id||'","customerId":"'||s.cif_no||'","branchId":"'||s.branch_code||'","maskedAccountNumber":"'||s.masked_no||'","accountName":"'||s.account_name||'","status":"ACTIVE","currency":"INR","currentBalance":'||TO_CHAR(s.balance,'FM9999999990D0000','NLS_NUMERIC_CHARACTERS=''.,''')||',"dormantSince":null,"sourceUpdatedAt":"2026-02-02T00:00:00Z"}',
        'PENDING',0,SYSTIMESTAMP,SYSTIMESTAMP);

MERGE INTO account_product_ownerships t USING (
  SELECT account_id, product_code, currency, interest_rate, tenure_months, opened_on, maturity_date,
         created_at, updated_at, status FROM accounts
  WHERE product_code IN ('SAV-REG','SAV-SENIOR','CUR-BASIC','FD-12M')
) s ON (t.ownership_id = 'BASE-' || s.account_id)
WHEN NOT MATCHED THEN INSERT
  (ownership_id, owner_account_id, product_code, product_name, product_type, product_version_id,
   product_version_number, acquisition_type, principal_amount, currency, interest_rate, tenure_months,
   acquired_on, maturity_date, status, purchase_transaction_id, reversal_transaction_id, version,
   created_at, updated_at)
VALUES ('BASE-' || s.account_id, s.account_id, s.product_code,
        CASE s.product_code WHEN 'SAV-REG' THEN 'Regular Savings Account' WHEN 'SAV-SENIOR' THEN 'Senior Citizen Savings' WHEN 'CUR-BASIC' THEN 'Basic Current Account' ELSE '12-Month Fixed Deposit' END,
        CASE WHEN s.product_code IN ('SAV-REG','SAV-SENIOR') THEN 'SAVINGS' WHEN s.product_code = 'CUR-BASIC' THEN 'CURRENT' ELSE 'TERM_DEPOSIT' END,
        NULL, NULL, 'ACCOUNT_OPENING', NULL, s.currency, s.interest_rate, s.tenure_months, s.opened_on,
        s.maturity_date, CASE WHEN s.status = 'CLOSED' THEN 'CLOSED' WHEN s.status = 'MATURED' THEN 'MATURED' ELSE 'ACTIVE' END,
        NULL, NULL, 0, s.created_at, s.updated_at);
