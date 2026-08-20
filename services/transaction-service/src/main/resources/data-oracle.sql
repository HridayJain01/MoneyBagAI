MERGE INTO transaction_limit_rules t USING (
  SELECT '00000000-0000-0000-0000-000000000001' limit_rule_id FROM dual
) s ON (t.limit_rule_id = s.limit_rule_id)
WHEN NOT MATCHED THEN INSERT
  (limit_rule_id, transaction_type, rail, payment_channel, currency, min_amount,
   max_amount, daily_limit, approval_threshold, priority, active, effective_from)
VALUES (s.limit_rule_id, NULL, 'RTGS', NULL, 'INR', 200000, NULL, NULL, 1000000, 100, 1, SYSTIMESTAMP);
