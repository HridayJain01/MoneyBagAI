-- Baseline templates. Placeholders are {{name}} style and substituted by TemplateRenderer.

INSERT INTO notification_templates (template_code, channel, subject_template, body_template,
                                    locale, active, updated_at) VALUES
    ('ACCOUNT_OPENED', 'EMAIL', 'Your MoneyBags account is open',
     'Dear {{customerName}}, your {{productName}} account {{maskedAccountNumber}} was opened on {{openedOn}} at branch {{branchCode}}. Thank you for banking with MoneyBags.',
     'en-IN', TRUE, NOW(6)),
    ('TXN_COMPLETED', 'SMS', NULL,
     'MoneyBags: {{direction}} of {{currency}} {{amount}} on account {{maskedAccountNumber}} completed on {{postedAt}}. Available balance {{currency}} {{availableBalance}}. Ref {{transactionReference}}.',
     'en-IN', TRUE, NOW(6)),
    ('KYC_VERIFIED', 'EMAIL', 'Your KYC is verified',
     'Dear {{customerName}}, your KYC documents were verified on {{verifiedAt}}. Your customer profile {{cifNo}} is now fully active.',
     'en-IN', TRUE, NOW(6)),
    ('ACCOUNT_FROZEN', 'EMAIL', 'Important: your account has been frozen',
     'Dear {{customerName}}, account {{maskedAccountNumber}} was frozen on {{changedAt}}. Reason: {{reason}}. Please contact branch {{branchCode}}.',
     'en-IN', TRUE, NOW(6));
