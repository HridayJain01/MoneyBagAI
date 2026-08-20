MERGE INTO notification_templates t USING (
  SELECT 'ACCOUNT_OPENED' code, 'EMAIL' channel, 'Your MoneyBags account is open' subject,
         'Dear {{customerName}}, your {{productName}} account {{maskedAccountNumber}} was opened on {{openedOn}} at branch {{branchCode}}. Thank you for banking with MoneyBags.' body FROM dual UNION ALL
  SELECT 'TXN_COMPLETED', 'SMS', NULL,
         'MoneyBags: {{direction}} of {{currency}} {{amount}} on account {{maskedAccountNumber}} completed on {{postedAt}}. Available balance {{currency}} {{availableBalance}}. Ref {{transactionReference}}.' FROM dual UNION ALL
  SELECT 'KYC_VERIFIED', 'EMAIL', 'Your KYC is verified',
         'Dear {{customerName}}, your KYC documents were verified on {{verifiedAt}}. Your customer profile {{cifNo}} is now fully active.' FROM dual UNION ALL
  SELECT 'ACCOUNT_FROZEN', 'EMAIL', 'Important: your account has been frozen',
         'Dear {{customerName}}, account {{maskedAccountNumber}} was frozen on {{changedAt}}. Reason: {{reason}}. Please contact branch {{branchCode}}.' FROM dual
) s ON (t.template_code = s.code)
WHEN NOT MATCHED THEN INSERT
  (template_code, channel, subject_template, body_template, locale, active, updated_at)
VALUES (s.code, s.channel, s.subject, s.body, 'en-IN', 1, SYSTIMESTAMP);
