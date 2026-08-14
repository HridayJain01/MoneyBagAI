-- Seed customers. See docs/SEED_FIXTURES.md.
--
-- These fixtures previously existed ONLY in demo-data.sql, which is H2-only (it uses
-- DATEADD) and loads only under the `demo` profile. Under MySQL there was no customer
-- data at all, so account-service's eligibility check had nothing to resolve.
--
-- user_id values match identity-service's seeded users; relationship_manager_emp_id 1002
-- matches branch-employee-service's seeded employee at branch BR001.

INSERT INTO customers (cif_no, user_id, relationship_manager_emp_id, first_name, last_name,
                       dob, gender, mobile, email, pan_no, status, kyc_status,
                       risk_classification, preferred_communication_channel,
                       email_notifications_enabled, sms_notifications_enabled,
                       push_notifications_enabled, kyc_failure_count,
                       created_at, updated_at) VALUES
    ('CIF900101', 1, 1002, 'Vikram',  'Rao',       '1992-03-18', 'MALE',   '+919820012345',
     'vikram.rao@example.test',      'ABCDE1234F', 'ACTIVE', 'VERIFIED', 'LOW',    'EMAIL', TRUE, TRUE, FALSE, 0, NOW(6), NOW(6)),
    ('CIF900102', 2, 1002, 'Ananya',  'Deshmukh',  '1995-07-25', 'FEMALE', '+919876543210',
     'ananya.deshmukh@example.test', 'XYZPS5678G', 'ACTIVE', 'PENDING',  'MEDIUM', 'SMS',   TRUE, TRUE, FALSE, 0, NOW(6), NOW(6));

INSERT INTO customer_addresses (cif_no, address_type, line1, city, state, pincode, country, is_current) VALUES
    ('CIF900101', 'PERMANENT',   'A-401, Sun City Heights',  'Mumbai',    'Maharashtra', '400076', 'India', TRUE),
    ('CIF900101', 'CURRENT',     'Flat 202, Green Acres',    'Bengaluru', 'Karnataka',   '560038', 'India', TRUE),
    ('CIF900102', 'PERMANENT',   '77 Blue Ridge Society',    'Pune',      'Maharashtra', '411057', 'India', TRUE);

-- Only hashes and storage references are stored; raw document numbers never are.
INSERT INTO kyc_documents (cif_no, doc_type, doc_number, document_number_hash, expiry_date,
                           file_path, verify_status, verified_by_emp_id, submitted_at, verified_at) VALUES
    ('CIF900101', 'PAN_CARD',     'XXXXX1234F', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
     NULL, '/docs/kyc/cif900101_pan.pdf',     'VERIFIED', 1002, NOW(6), NOW(6)),
    ('CIF900101', 'AADHAAR_CARD', 'XXXX-XXXX-4321', '8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4',
     NULL, '/docs/kyc/cif900101_aadhaar.pdf', 'VERIFIED', 1002, NOW(6), NOW(6)),
    ('CIF900102', 'PAN_CARD',     'XXXXX5678G', '2c624232cdd221771294dfbb310aca000a0df6ac8b66b696d90ef06fdefb64a3',
     NULL, '/docs/kyc/cif900102_pan.pdf',     'PENDING',  NULL, NOW(6), NULL);

-- One active beneficiary so an internal transfer between the two seeded customers has a
-- registered payee to validate against.
INSERT INTO beneficiaries (cif_no, beneficiary_name, beneficiary_account_no, beneficiary_bank_name,
                           beneficiary_ifsc, beneficiary_nickname, beneficiary_type, status,
                           added_at, activated_at) VALUES
    ('CIF900101', 'Ananya Deshmukh', '520000000103', 'MoneyBags Bank', 'MBAG0000002',
     'Ananya', 'INTERNAL', 'ACTIVE', NOW(6), NOW(6));
