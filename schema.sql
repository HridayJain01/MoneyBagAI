-- MoneyBags development schema (MySQL 8+)

CREATE DATABASE IF NOT EXISTS moneybags_security
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moneybags_security;

CREATE TABLE IF NOT EXISTS branches (
    branch_code VARCHAR(20) NOT NULL,
    branch_name VARCHAR(120) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (branch_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (role_id),
    CONSTRAINT uk_roles_role_name UNIQUE (role_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `users` (
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
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES `users` (user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (role_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS employees (
    emp_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    employee_code VARCHAR(30) NOT NULL,
    designation VARCHAR(80) NOT NULL,
    branch_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    date_of_joining DATE NOT NULL,
    PRIMARY KEY (emp_id),
    CONSTRAINT uk_employees_employee_code UNIQUE (employee_code)
) ENGINE=InnoDB;

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
    PRIMARY KEY (session_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS login_audit (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(80) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    event_time DATETIME(6) NOT NULL,
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    failure_reason VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (audit_id)
) ENGINE=InnoDB;

CREATE DATABASE IF NOT EXISTS mydb
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE mydb;

CREATE TABLE IF NOT EXISTS customers (
    cif_no BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    pan_no VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    kyc_status VARCHAR(20) NOT NULL,
    PRIMARY KEY (cif_no),
    CONSTRAINT uk_customers_pan_no UNIQUE (pan_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer_addresses (
    address_id BIGINT NOT NULL AUTO_INCREMENT,
    cif_no BIGINT NOT NULL,
    address_type VARCHAR(20) NOT NULL,
    line1 VARCHAR(150) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    country VARCHAR(80) NOT NULL,
    PRIMARY KEY (address_id),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (cif_no) REFERENCES customers (cif_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS kyc_documents (
    doc_id BIGINT NOT NULL AUTO_INCREMENT,
    cif_no BIGINT NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    doc_number VARCHAR(80) NOT NULL,
    expiry_date DATE,
    file_path VARCHAR(500) NOT NULL,
    verify_status VARCHAR(20) NOT NULL,
    verified_by BIGINT,
    PRIMARY KEY (doc_id),
    CONSTRAINT fk_kyc_documents_customer FOREIGN KEY (cif_no) REFERENCES customers (cif_no)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS product_charges (
    charge_id BIGINT NOT NULL AUTO_INCREMENT,
    product_code VARCHAR(30) NOT NULL,
    charge_type VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    frequency VARCHAR(30) NOT NULL,
    PRIMARY KEY (charge_id),
    CONSTRAINT fk_product_charges_product FOREIGN KEY (product_code) REFERENCES products (product_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS accounts (
    account_no VARCHAR(30) NOT NULL,
    cif_no VARCHAR(30) NOT NULL,
    product_code VARCHAR(30) NOT NULL,
    branch_code VARCHAR(20) NOT NULL,
    balance DECIMAL(19,2) NOT NULL,
    min_balance DECIMAL(19,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    opened_on DATE NOT NULL,
    closed_on DATE,
    version INT NOT NULL,
    PRIMARY KEY (account_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS account_approvals (
    approval_id BIGINT NOT NULL AUTO_INCREMENT,
    account_no VARCHAR(30) NOT NULL,
    emp_id BIGINT NOT NULL,
    approval_status VARCHAR(20) NOT NULL,
    remarks VARCHAR(500),
    approved_at DATETIME(6),
    PRIMARY KEY (approval_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    txn_id BIGINT NOT NULL AUTO_INCREMENT,
    txn_ref VARCHAR(50) NOT NULL,
    request_ref VARCHAR(80) NOT NULL,
    account_no VARCHAR(30) NOT NULL,
    txn_type VARCHAR(30) NOT NULL,
    dr_cr VARCHAR(5) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    running_balance DECIMAL(19,2) NOT NULL,
    counterparty_acct VARCHAR(30),
    narration VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    txn_date DATETIME(6) NOT NULL,
    posted_by VARCHAR(80) NOT NULL,
    PRIMARY KEY (txn_id),
    CONSTRAINT uk_transactions_request_ref UNIQUE (request_ref),
    INDEX idx_transactions_account_date (account_no, txn_date)
) ENGINE=InnoDB;
