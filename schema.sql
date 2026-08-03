CREATE DATABASE IF NOT EXISTS moneybags_auth
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moneybags_auth;

CREATE TABLE IF NOT EXISTS users (
                                     user_id BIGINT NOT NULL AUTO_INCREMENT,
                                     username VARCHAR(80) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    mobile VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    last_login_at DATETIME(6),
    password_changed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
    ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS roles (
                                     role_id BIGINT NOT NULL AUTO_INCREMENT,
                                     role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),

    PRIMARY KEY (role_id),
    UNIQUE KEY uk_roles_name (role_name)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id BIGINT NOT NULL,

                                          PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES users(user_id),

    CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES roles(role_id)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS user_sessions (
                                             session_id BINARY(16) NOT NULL,
    user_id BIGINT NOT NULL,
    access_token_hash VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255),
    device_type VARCHAR(50),
    device_name VARCHAR(100),
    browser VARCHAR(100),
    operating_system VARCHAR(100),
    ip_address VARCHAR(45),
    login_time DATETIME(6) NOT NULL,
    last_activity_time DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    logout_time DATETIME(6),
    session_status VARCHAR(20) NOT NULL,

    PRIMARY KEY (session_id),

    CONSTRAINT fk_user_sessions_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS login_audit (
                                           audit_id BIGINT NOT NULL AUTO_INCREMENT,
                                           user_id BIGINT,
                                           username VARCHAR(80) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    event_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    failure_reason VARCHAR(255),
    status VARCHAR(20) NOT NULL,

    PRIMARY KEY (audit_id),

    CONSTRAINT fk_login_audit_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ) ENGINE = InnoDB;


-- =====================================================
-- BANK ORGANISATION DATABASE
-- =====================================================

CREATE DATABASE IF NOT EXISTS moneybags_bank
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moneybags_bank;

CREATE TABLE IF NOT EXISTS branches (
                                        branch_code VARCHAR(20) NOT NULL,
    branch_name VARCHAR(120) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,

    PRIMARY KEY (branch_code),
    UNIQUE KEY uk_branches_ifsc (ifsc_code)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS employees (
                                         emp_id BIGINT NOT NULL AUTO_INCREMENT,
                                         user_id BIGINT NOT NULL,
                                         employee_code VARCHAR(30) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80),
    email VARCHAR(150) NOT NULL,
    mobile VARCHAR(20),
    designation VARCHAR(80) NOT NULL,
    branch_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    date_of_joining DATE NOT NULL,

    PRIMARY KEY (emp_id),
    UNIQUE KEY uk_employees_user (user_id),
    UNIQUE KEY uk_employees_code (employee_code),

    CONSTRAINT fk_employees_branch
    FOREIGN KEY (branch_code) REFERENCES branches(branch_code)
    ) ENGINE = InnoDB;


-- =====================================================
-- CUSTOMER DATABASE
-- =====================================================

CREATE DATABASE IF NOT EXISTS moneybags_customer
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moneybags_customer;

CREATE TABLE IF NOT EXISTS customers (
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
    kyc_failure_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (cif_no),
    UNIQUE KEY uk_customers_user (user_id),
    UNIQUE KEY uk_customers_pan (pan_no)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS customer_addresses (
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

    CONSTRAINT fk_customer_addresses_customer
    FOREIGN KEY (cif_no) REFERENCES customers(cif_no)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS kyc_documents (
                                             doc_id BIGINT NOT NULL AUTO_INCREMENT,
                                             cif_no VARCHAR(30) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    doc_number VARCHAR(80) NOT NULL,
    expiry_date DATE,
    file_path VARCHAR(500) NOT NULL,
    verify_status VARCHAR(20) NOT NULL,
    assigned_to_emp_id BIGINT,
    verified_by_emp_id BIGINT,
    rejection_reason VARCHAR(500),
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    verified_at DATETIME(6),

    PRIMARY KEY (doc_id),

    CONSTRAINT fk_kyc_documents_customer
    FOREIGN KEY (cif_no) REFERENCES customers(cif_no)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS kyc_rejection_history (
    rejection_id BIGINT NOT NULL AUTO_INCREMENT,
    cif_no VARCHAR(30) NOT NULL,
    doc_id BIGINT NOT NULL,
    failure_reason VARCHAR(500) NOT NULL,
    rejected_by_emp_id BIGINT,
    attempt_number INT NOT NULL,
    rejected_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (rejection_id),
    KEY idx_kyc_rejection_cif (cif_no),

    CONSTRAINT fk_kyc_rejection_document
        FOREIGN KEY (doc_id) REFERENCES kyc_documents(doc_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS beneficiaries (
                                             beneficiary_id BIGINT NOT NULL AUTO_INCREMENT,
                                             cif_no VARCHAR(30) NOT NULL,
    beneficiary_name VARCHAR(150) NOT NULL,
    beneficiary_account_no VARCHAR(30) NOT NULL,
    beneficiary_bank_name VARCHAR(150),
    beneficiary_ifsc VARCHAR(20) NOT NULL,
    beneficiary_nickname VARCHAR(80),
    beneficiary_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    added_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    activated_at DATETIME(6),

    PRIMARY KEY (beneficiary_id),

    CONSTRAINT fk_beneficiaries_customer
    FOREIGN KEY (cif_no) REFERENCES customers(cif_no),

    UNIQUE KEY uk_customer_beneficiary (
                                           cif_no,
                                           beneficiary_account_no,
                                           beneficiary_ifsc
                                       )
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS beneficiary_change_history (
    history_id BIGINT NOT NULL AUTO_INCREMENT,
    beneficiary_id BIGINT NOT NULL,
    cif_no VARCHAR(30) NOT NULL,
    change_type VARCHAR(40) NOT NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (history_id),
    KEY idx_beneficiary_history_beneficiary (beneficiary_id),
    KEY idx_beneficiary_history_cif (cif_no)
) ENGINE = InnoDB;


-- =====================================================
-- PRODUCT DATABASE
-- =====================================================

CREATE DATABASE IF NOT EXISTS moneybags_product
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moneybags_product;

CREATE TABLE IF NOT EXISTS products (
                                        product_code VARCHAR(30) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_type VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    interest_rate DECIMAL(8,4) NOT NULL,
    min_balance DECIMAL(19,2) NOT NULL,
    max_withdrawal_per_day DECIMAL(19,2) NOT NULL,
    free_txn_per_month INT NOT NULL,
    status VARCHAR(20) NOT NULL,

    PRIMARY KEY (product_code)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS product_charges (
                                               charge_id BIGINT NOT NULL AUTO_INCREMENT,
                                               product_code VARCHAR(30) NOT NULL,
    charge_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    frequency VARCHAR(30) NOT NULL,

    PRIMARY KEY (charge_id),

    CONSTRAINT fk_product_charges_product
    FOREIGN KEY (product_code) REFERENCES products(product_code),

    CONSTRAINT chk_product_charge_amount
    CHECK (amount >= 0)
    ) ENGINE = InnoDB;


-- =====================================================
-- ACCOUNT DATABASE
-- =====================================================

CREATE DATABASE IF NOT EXISTS moneybags_account
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moneybags_account;

CREATE TABLE IF NOT EXISTS accounts (
                                        account_no VARCHAR(30) NOT NULL,
    cif_no VARCHAR(30) NOT NULL,
    product_code VARCHAR(30) NOT NULL,
    branch_code VARCHAR(20) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0,
    min_balance DECIMAL(19,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    opened_on DATE NOT NULL,
    closed_on DATE,
    version INT NOT NULL DEFAULT 0,

    PRIMARY KEY (account_no),
    KEY idx_accounts_cif (cif_no),
    KEY idx_accounts_branch (branch_code)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS account_approvals (
                                                 approval_id BIGINT NOT NULL AUTO_INCREMENT,
                                                 account_no VARCHAR(30) NOT NULL,
    emp_id BIGINT NOT NULL,
    approval_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(500),
    approved_at DATETIME(6),

    PRIMARY KEY (approval_id),

    CONSTRAINT fk_account_approvals_account
    FOREIGN KEY (account_no) REFERENCES accounts(account_no)
    ) ENGINE = InnoDB;


-- =====================================================
-- LEDGER DATABASE
-- =====================================================

CREATE DATABASE IF NOT EXISTS moneybags_ledger
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moneybags_ledger;

CREATE TABLE IF NOT EXISTS gl_accounts (
                                           gl_code VARCHAR(30) NOT NULL,
    gl_name VARCHAR(120) NOT NULL,
    gl_type VARCHAR(30) NOT NULL,
    parent_gl_code VARCHAR(30),
    is_control BOOLEAN NOT NULL DEFAULT FALSE,
    current_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,

    PRIMARY KEY (gl_code),

    CONSTRAINT fk_gl_parent
    FOREIGN KEY (parent_gl_code) REFERENCES gl_accounts(gl_code)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
                                            txn_id BIGINT NOT NULL AUTO_INCREMENT,
                                            txn_ref VARCHAR(50) NOT NULL,
    request_ref VARCHAR(80) NOT NULL,
    txn_type VARCHAR(30) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    total_debit DECIMAL(19,2) NOT NULL,
    total_credit DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    value_date DATE NOT NULL,
    posting_date DATETIME(6),
    reversal_of BIGINT,
    narration VARCHAR(500),
    posted_by_user_id BIGINT,
    performed_by_emp_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (txn_id),
    UNIQUE KEY uk_transactions_ref (txn_ref),
    UNIQUE KEY uk_transactions_request (request_ref),
    UNIQUE KEY uk_transactions_reversal (reversal_of),

    CONSTRAINT fk_transactions_reversal
    FOREIGN KEY (reversal_of) REFERENCES transactions(txn_id),

    CONSTRAINT chk_transaction_amount
    CHECK (total_amount > 0),

    CONSTRAINT chk_transaction_balanced
    CHECK (
              total_debit = total_credit
              AND total_amount = total_debit
          )
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ledger_entries (
                                              ledger_id BIGINT NOT NULL AUTO_INCREMENT,
                                              txn_id BIGINT NOT NULL,
                                              account_no VARCHAR(30),
    gl_code VARCHAR(30),
    dr_cr CHAR(2) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    balance_after DECIMAL(19,2),
    counterparty_ref VARCHAR(100),
    value_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (ledger_id),

    CONSTRAINT fk_ledger_entry_transaction
    FOREIGN KEY (txn_id) REFERENCES transactions(txn_id),

    CONSTRAINT fk_ledger_entry_gl
    FOREIGN KEY (gl_code) REFERENCES gl_accounts(gl_code),

    CONSTRAINT chk_ledger_entry_target
    CHECK (
(account_no IS NOT NULL AND gl_code IS NULL)
    OR
(account_no IS NULL AND gl_code IS NOT NULL)
    ),

    CONSTRAINT chk_ledger_entry_side
    CHECK (dr_cr IN ('DR', 'CR')),

    CONSTRAINT chk_ledger_entry_amount
    CHECK (amount > 0),

    KEY idx_ledger_transaction (txn_id),
    KEY idx_ledger_account_date (account_no, value_date),
    KEY idx_ledger_gl_date (gl_code, value_date)
    ) ENGINE = InnoDB;
