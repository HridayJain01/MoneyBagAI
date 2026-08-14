CREATE TABLE customers (
    cif_no VARCHAR(30) NOT NULL,
    user_id BIGINT,
    relationship_manager_emp_id BIGINT,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80),
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    pan_no VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    kyc_status VARCHAR(20) NOT NULL,
    risk_classification VARCHAR(20) NOT NULL DEFAULT 'LOW',
    preferred_communication_channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_notifications_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    kyc_failure_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (cif_no),
    CONSTRAINT uk_customers_user UNIQUE (user_id),
    CONSTRAINT uk_customers_pan UNIQUE (pan_no)
);

CREATE TABLE customer_addresses (
    address_id BIGINT NOT NULL AUTO_INCREMENT,
    cif_no VARCHAR(30) NOT NULL,
    address_type VARCHAR(20) NOT NULL,
    line1 VARCHAR(150) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    country VARCHAR(80) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (address_id),
    KEY idx_customer_addresses_cif (cif_no),
    CONSTRAINT fk_customer_addresses_customer
        FOREIGN KEY (cif_no) REFERENCES customers(cif_no)
);

CREATE TABLE kyc_documents (
    doc_id BIGINT NOT NULL AUTO_INCREMENT,
    cif_no VARCHAR(30) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    doc_number VARCHAR(80) NOT NULL,
    document_number_hash VARCHAR(64) NOT NULL,
    expiry_date DATE,
    file_path VARCHAR(500) NOT NULL,
    verify_status VARCHAR(20) NOT NULL,
    assigned_to_emp_id BIGINT,
    verified_by_emp_id BIGINT,
    rejection_reason VARCHAR(500),
    submitted_at DATETIME(6) NOT NULL,
    verified_at DATETIME(6),
    expiry_alerted_at DATETIME(6),
    PRIMARY KEY (doc_id),
    KEY idx_kyc_documents_cif (cif_no),
    KEY idx_kyc_documents_status (verify_status),
    CONSTRAINT fk_kyc_documents_customer
        FOREIGN KEY (cif_no) REFERENCES customers(cif_no)
);

CREATE TABLE kyc_rejection_history (
    rejection_id BIGINT NOT NULL AUTO_INCREMENT,
    cif_no VARCHAR(30) NOT NULL,
    doc_id BIGINT NOT NULL,
    failure_reason VARCHAR(500) NOT NULL,
    rejected_by_emp_id BIGINT,
    attempt_number INT NOT NULL,
    rejected_at DATETIME(6) NOT NULL,
    PRIMARY KEY (rejection_id),
    KEY idx_kyc_rejection_cif (cif_no),
    CONSTRAINT fk_kyc_rejection_document
        FOREIGN KEY (doc_id) REFERENCES kyc_documents(doc_id)
);

CREATE TABLE beneficiaries (
    beneficiary_id BIGINT NOT NULL AUTO_INCREMENT,
    cif_no VARCHAR(30) NOT NULL,
    beneficiary_name VARCHAR(150) NOT NULL,
    beneficiary_account_no VARCHAR(30) NOT NULL,
    beneficiary_bank_name VARCHAR(150),
    beneficiary_ifsc VARCHAR(20) NOT NULL,
    beneficiary_nickname VARCHAR(80),
    beneficiary_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    added_at DATETIME(6) NOT NULL,
    activated_at DATETIME(6),
    PRIMARY KEY (beneficiary_id),
    CONSTRAINT uk_customer_beneficiary UNIQUE (cif_no, beneficiary_account_no, beneficiary_ifsc),
    CONSTRAINT fk_beneficiaries_customer
        FOREIGN KEY (cif_no) REFERENCES customers(cif_no)
);

CREATE TABLE beneficiary_change_history (
    history_id BIGINT NOT NULL AUTO_INCREMENT,
    beneficiary_id BIGINT NOT NULL,
    cif_no VARCHAR(30) NOT NULL,
    change_type VARCHAR(40) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (history_id),
    KEY idx_beneficiary_history_beneficiary (beneficiary_id),
    KEY idx_beneficiary_history_cif (cif_no)
);

CREATE TABLE customer_domain_events (
    event_id BIGINT NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    publication_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    occurred_at DATETIME(6) NOT NULL,
    published_at DATETIME(6),
    failure_reason VARCHAR(500),
    PRIMARY KEY (event_id),
    KEY idx_customer_event_status (publication_status, occurred_at)
);
