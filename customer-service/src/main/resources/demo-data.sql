-- Explicit fictional demo data used only when the "demo" Spring profile is active.
INSERT INTO customers (
    cif_no, user_id, relationship_manager_emp_id, first_name, last_name, dob, gender,
    pan_no, mobile, email, status, kyc_status, risk_classification,
    preferred_communication_channel, email_notifications_enabled,
    sms_notifications_enabled, push_notifications_enabled, kyc_failure_count,
    created_at, updated_at
) VALUES
    ('CIF900101', NULL, 1002, 'Vikram', 'Rao', DATE '1992-03-18', 'MALE',
     'ABCDE1234F', '9820012345', 'vikram.rao@example.test', 'ACTIVE', 'VERIFIED', 'LOW',
     'EMAIL', TRUE, TRUE, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('CIF900102', NULL, NULL, 'Ananya', 'Deshmukh', DATE '1995-07-25', 'FEMALE',
     'XYZPS5678G', '9876543210', 'ananya.d@example.test', 'ACTIVE', 'PENDING', 'MEDIUM',
     'SMS', TRUE, TRUE, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO customer_addresses (
    address_id, cif_no, address_type, line1, city, state, pincode, country, is_current
) VALUES
    (2001, 'CIF900101', 'PERMANENT', 'A-401, Sun City Heights', 'Mumbai', 'Maharashtra', '400076', 'India', TRUE),
    (2002, 'CIF900101', 'RESIDENTIAL', 'Flat 202, Green Acres', 'Bengaluru', 'Karnataka', '560038', 'India', TRUE),
    (2003, 'CIF900102', 'PERMANENT', '77 Blue Ridge Society', 'Pune', 'Maharashtra', '411057', 'India', TRUE);

INSERT INTO kyc_documents (
    doc_id, cif_no, doc_type, doc_number, document_number_hash, expiry_date,
    file_path, verify_status, assigned_to_emp_id, verified_by_emp_id,
    rejection_reason, submitted_at, verified_at, expiry_alerted_at
) VALUES
    (3001, 'CIF900101', 'PAN_CARD', '******234F',
     '6442fd73a940c1186d6268bd27f89233e12429902c7805037e8aab6e717be6d9',
     DATE '2030-03-18', 'demo://kyc/CIF900101/pan-card', 'VERIFIED', 1002, 1002,
     NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

INSERT INTO beneficiaries (
    beneficiary_id, cif_no, beneficiary_name, beneficiary_account_no,
    beneficiary_bank_name, beneficiary_ifsc, beneficiary_nickname,
    beneficiary_type, status, added_at, activated_at
) VALUES
    (4001, 'CIF900101', 'Ravi Kumar', '123456789012', 'Demo Bank',
     'DEMO0000123', 'Ravi', 'BANK_ACCOUNT', 'ACTIVE',
     DATEADD('HOUR', -48, CURRENT_TIMESTAMP), DATEADD('HOUR', -24, CURRENT_TIMESTAMP));

ALTER TABLE customer_addresses ALTER COLUMN address_id RESTART WITH 2100;
ALTER TABLE kyc_documents ALTER COLUMN doc_id RESTART WITH 3100;
ALTER TABLE beneficiaries ALTER COLUMN beneficiary_id RESTART WITH 4100;
