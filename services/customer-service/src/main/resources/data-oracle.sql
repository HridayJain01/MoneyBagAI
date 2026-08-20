MERGE INTO customers t USING (
  SELECT 'CIF900101' cif_no, 1 user_id, 'Vikram' first_name, 'Rao' last_name, DATE '1992-03-18' dob,
         'MALE' gender, '+919820012345' mobile, 'vikram.rao@example.test' email, 'ABCDE1234F' pan_no,
         'VERIFIED' kyc_status, 'LOW' risk_classification, 'EMAIL' preferred_channel FROM dual UNION ALL
  SELECT 'CIF900102', 2, 'Ananya', 'Deshmukh', DATE '1995-07-25', 'FEMALE', '+919876543210',
         'ananya.deshmukh@example.test', 'XYZPS5678G', 'PENDING', 'MEDIUM', 'SMS' FROM dual
) s ON (t.cif_no = s.cif_no)
WHEN NOT MATCHED THEN INSERT
  (cif_no, user_id, relationship_manager_emp_id, first_name, last_name, dob, gender, mobile, email,
   pan_no, status, kyc_status, risk_classification, preferred_communication_channel,
   email_notifications_enabled, sms_notifications_enabled, push_notifications_enabled,
   kyc_failure_count, created_at, updated_at)
VALUES (s.cif_no, s.user_id, 1002, s.first_name, s.last_name, s.dob, s.gender, s.mobile, s.email,
        s.pan_no, 'ACTIVE', s.kyc_status, s.risk_classification, s.preferred_channel,
        1, 1, 0, 0, SYSTIMESTAMP, SYSTIMESTAMP);

MERGE INTO customer_addresses t USING (
  SELECT 'CIF900101' cif_no, 'PERMANENT' address_type, 'A-401, Sun City Heights' line1, 'Mumbai' city, 'Maharashtra' state, '400076' pincode FROM dual UNION ALL
  SELECT 'CIF900101', 'RESIDENTIAL', 'Flat 202, Green Acres', 'Bengaluru', 'Karnataka', '560038' FROM dual UNION ALL
  SELECT 'CIF900102', 'PERMANENT', '77 Blue Ridge Society', 'Pune', 'Maharashtra', '411057' FROM dual
) s ON (t.cif_no = s.cif_no AND t.address_type = s.address_type)
WHEN NOT MATCHED THEN INSERT (cif_no, address_type, line1, city, state, pincode, country, is_current)
VALUES (s.cif_no, s.address_type, s.line1, s.city, s.state, s.pincode, 'India', 1);

MERGE INTO customer_kyc_documents t USING (
  SELECT 'CIF900101' cif_no, 'PAN_CARD' doc_type, 'XXXXX1234F' doc_number,
         'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' doc_hash,
         '/docs/kyc/cif900101_pan.pdf' file_path, 'VERIFIED' verify_status, 1002 verified_by FROM dual UNION ALL
  SELECT 'CIF900101', 'AADHAAR_CARD', 'XXXX-XXXX-4321',
         '8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4',
         '/docs/kyc/cif900101_aadhaar.pdf', 'VERIFIED', 1002 FROM dual UNION ALL
  SELECT 'CIF900102', 'PAN_CARD', 'XXXXX5678G',
         '2c624232cdd221771294dfbb310aca000a0df6ac8b66b696d90ef06fdefb64a3',
         '/docs/kyc/cif900102_pan.pdf', 'PENDING', NULL FROM dual
) s ON (t.cif_no = s.cif_no AND t.doc_type = s.doc_type)
WHEN NOT MATCHED THEN INSERT
  (cif_no, doc_type, doc_number, document_number_hash, expiry_date, file_path, verify_status,
   verified_by_emp_id, submitted_at, verified_at)
VALUES (s.cif_no, s.doc_type, s.doc_number, s.doc_hash, NULL, s.file_path, s.verify_status,
        s.verified_by, SYSTIMESTAMP, CASE WHEN s.verified_by IS NULL THEN NULL ELSE SYSTIMESTAMP END);

MERGE INTO beneficiaries t USING (
  SELECT 'CIF900101' cif_no, '520000000103' account_no FROM dual
) s ON (t.cif_no = s.cif_no AND t.beneficiary_account_no = s.account_no)
WHEN NOT MATCHED THEN INSERT
  (cif_no, beneficiary_name, beneficiary_account_no, beneficiary_bank_name, beneficiary_ifsc,
   beneficiary_nickname, beneficiary_type, status, added_at, activated_at)
VALUES (s.cif_no, 'Ananya Deshmukh', s.account_no, 'MoneyBags Bank', 'MBAG0000002',
        'Ananya', 'INTERNAL', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP);
