ALTER TABLE customers
    ADD COLUMN external_kyc_session_id VARCHAR(36),
    ADD COLUMN external_kyc_decision VARCHAR(20),
    ADD COLUMN external_kyc_decided_at DATETIME(6),
    ADD KEY idx_customers_external_kyc_session (external_kyc_session_id);
