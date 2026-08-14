-- Baseline policies, limits and thresholds. See docs/SEED_FIXTURES.md.
--
-- The maker-checker thresholds and channel limits here must not contradict
-- transaction-service's own seeded limit rules, or the smoke flow will be rejected
-- at one layer and allowed at the other.

INSERT INTO password_policy (min_length, require_upper, require_digit, require_special,
                             history_count, max_age_days, effective_from) VALUES
    (8, 'Y', 'Y', 'Y', 5, 90, '2026-01-01 00:00:00.000000');

INSERT INTO session_policy (idle_timeout_minutes, absolute_timeout_minutes,
                            max_concurrent_sessions, effective_from) VALUES
    (30, 480, 5, '2026-01-01 00:00:00.000000');

INSERT INTO otp_policy (expiry_seconds, max_retries, resend_cooldown_seconds, effective_from) VALUES
    (300, 3, 60, '2026-01-01 00:00:00.000000');

INSERT INTO channel_limits (channel, limit_type, max_amount, currency, effective_from) VALUES
    ('BRANCH', 'PER_TRANSACTION', 1000000.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('BRANCH', 'DAILY',           2000000.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('NEFT',   'PER_TRANSACTION',  500000.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('RTGS',   'PER_TRANSACTION', 5000000.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('IMPS',   'PER_TRANSACTION',  200000.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('UPI',    'PER_TRANSACTION',  100000.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('UPI',    'DAILY',            200000.00, 'INR', '2026-01-01 00:00:00.000000');

-- Above these amounts a transaction routes to a checker. Set high enough that the
-- smoke flow's small deposits and transfers post straight through.
INSERT INTO maker_checker_thresholds (action_type, threshold_amount, currency, effective_from) VALUES
    ('TRANSACTION_APPROVE', 200000.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('ACCOUNT_APPROVE',          0.00, 'INR', '2026-01-01 00:00:00.000000'),
    ('TRANSACTION_REVERSE',      0.00, 'INR', '2026-01-01 00:00:00.000000');

INSERT INTO feature_flags (flag_key, enabled, description, updated_at) VALUES
    ('UPI_TRANSFERS_ENABLED',    'Y', 'Allow UPI rail transfers',                   NOW(6)),
    ('CHEQUE_CLEARING_ENABLED',  'Y', 'Allow cheque submission and clearing',       NOW(6)),
    ('CARD_PAYMENTS_ENABLED',    'Y', 'Allow card payment transactions',            NOW(6)),
    ('EOD_INTEREST_ACCRUAL',     'Y', 'Run the end-of-day interest accrual job',    NOW(6)),
    ('STATEMENT_SCHEDULES_ENABLED','Y','Allow recurring statement schedules',       NOW(6));

INSERT INTO config_entries (namespace, config_key, config_value, value_type, version,
                            effective_from, updated_at) VALUES
    ('transaction', 'default.currency',            'INR',   'STRING',  1, '2026-01-01 00:00:00.000000', NOW(6)),
    ('transaction', 'rtgs.minimum.amount',         '200000','DECIMAL', 1, '2026-01-01 00:00:00.000000', NOW(6)),
    ('account',     'dormancy.inactive.days',      '365',   'INTEGER', 1, '2026-01-01 00:00:00.000000', NOW(6)),
    ('account',     'closure.settlement.days',     '7',     'INTEGER', 1, '2026-01-01 00:00:00.000000', NOW(6)),
    ('statement',   'download.link.ttl.minutes',   '15',    'INTEGER', 1, '2026-01-01 00:00:00.000000', NOW(6)),
    ('notification','retry.max.attempts',          '5',     'INTEGER', 1, '2026-01-01 00:00:00.000000', NOW(6));
