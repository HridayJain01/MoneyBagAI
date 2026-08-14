-- Seed accounts. See docs/SEED_FIXTURES.md.
--
-- cif_no, product_code and branch_code are literals that MUST match the seeds owned by
-- customer-service, product-service and branch-employee-service respectively. Nothing
-- is looked up here: services start in parallel, so a cross-service call during
-- migration would deadlock startup.
--
-- Fixed UUIDs so tests and the smoke script can address these accounts directly.

INSERT INTO accounts (account_id, account_number, masked_account_number, account_name,
                      cif_no, product_code, branch_code, currency, status,
                      ledger_balance, held_amount, min_balance, overdraft_limit,
                      interest_rate, tenure_months, maturity_date, opened_on,
                      version, created_at, updated_at) VALUES
    ('a0000000-0000-0000-0000-000000000101', '510000000101', 'XXXXXXXX0101',
     'Regular Savings - Vikram Rao',      'CIF900101', 'SAV-REG',    'BR001', 'INR', 'ACTIVE',
      50000.0000, 0.0000, 1000.0000,     0.0000, 3.5000, NULL, NULL, '2026-02-02', 0, NOW(6), NOW(6)),
    ('a0000000-0000-0000-0000-000000000102', '510000000102', 'XXXXXXXX0102',
     'Business Current - Vikram Rao',     'CIF900101', 'CUR-BASIC',  'BR001', 'INR', 'ACTIVE',
     120000.0000, 0.0000, 5000.0000, 25000.0000, 0.0000, NULL, NULL, '2026-02-05', 0, NOW(6), NOW(6)),
    ('a0000000-0000-0000-0000-000000000103', '520000000103', 'XXXXXXXX0103',
     'Senior Savings - Ananya Deshmukh',  'CIF900102', 'SAV-SENIOR', 'BR002', 'INR', 'ACTIVE',
       8000.0000, 0.0000,  500.0000,     0.0000, 4.2500, NULL, NULL, '2026-02-11', 0, NOW(6), NOW(6));

INSERT INTO account_holders (holder_id, account_id, cif_no, holder_role, holder_sequence,
                             status, added_at) VALUES
    ('h0000000-0000-0000-0000-000000000101', 'a0000000-0000-0000-0000-000000000101', 'CIF900101', 'PRIMARY', 1, 'ACTIVE', NOW(6)),
    ('h0000000-0000-0000-0000-000000000102', 'a0000000-0000-0000-0000-000000000102', 'CIF900101', 'PRIMARY', 1, 'ACTIVE', NOW(6)),
    ('h0000000-0000-0000-0000-000000000103', 'a0000000-0000-0000-0000-000000000103', 'CIF900102', 'PRIMARY', 1, 'ACTIVE', NOW(6));

INSERT INTO account_status_history (account_id, from_status, to_status, reason, source, changed_at) VALUES
    ('a0000000-0000-0000-0000-000000000101', NULL, 'ACTIVE', 'Seeded fixture account', 'SEED', NOW(6)),
    ('a0000000-0000-0000-0000-000000000102', NULL, 'ACTIVE', 'Seeded fixture account', 'SEED', NOW(6)),
    ('a0000000-0000-0000-0000-000000000103', NULL, 'ACTIVE', 'Seeded fixture account', 'SEED', NOW(6));

-- Two cards backing transaction-service's card-payment path, which resolves the
-- card-service contract against this service.
INSERT INTO linked_cards (card_id, account_id, cif_no, masked_pan, card_type, status,
                          currency, issued_on, expires_on) VALUES
    ('c0000000-0000-0000-0000-000000000101', 'a0000000-0000-0000-0000-000000000101', 'CIF900101',
     'XXXXXXXXXXXX4321', 'DEBIT', 'ACTIVE', 'INR', '2026-02-02', '2030-02-28'),
    ('c0000000-0000-0000-0000-000000000102', 'a0000000-0000-0000-0000-000000000102', 'CIF900101',
     'XXXXXXXXXXXX8765', 'DEBIT', 'ACTIVE', 'INR', '2026-02-05', '2030-02-28');

INSERT INTO account_limits (account_id, per_transaction_limit, daily_withdrawal_limit, updated_at) VALUES
    ('a0000000-0000-0000-0000-000000000101',  50000.0000,  50000.0000, NOW(6)),
    ('a0000000-0000-0000-0000-000000000102', 200000.0000, 200000.0000, NOW(6)),
    ('a0000000-0000-0000-0000-000000000103',  50000.0000,  50000.0000, NOW(6));

-- PENDING outbox rows so the scheduled publisher backfills the statement read model on
-- first run. Without these, a statement for a seeded account falls back to the source
-- API instead of reading the projection.
INSERT INTO account_outbox (event_id, aggregate_type, aggregate_id, event_type, destination,
                            payload, status, attempts, next_attempt_at, created_at) VALUES
    ('e0000000-0000-0000-0000-000000000101', 'ACCOUNT', 'a0000000-0000-0000-0000-000000000101',
     'ACCOUNT_OPENED', 'STATEMENT',
     '{"sourceEventId":"e0000000-0000-0000-0000-000000000101","accountId":"a0000000-0000-0000-0000-000000000101","customerId":"CIF900101","branchId":"BR001","maskedAccountNumber":"XXXXXXXX0101","accountName":"Regular Savings - Vikram Rao","status":"ACTIVE","currency":"INR","currentBalance":50000.0000,"dormantSince":null,"sourceUpdatedAt":"2026-02-02T00:00:00Z"}',
     'PENDING', 0, NOW(6), NOW(6)),
    ('e0000000-0000-0000-0000-000000000102', 'ACCOUNT', 'a0000000-0000-0000-0000-000000000102',
     'ACCOUNT_OPENED', 'STATEMENT',
     '{"sourceEventId":"e0000000-0000-0000-0000-000000000102","accountId":"a0000000-0000-0000-0000-000000000102","customerId":"CIF900101","branchId":"BR001","maskedAccountNumber":"XXXXXXXX0102","accountName":"Business Current - Vikram Rao","status":"ACTIVE","currency":"INR","currentBalance":120000.0000,"dormantSince":null,"sourceUpdatedAt":"2026-02-05T00:00:00Z"}',
     'PENDING', 0, NOW(6), NOW(6)),
    ('e0000000-0000-0000-0000-000000000103', 'ACCOUNT', 'a0000000-0000-0000-0000-000000000103',
     'ACCOUNT_OPENED', 'STATEMENT',
     '{"sourceEventId":"e0000000-0000-0000-0000-000000000103","accountId":"a0000000-0000-0000-0000-000000000103","customerId":"CIF900102","branchId":"BR002","maskedAccountNumber":"XXXXXXXX0103","accountName":"Senior Savings - Ananya Deshmukh","status":"ACTIVE","currency":"INR","currentBalance":8000.0000,"dormantSince":null,"sourceUpdatedAt":"2026-02-11T00:00:00Z"}',
     'PENDING', 0, NOW(6), NOW(6));
