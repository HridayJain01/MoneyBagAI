MERGE INTO password_policy t USING (SELECT TIMESTAMP '2026-01-01 00:00:00' effective_from FROM dual) s
ON (t.effective_from=s.effective_from) WHEN NOT MATCHED THEN INSERT
(min_length,require_upper,require_digit,require_special,history_count,max_age_days,effective_from)
VALUES (8,'Y','Y','Y',5,90,s.effective_from);

MERGE INTO session_policy t USING (SELECT TIMESTAMP '2026-01-01 00:00:00' effective_from FROM dual) s
ON (t.effective_from=s.effective_from) WHEN NOT MATCHED THEN INSERT
(idle_timeout_minutes,absolute_timeout_minutes,max_concurrent_sessions,effective_from)
VALUES (30,480,5,s.effective_from);

MERGE INTO otp_policy t USING (SELECT TIMESTAMP '2026-01-01 00:00:00' effective_from FROM dual) s
ON (t.effective_from=s.effective_from) WHEN NOT MATCHED THEN INSERT
(expiry_seconds,max_retries,resend_cooldown_seconds,effective_from) VALUES (300,3,60,s.effective_from);

MERGE INTO channel_limits t USING (
 SELECT 'BRANCH' channel,'PER_TRANSACTION' limit_type,1000000 amount FROM dual UNION ALL SELECT 'BRANCH','DAILY',2000000 FROM dual UNION ALL
 SELECT 'NEFT','PER_TRANSACTION',500000 FROM dual UNION ALL SELECT 'RTGS','PER_TRANSACTION',5000000 FROM dual UNION ALL
 SELECT 'IMPS','PER_TRANSACTION',200000 FROM dual UNION ALL SELECT 'UPI','PER_TRANSACTION',100000 FROM dual UNION ALL SELECT 'UPI','DAILY',200000 FROM dual
) s ON (t.channel=s.channel AND t.limit_type=s.limit_type AND t.effective_from=TIMESTAMP '2026-01-01 00:00:00')
WHEN NOT MATCHED THEN INSERT (channel,limit_type,max_amount,currency,effective_from)
VALUES (s.channel,s.limit_type,s.amount,'INR',TIMESTAMP '2026-01-01 00:00:00');

MERGE INTO maker_checker_thresholds t USING (
 SELECT 'TRANSACTION_APPROVE' action_type,200000 amount FROM dual UNION ALL
 SELECT 'ACCOUNT_APPROVE',0 FROM dual UNION ALL SELECT 'TRANSACTION_REVERSE',0 FROM dual
) s ON (t.action_type=s.action_type AND t.effective_from=TIMESTAMP '2026-01-01 00:00:00')
WHEN NOT MATCHED THEN INSERT (action_type,threshold_amount,currency,effective_from)
VALUES (s.action_type,s.amount,'INR',TIMESTAMP '2026-01-01 00:00:00');

MERGE INTO feature_flags t USING (
 SELECT 'UPI_TRANSFERS_ENABLED' flag_key,'Allow UPI rail transfers' description FROM dual UNION ALL
 SELECT 'CHEQUE_CLEARING_ENABLED','Allow cheque submission and clearing' FROM dual UNION ALL
 SELECT 'CARD_PAYMENTS_ENABLED','Allow card payment transactions' FROM dual UNION ALL
 SELECT 'EOD_INTEREST_ACCRUAL','Run the end-of-day interest accrual job' FROM dual UNION ALL
 SELECT 'STATEMENT_SCHEDULES_ENABLED','Allow recurring statement schedules' FROM dual
) s ON (t.flag_key=s.flag_key) WHEN NOT MATCHED THEN INSERT (flag_key,enabled,description,updated_at)
VALUES (s.flag_key,'Y',s.description,SYSTIMESTAMP);

MERGE INTO config_entries t USING (
 SELECT 'transaction' namespace,'default.currency' config_key,'INR' config_value,'STRING' value_type FROM dual UNION ALL
 SELECT 'transaction','rtgs.minimum.amount','200000','DECIMAL' FROM dual UNION ALL
 SELECT 'account','dormancy.inactive.days','365','INTEGER' FROM dual UNION ALL
 SELECT 'account','closure.settlement.days','7','INTEGER' FROM dual UNION ALL
 SELECT 'statement','download.link.ttl.minutes','15','INTEGER' FROM dual UNION ALL
 SELECT 'notification','retry.max.attempts','5','INTEGER' FROM dual
) s ON (t.namespace=s.namespace AND t.config_key=s.config_key AND t.version=1)
WHEN NOT MATCHED THEN INSERT (namespace,config_key,config_value,value_type,version,effective_from,updated_at)
VALUES (s.namespace,s.config_key,s.config_value,s.value_type,1,TIMESTAMP '2026-01-01 00:00:00',SYSTIMESTAMP);
