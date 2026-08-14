-- The seeded GL accounts were created in USD while every other service in the system
-- works in INR (transaction-service's default currency, product-service's seeded
-- products, and account-service's seeded accounts are all INR).
--
-- A journal posted in INR against a USD control account is a currency mismatch waiting
-- to happen, so the control accounts are realigned. Safe to run: these accounts carry a
-- zero balance until the first posting.

UPDATE ledger_accounts
   SET currency_code = 'INR',
       updated_at    = NOW(6)
 WHERE code IN ('110100', '210000', '220100', '220200', '410100')
   AND currency_code = 'USD'
   AND balance = 0;
