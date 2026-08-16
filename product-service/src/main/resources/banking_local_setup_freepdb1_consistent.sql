SET SQLBLANKLINES ON

-- =========================================================
-- Oracle Free local bootstrap for Banking microservices demo
-- Connect string example: localhost:1521/FREEPDB1
--
-- Notes:
-- 1) Cross-service identifiers are stored as logical references only.
-- 2) This script keeps the schema consistent with the reference model
--    you shared, while using Oracle-compatible datatypes and constraints.
-- =========================================================

-- Run as SYS once if you need a dedicated app user:
--   sqlplus sys@localhost:1521/FREEPDB1 as sysdba
--   CREATE USER bank_app IDENTIFIED BY "Bank@123";
--   GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE TO bank_app;
--   ALTER USER bank_app QUOTA UNLIMITED ON USERS;
-- Then connect:
--   sqlplus bank_app/"Bank@123"@localhost:1521/FREEPDB1

-- Optional cleanup for reruns
BEGIN EXECUTE IMMEDIATE 'DROP TABLE ledger_entries PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE transactions PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE secondary_link_products PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE account_holders PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE accounts PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE product_version_attributes PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE product_attribute_definitions PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE product_versions PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE product_rules PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE products PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE documents PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE customer_addresses PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE customers PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE employees PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE role_permissions PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE permissions PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE users PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE branches PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE roles PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- =========================================================
-- 1) Identity & Access Service
-- =========================================================
CREATE TABLE roles (
  id          NUMBER PRIMARY KEY,
  name        VARCHAR2(30)  NOT NULL UNIQUE,
  description VARCHAR2(200)
);

CREATE TABLE permissions (
  id              NUMBER PRIMARY KEY,
  permission_code VARCHAR2(50)  NOT NULL UNIQUE,
  permission_name  VARCHAR2(100) NOT NULL,
  service_name     VARCHAR2(50)  NOT NULL,
  action_name      VARCHAR2(20)  NOT NULL,
  description      VARCHAR2(200)
);

CREATE TABLE role_permissions (
  role_id      NUMBER NOT NULL,
  permission_id NUMBER NOT NULL,
  scope_rule   VARCHAR2(200),
  CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
  id            NUMBER PRIMARY KEY,
  first_name    VARCHAR2(50)  NOT NULL,
  last_name     VARCHAR2(50)  NOT NULL,
  email         VARCHAR2(255) NOT NULL UNIQUE,
  password_hash VARCHAR2(255) NOT NULL,
  dob           DATE,
  gender        VARCHAR2(10),
  mobile        VARCHAR2(20),
  role_id       NUMBER NOT NULL,
  status        VARCHAR2(15) NOT NULL,
  created_at    TIMESTAMP DEFAULT SYSTIMESTAMP,
  last_login_at TIMESTAMP,
  CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
  CONSTRAINT ck_users_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER') OR gender IS NULL)
);

-- =========================================================
-- 2) Branch Service
-- =========================================================
CREATE TABLE branches (
  id          NUMBER PRIMARY KEY,
  branch_code VARCHAR2(20) NOT NULL UNIQUE,
  name        VARCHAR2(100) NOT NULL,
  address     VARCHAR2(200),
  city        VARCHAR2(60),
  state       VARCHAR2(60),
  pincode     VARCHAR2(10),
  ifsc_code   VARCHAR2(20) NOT NULL UNIQUE
);

-- =========================================================
-- 3) Employee Service
-- =========================================================
CREATE TABLE employees (
  id                   NUMBER PRIMARY KEY,
  user_id              NUMBER NOT NULL UNIQUE,
  employee_code        VARCHAR2(30) NOT NULL UNIQUE,
  dob                  DATE,
  branch_id            NUMBER NOT NULL,
  designation          VARCHAR2(100),
  reporting_manager_id NUMBER,
  joining_date         DATE,
  status               VARCHAR2(15) NOT NULL,
  CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'RESIGNED'))
);

-- =========================================================
-- 4) Customer Service
-- =========================================================
CREATE TABLE customers (
  cif              VARCHAR2(20) PRIMARY KEY,
  user_id          NUMBER NOT NULL UNIQUE,
  customer_type    VARCHAR2(20) NOT NULL,
  dob              DATE,
  gender           VARCHAR2(10),
  mobile           VARCHAR2(20),
  pan_number       VARCHAR2(20),
  aadhaar_masked   VARCHAR2(20),
  kyc_status       VARCHAR2(20) NOT NULL,
  kyc_verified_at  TIMESTAMP,
  created_at       TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT ck_customers_type CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE', 'NRI')),
  CONSTRAINT ck_customers_status CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
  CONSTRAINT ck_customers_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER') OR gender IS NULL)
);

CREATE TABLE customer_addresses (
  id           NUMBER PRIMARY KEY,
  cif          VARCHAR2(20) NOT NULL,
  address_type VARCHAR2(20) NOT NULL,
  line1        VARCHAR2(120) NOT NULL,
  line2        VARCHAR2(120),
  city         VARCHAR2(60),
  state        VARCHAR2(60),
  pincode      VARCHAR2(10),
  CONSTRAINT ck_customer_addresses_type CHECK (address_type IN ('PERMANENT', 'CURRENT'))
);

-- =========================================================
-- 5) Document / KYC Service
-- =========================================================
CREATE TABLE documents (
  id                   NUMBER PRIMARY KEY,
  cif                  VARCHAR2(20) NOT NULL,
  document_type        VARCHAR2(50) NOT NULL,
  document_number_hash  VARCHAR2(200) NOT NULL,
  storage_path         VARCHAR2(300),
  verification_status  VARCHAR2(20) NOT NULL,
  uploaded_at          TIMESTAMP DEFAULT SYSTIMESTAMP,
  verified_by          NUMBER,
  verified_at          TIMESTAMP,
  remarks              VARCHAR2(200),
  CONSTRAINT ck_documents_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

-- =========================================================
-- 6) Product Service
-- =========================================================

CREATE TABLE products (
    id                NUMBER PRIMARY KEY,
    product_code      VARCHAR2(50) NOT NULL,
    name              VARCHAR2(150) NOT NULL,
    product_category  VARCHAR2(30) NOT NULL,
    product_type      VARCHAR2(50) NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_products_code
        UNIQUE (product_code)
);

CREATE TABLE product_versions (
    id              NUMBER PRIMARY KEY,
    product_id      NUMBER NOT NULL,
    version_number  NUMBER NOT NULL,
    status          VARCHAR2(20) NOT NULL,
    effective_from  TIMESTAMP NOT NULL,
    effective_to    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_versions_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),
    CONSTRAINT uk_product_version
        UNIQUE (product_id, version_number),
    CONSTRAINT uk_product_version_pair
        UNIQUE (id, product_id),
    CONSTRAINT ck_product_version_dates
        CHECK (
            effective_to IS NULL
            OR effective_to > effective_from
        )
);

CREATE TABLE product_attribute_definitions (
    id                NUMBER PRIMARY KEY,
    attribute_code    VARCHAR2(50) NOT NULL,
    display_name      VARCHAR2(100) NOT NULL,
    data_type         VARCHAR2(20) NOT NULL,
    unit              VARCHAR2(20),
    CONSTRAINT uk_attribute_code
        UNIQUE (attribute_code),
    CONSTRAINT ck_attribute_data_type
        CHECK (
            data_type IN (
                'STRING',
                'INTEGER',
                'DECIMAL',
                'BOOLEAN',
                'DATE'
            )
        )
);

CREATE TABLE product_version_attributes (
    id                  NUMBER PRIMARY KEY,
    product_version_id  NUMBER NOT NULL,
    attribute_id        NUMBER NOT NULL,
    attribute_value     VARCHAR2(500) NOT NULL,
    CONSTRAINT fk_pva_product_version
        FOREIGN KEY (product_version_id)
        REFERENCES product_versions(id),
    CONSTRAINT fk_pva_attribute
        FOREIGN KEY (attribute_id)
        REFERENCES product_attribute_definitions(id),
    CONSTRAINT uk_pva_version_attribute
        UNIQUE (product_version_id, attribute_id)
);

-- =========================================================
-- 7) Accounts Service
-- =========================================================
CREATE TABLE accounts (
  id                 NUMBER PRIMARY KEY,
  account_number     VARCHAR2(30) NOT NULL UNIQUE,
  branch_id          NUMBER NOT NULL,
  product_id         NUMBER NOT NULL,
  product_version_id NUMBER NOT NULL,
  cif                VARCHAR2(20) NOT NULL,
  account_type       VARCHAR2(20) NOT NULL,
  status             VARCHAR2(20) NOT NULL,
  available_balance  NUMBER(18,2) NOT NULL,
  opened_at          TIMESTAMP NOT NULL,
  closed_at          TIMESTAMP,
  currency           VARCHAR2(3) NOT NULL,
  CONSTRAINT fk_accounts_product_version
    FOREIGN KEY (product_version_id, product_id)
    REFERENCES product_versions(id, product_id),
  CONSTRAINT ck_accounts_type
    CHECK (account_type IN ('SAVINGS', 'CURRENT')),
  CONSTRAINT ck_accounts_status
    CHECK (status IN ('OPEN', 'CLOSED', 'FROZEN', 'DORMANT'))
);

CREATE TABLE account_holders (
  id           NUMBER PRIMARY KEY,
  account_id   NUMBER NOT NULL,
  cif          VARCHAR2(20) NOT NULL,
  holder_role  VARCHAR2(20) NOT NULL,
  is_primary   CHAR(1) NOT NULL,
  created_at   TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT ck_account_holders_role CHECK (holder_role IN ('PRIMARY', 'JOINT', 'NOMINEE')),
  CONSTRAINT ck_account_holders_primary CHECK (is_primary IN ('Y', 'N'))
);

CREATE TABLE secondary_link_products (
  account_id          NUMBER NOT NULL,
  product_id          NUMBER NOT NULL,
  product_version_id  NUMBER NOT NULL,
  linked_at           TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT pk_secondary_link_products
    PRIMARY KEY (account_id, product_id, product_version_id),
  CONSTRAINT fk_slp_product_version
    FOREIGN KEY (product_version_id, product_id)
    REFERENCES product_versions(id, product_id)
);

-- =========================================================
-- 8) Transaction Service
-- =========================================================
CREATE TABLE transactions (
  id                 NUMBER PRIMARY KEY,
  txn_ref            VARCHAR2(40) NOT NULL UNIQUE,
  account_id         NUMBER NOT NULL,
  counter_account_id NUMBER,
  txn_type           VARCHAR2(20) NOT NULL,
  amount             NUMBER(18,2) NOT NULL,
  txn_mode           VARCHAR2(20) NOT NULL,
  description        VARCHAR2(200),
  status             VARCHAR2(20) NOT NULL,
  created_at         TIMESTAMP DEFAULT SYSTIMESTAMP,
  created_by         NUMBER NOT NULL,
  CONSTRAINT ck_transactions_type CHECK (txn_type IN ('DEBIT', 'CREDIT')),
  CONSTRAINT ck_transactions_mode CHECK (txn_mode IN ('UPI', 'NEFT', 'CASH', 'CHEQUE', 'CARD')),
  CONSTRAINT ck_transactions_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILURE'))
);

-- =========================================================
-- 9) Ledger Service
-- =========================================================
CREATE TABLE ledger_entries (
  id            NUMBER PRIMARY KEY,
  txn_id        NUMBER NOT NULL,
  account_id    NUMBER NOT NULL,
  entry_type    VARCHAR2(20) NOT NULL,
  amount        NUMBER(18,2) NOT NULL,
  posting_date  DATE NOT NULL,
  narration     VARCHAR2(200),
  created_at    TIMESTAMP DEFAULT SYSTIMESTAMP,
  CONSTRAINT ck_ledger_entries_type CHECK (entry_type IN ('CREDIT', 'DEBIT'))
);

-- =========================================================
-- Seed data
-- =========================================================

INSERT ALL
  INTO roles (id, name, description) VALUES (1, 'ADMIN', 'System administrator with full platform access')
  INTO roles (id, name, description) VALUES (2, 'TELLER', 'Branch teller for cash and counter operations')
  INTO roles (id, name, description) VALUES (3, 'BRANCH_MANAGER', 'Branch supervisor for approvals and overrides')
  INTO roles (id, name, description) VALUES (4, 'LOAN_OFFICER', 'Officer responsible for loan evaluation')
  INTO roles (id, name, description) VALUES (5, 'AUDITOR', 'Read-only compliance and audit access')
  INTO roles (id, name, description) VALUES (6, 'CUSTOMER', 'End user limited to owned banking resources')
SELECT 1 FROM dual;

INSERT ALL
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (1, 'USER_MANAGE', 'Manage users', 'auth-service', 'MANAGE', 'Manage users, status and role assignment')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (2, 'ROLE_PERMISSION_MANAGE', 'Manage role permissions', 'auth-service', 'MANAGE', 'Manage roles and permission mappings')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (3, 'CUSTOMER_READ', 'Read customer summary', 'customer-service', 'READ', 'Read customer and KYC summary')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (4, 'CUSTOMER_UPDATE', 'Update customer profile', 'customer-service', 'UPDATE', 'Update customer profile')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (5, 'KYC_VERIFY', 'Verify KYC documents', 'customer-service', 'APPROVE', 'Verify or reject KYC documents')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (6, 'PRODUCT_READ', 'View product catalogue', 'product-service', 'READ', 'View product catalogue and rules')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (7, 'PRODUCT_MANAGE', 'Manage products', 'product-service', 'MANAGE', 'Create and change products and rates')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (8, 'ACCOUNT_OPEN', 'Create account application', 'account-service', 'CREATE', 'Create an account application')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (9, 'ACCOUNT_APPROVE', 'Approve account opening', 'account-service', 'APPROVE', 'Approve or reject account opening')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (10, 'ACCOUNT_VIEW', 'View account details', 'account-service', 'READ', 'View permitted account details and balances')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (11, 'ACCOUNT_STATUS_MANAGE', 'Manage account status', 'account-service', 'MANAGE', 'Freeze, unfreeze, block or close account')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (12, 'TRANSACTION_CREATE', 'Initiate transaction', 'transaction-service', 'CREATE', 'Initiate an allowed financial transaction')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (13, 'TRANSACTION_APPROVE', 'Approve transaction', 'transaction-service', 'APPROVE', 'Approve maker-checker transactions')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (14, 'TRANSACTION_REVERSE', 'Reverse transaction', 'transaction-service', 'REVERSE', 'Create compensating reversal')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (15, 'STATEMENT_VIEW', 'Generate statements', 'reporting-service', 'READ', 'Generate permitted statements')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (16, 'AUDIT_READ', 'Read audit records', 'audit-service', 'READ', 'Search and export audit records')
  INTO permissions (id, permission_code, permission_name, service_name, action_name, description) VALUES (17, 'CONFIG_MANAGE', 'Manage configuration', 'config-service', 'MANAGE', 'Change limits, policies and feature flags')
SELECT 1 FROM dual;

INSERT ALL
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 1, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 2, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 3, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 4, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 5, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 6, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 7, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 8, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 9, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 10, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 11, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 12, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 13, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 14, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 15, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 16, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (1, 17, 'UNRESTRICTED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (2, 3, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (2, 8, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (2, 10, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (2, 12, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 3, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 5, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 8, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 9, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 10, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 11, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 12, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 13, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (3, 14, 'BRANCH_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (4, 3, 'DOMAIN_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (4, 5, 'DOMAIN_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (4, 6, 'DOMAIN_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (4, 10, 'DOMAIN_SCOPED')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (5, 3, 'READ_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (5, 10, 'READ_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (5, 15, 'READ_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (5, 16, 'READ_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (6, 6, 'OWNED_RESOURCES_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (6, 8, 'OWNED_RESOURCES_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (6, 10, 'OWNED_RESOURCES_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (6, 12, 'OWNED_RESOURCES_ONLY')
  INTO role_permissions (role_id, permission_id, scope_rule) VALUES (6, 15, 'OWNED_RESOURCES_ONLY')
SELECT 1 FROM dual;

INSERT ALL
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (101, 'Rajesh', 'Sharma', 'rajesh.sharma@example.test', '$2a$12$DEMO...9a2', DATE '1980-01-15', 'MALE', '+91 90000 00001', 1, 'ACTIVE', TIMESTAMP '2026-01-10 09:00:00', TIMESTAMP '2026-08-04 14:22:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (102, 'Priya', 'Nair', 'priya.nair@example.test', '$2a$12$DEMO...1b8', DATE '1986-12-12', 'FEMALE', '+91 90000 00002', 3, 'ACTIVE', TIMESTAMP '2026-01-12 10:15:00', TIMESTAMP '2026-08-05 08:11:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (103, 'Amit', 'Patel', 'amit.patel@example.test', '$2a$12$DEMO...4c3', DATE '1988-05-22', 'MALE', '+91 90000 00003', 2, 'ACTIVE', TIMESTAMP '2026-01-15 11:20:00', TIMESTAMP '2026-08-05 09:00:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (104, 'Vikram', 'Rao', 'vikram.rao@example.test', '$2a$12$DEMO...8x1', DATE '1984-09-09', 'MALE', '+91 90000 00004', 5, 'ACTIVE', TIMESTAMP '2026-02-01 09:30:00', TIMESTAMP '2026-08-04 18:45:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (105, 'Ananya', 'Deshmukh', 'ananya.d@example.test', '$2a$12$DEMO...2y9', DATE '1990-02-10', 'FEMALE', '+91 90000 00005', 4, 'ACTIVE', TIMESTAMP '2026-02-10 13:05:00', TIMESTAMP '2026-08-03 11:12:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (106, 'Sneha', 'Iyer', 'sneha.iyer@example.test', '$2a$12$DEMO...6z7', DATE '1992-03-18', 'FEMALE', '+91 90000 00006', 6, 'ACTIVE', TIMESTAMP '2026-02-15 08:45:00', TIMESTAMP '2026-08-05 17:10:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (107, 'Arjun', 'Mehta', 'arjun.mehta@example.test', '$2a$12$DEMO...7q1', DATE '1994-11-03', 'MALE', '+91 90000 00007', 6, 'ACTIVE', TIMESTAMP '2026-02-16 08:45:00', TIMESTAMP '2026-08-04 10:20:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (108, 'Pooja', 'Nair', 'pooja.nair@example.test', '$2a$12$DEMO...5m4', DATE '1995-07-25', 'FEMALE', '+91 90000 00008', 6, 'ACTIVE', TIMESTAMP '2026-02-17 08:45:00', TIMESTAMP '2026-08-03 09:12:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (109, 'Rohit', 'Verma', 'rohit.verma@example.test', '$2a$12$DEMO...1x9', DATE '1989-10-25', 'MALE', '+91 90000 00009', 6, 'ACTIVE', TIMESTAMP '2026-02-18 08:45:00', TIMESTAMP '2026-08-05 13:30:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (110, 'Kavya', 'Jain', 'kavya.jain@example.test', '$2a$12$DEMO...3p0', DATE '1993-01-29', 'FEMALE', '+91 90000 00010', 6, 'ACTIVE', TIMESTAMP '2026-02-19 08:45:00', TIMESTAMP '2026-08-02 16:40:00')
  INTO users (id, first_name, last_name, email, password_hash, dob, gender, mobile, role_id, status, created_at, last_login_at) VALUES (111, 'Imran', 'Ali', 'imran.ali@example.test', '$2a$12$DEMO...9k8', DATE '1991-08-07', 'MALE', '+91 90000 00011', 6, 'ACTIVE', TIMESTAMP '2026-02-20 08:45:00', TIMESTAMP '2026-08-05 19:10:00')
SELECT 1 FROM dual;

INSERT ALL
  INTO branches (id, branch_code, name, address, city, state, pincode, ifsc_code) VALUES (501, 'MB001', 'Mumbai Fort Main', '14 Nariman Point, MG Road', 'Mumbai', 'Maharashtra', '400021', 'MBAG0000001')
  INTO branches (id, branch_code, name, address, city, state, pincode, ifsc_code) VALUES (502, 'MB002', 'Bangalore MG Road', '88 Trinity Circle, MG Road', 'Bengaluru', 'Karnataka', '560001', 'MBAG0000002')
  INTO branches (id, branch_code, name, address, city, state, pincode, ifsc_code) VALUES (503, 'MB003', 'Delhi Connaught Place', '7 Outer Circle', 'New Delhi', 'Delhi', '110001', 'MBAG0000003')
  INTO branches (id, branch_code, name, address, city, state, pincode, ifsc_code) VALUES (504, 'MB004', 'Pune Shivajinagar', '10 FC Road', 'Pune', 'Maharashtra', '411005', 'MBAG0000004')
  INTO branches (id, branch_code, name, address, city, state, pincode, ifsc_code) VALUES (505, 'MB005', 'Chennai T Nagar', '44 Usman Road', 'Chennai', 'Tamil Nadu', '600017', 'MBAG0000005')
SELECT 1 FROM dual;

INSERT ALL
  INTO employees (id, user_id, employee_code, dob, branch_id, designation, reporting_manager_id, joining_date, status) VALUES (1001, 101, 'EMP-001', DATE '1980-01-15', 501, 'System Administrator', NULL, DATE '2020-01-15', 'ACTIVE')
  INTO employees (id, user_id, employee_code, dob, branch_id, designation, reporting_manager_id, joining_date, status) VALUES (1002, 102, 'EMP-002', DATE '1986-12-12', 501, 'Branch Manager', 1001, DATE '2021-03-01', 'ACTIVE')
  INTO employees (id, user_id, employee_code, dob, branch_id, designation, reporting_manager_id, joining_date, status) VALUES (1003, 103, 'EMP-003', DATE '1988-05-22', 501, 'Senior Teller', 1002, DATE '2022-06-15', 'ACTIVE')
  INTO employees (id, user_id, employee_code, dob, branch_id, designation, reporting_manager_id, joining_date, status) VALUES (1004, 104, 'EMP-004', DATE '1984-09-09', 502, 'Auditor', 1001, DATE '2023-01-10', 'ACTIVE')
  INTO employees (id, user_id, employee_code, dob, branch_id, designation, reporting_manager_id, joining_date, status) VALUES (1005, 105, 'EMP-005', DATE '1990-02-10', 503, 'Loan Officer', 1002, DATE '2023-04-20', 'ACTIVE')
SELECT 1 FROM dual;

INSERT ALL
  INTO customers (cif, user_id, customer_type, dob, gender, mobile, pan_number, aadhaar_masked, kyc_status, kyc_verified_at, created_at) VALUES ('CIF900101', 106, 'INDIVIDUAL', DATE '1992-03-18', 'FEMALE', '+91 98200 12345', 'ABCDE1234F', 'XXXX-XXXX-4321', 'VERIFIED', TIMESTAMP '2026-02-01 14:00:00', TIMESTAMP '2026-02-01 13:30:00')
  INTO customers (cif, user_id, customer_type, dob, gender, mobile, pan_number, aadhaar_masked, kyc_status, kyc_verified_at, created_at) VALUES ('CIF900102', 107, 'INDIVIDUAL', DATE '1994-11-03', 'MALE', '+91 98765 43210', 'XYZPS5678G', 'XXXX-XXXX-8765', 'VERIFIED', TIMESTAMP '2026-02-02 10:15:00', TIMESTAMP '2026-02-02 09:50:00')
  INTO customers (cif, user_id, customer_type, dob, gender, mobile, pan_number, aadhaar_masked, kyc_status, kyc_verified_at, created_at) VALUES ('CIF900103', 108, 'CORPORATE', DATE '1989-07-25', 'FEMALE', '+91 98111 12345', 'AACCC1234D', 'XXXX-XXXX-1122', 'VERIFIED', TIMESTAMP '2026-02-03 11:25:00', TIMESTAMP '2026-02-03 11:00:00')
  INTO customers (cif, user_id, customer_type, dob, gender, mobile, pan_number, aadhaar_masked, kyc_status, kyc_verified_at, created_at) VALUES ('CIF900104', 109, 'NRI', DATE '1989-10-25', 'MALE', '+91 99000 10101', 'NRIAX1234K', 'XXXX-XXXX-9988', 'PENDING', NULL, TIMESTAMP '2026-02-04 12:15:00')
  INTO customers (cif, user_id, customer_type, dob, gender, mobile, pan_number, aadhaar_masked, kyc_status, kyc_verified_at, created_at) VALUES ('CIF900105', 110, 'INDIVIDUAL', DATE '1993-01-29', 'FEMALE', '+91 98888 22222', 'PQRSX9876L', 'XXXX-XXXX-7766', 'VERIFIED', TIMESTAMP '2026-02-05 16:45:00', TIMESTAMP '2026-02-05 16:15:00')
  INTO customers (cif, user_id, customer_type, dob, gender, mobile, pan_number, aadhaar_masked, kyc_status, kyc_verified_at, created_at) VALUES ('CIF900106', 111, 'INDIVIDUAL', DATE '1991-08-07', 'MALE', '+91 97777 33333', 'LMNOP4321Q', 'XXXX-XXXX-5544', 'VERIFIED', TIMESTAMP '2026-02-06 09:20:00', TIMESTAMP '2026-02-06 08:55:00')
SELECT 1 FROM dual;

INSERT ALL
  INTO customer_addresses (id, cif, address_type, line1, line2, city, state, pincode) VALUES (2001, 'CIF900101', 'PERMANENT', 'A-401, Sun City Heights', 'Andheri East', 'Mumbai', 'Maharashtra', '400076')
  INTO customer_addresses (id, cif, address_type, line1, line2, city, state, pincode) VALUES (2002, 'CIF900101', 'CURRENT', 'Flat 202, Green Acres', 'Indiranagar', 'Bengaluru', 'Karnataka', '560038')
  INTO customer_addresses (id, cif, address_type, line1, line2, city, state, pincode) VALUES (2003, 'CIF900102', 'PERMANENT', '77 Blue Ridge Society', 'Hinjewadi', 'Pune', 'Maharashtra', '411057')
  INTO customer_addresses (id, cif, address_type, line1, line2, city, state, pincode) VALUES (2004, 'CIF900103', 'PERMANENT', '12 Corporate Park', 'Outer Ring Road', 'Bengaluru', 'Karnataka', '560103')
  INTO customer_addresses (id, cif, address_type, line1, line2, city, state, pincode) VALUES (2005, 'CIF900104', 'CURRENT', '44 Palm Avenue', 'Sector 18', 'Gurugram', 'Haryana', '122018')
  INTO customer_addresses (id, cif, address_type, line1, line2, city, state, pincode) VALUES (2006, 'CIF900105', 'PERMANENT', '19 Lake View Towers', 'Egmore', 'Chennai', 'Tamil Nadu', '600008')
SELECT 1 FROM dual;

INSERT ALL
  INTO documents (id, cif, document_type, document_number_hash, storage_path, verification_status, uploaded_at, verified_by, verified_at, remarks) VALUES (3001, 'CIF900101', 'PAN_CARD', 'SHA-256:e3b0c44298fc1c149afbf4c8996fb924', '/docs/kyc/cif900101_pan.pdf', 'VERIFIED', TIMESTAMP '2026-02-01 14:25:00', 104, TIMESTAMP '2026-02-01 14:40:00', 'PAN verified')
  INTO documents (id, cif, document_type, document_number_hash, storage_path, verification_status, uploaded_at, verified_by, verified_at, remarks) VALUES (3002, 'CIF900101', 'AADHAAR_CARD', 'SHA-256:8f43c59d3f1027aa4b3a2b1c99f0d123', '/docs/kyc/cif900101_aadhaar.pdf', 'VERIFIED', TIMESTAMP '2026-02-01 14:26:00', 104, TIMESTAMP '2026-02-01 14:41:00', 'Aadhaar verified')
  INTO documents (id, cif, document_type, document_number_hash, storage_path, verification_status, uploaded_at, verified_by, verified_at, remarks) VALUES (3003, 'CIF900102', 'PAN_CARD', 'SHA-256:98ab12cd34ef56aa7890bbccddeeff00', '/docs/kyc/cif900102_pan.pdf', 'VERIFIED', TIMESTAMP '2026-02-02 10:20:00', 104, TIMESTAMP '2026-02-02 10:35:00', 'Verified digitally')
  INTO documents (id, cif, document_type, document_number_hash, storage_path, verification_status, uploaded_at, verified_by, verified_at, remarks) VALUES (3004, 'CIF900103', 'INCORPORATION_CERT', 'SHA-256:1122aabb3344ccdd5566eeff77889900', '/docs/kyc/cif900103_incorp.pdf', 'VERIFIED', TIMESTAMP '2026-02-03 11:30:00', 104, TIMESTAMP '2026-02-03 11:45:00', 'Corporate docs complete')
  INTO documents (id, cif, document_type, document_number_hash, storage_path, verification_status, uploaded_at, verified_by, verified_at, remarks) VALUES (3005, 'CIF900104', 'PASSPORT', 'SHA-256:2233bbcc4455ddaa6677889900aabbcc', '/docs/kyc/cif900104_passport.pdf', 'PENDING', TIMESTAMP '2026-02-04 12:20:00', NULL, NULL, 'Awaiting verification')
  INTO documents (id, cif, document_type, document_number_hash, storage_path, verification_status, uploaded_at, verified_by, verified_at, remarks) VALUES (3006, 'CIF900105', 'PAN_CARD', 'SHA-256:3344ccdd5566eeff77889900aabbcc11', '/docs/kyc/cif900105_pan.pdf', 'VERIFIED', TIMESTAMP '2026-02-05 16:50:00', 104, TIMESTAMP '2026-02-05 17:05:00', 'Verified after resubmission')
SELECT 1 FROM dual;

INSERT ALL
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (1, 'SAV-REG', 'Regular Savings Account', 'DEPOSIT', 'SAVINGS', TIMESTAMP '2026-07-01 09:00:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (2, 'SAV-SENIOR', 'Senior Citizen Savings', 'DEPOSIT', 'SAVINGS', TIMESTAMP '2026-07-01 09:05:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (3, 'CUR-BASIC', 'Basic Current Account', 'DEPOSIT', 'CURRENT', TIMESTAMP '2026-07-01 09:10:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (4, 'FD-12M', '12-Month Fixed Deposit', 'DEPOSIT', 'FIXED_DEPOSIT', TIMESTAMP '2026-07-01 09:15:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (5, 'CARD-DEBIT-CLASSIC', 'Debit Card Classic', 'CARD', 'DEBIT_CARD', TIMESTAMP '2026-07-01 09:20:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (6, 'CARD-CREDIT-GOLD', 'Credit Card Gold', 'CARD', 'CREDIT_CARD', TIMESTAMP '2026-07-01 09:25:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (7, 'RD-24M', '24-Month Recurring Deposit', 'DEPOSIT', 'RECURRING_DEPOSIT', TIMESTAMP '2026-07-01 09:30:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (8, 'PL-STANDARD', 'Standard Personal Loan', 'LOAN', 'PERSONAL_LOAN', TIMESTAMP '2026-07-01 09:35:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO products (id, product_code, name, product_category, product_type, created_at, updated_at)
    VALUES (9, 'HL-STANDARD', 'Standard Home Loan', 'LOAN', 'HOME_LOAN', TIMESTAMP '2026-07-01 09:40:00', TIMESTAMP '2026-08-01 09:00:00')
SELECT 1 FROM dual;

INSERT ALL
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (101, 1, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:00:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (201, 2, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:05:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (301, 3, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:10:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (401, 4, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:15:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (501, 5, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:20:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (601, 6, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:25:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (701, 7, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:30:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (801, 8, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:35:00', TIMESTAMP '2026-08-01 09:00:00')
  INTO product_versions (id, product_id, version_number, status, effective_from, effective_to, created_at, updated_at)
    VALUES (901, 9, 1, 'ACTIVE', TIMESTAMP '2026-07-01 00:00:00', NULL, TIMESTAMP '2026-07-01 09:40:00', TIMESTAMP '2026-08-01 09:00:00')
SELECT 1 FROM dual;

INSERT ALL
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (1, 'INTEREST_RATE', 'Interest Rate', 'DECIMAL', 'PERCENT')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (2, 'MIN_BALANCE', 'Minimum Balance', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (3, 'OPENING_FEE', 'Opening Fee', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (4, 'TENURE_MONTHS', 'Tenure Months', 'INTEGER', 'MONTHS')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (5, 'MIN_DEPOSIT', 'Minimum Deposit', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (6, 'MONTHLY_MIN_INSTALLMENT', 'Monthly Minimum Installment', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (7, 'MIN_LOAN_AMOUNT', 'Minimum Loan Amount', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (8, 'MAX_LOAN_AMOUNT', 'Maximum Loan Amount', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (9, 'MAX_TENURE_MONTHS', 'Maximum Tenure Months', 'INTEGER', 'MONTHS')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (10, 'PROCESSING_FEE_PERCENT', 'Processing Fee Percent', 'DECIMAL', 'PERCENT')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (11, 'CREDIT_LIMIT_MIN', 'Minimum Credit Limit', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (12, 'CREDIT_LIMIT_MAX', 'Maximum Credit Limit', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (13, 'ANNUAL_FEE', 'Annual Fee', 'DECIMAL', 'CURRENCY')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (14, 'APR', 'Annual Percentage Rate', 'DECIMAL', 'PERCENT')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (15, 'CARD_VALIDITY_MONTHS', 'Card Validity Months', 'INTEGER', 'MONTHS')
  INTO product_attribute_definitions (id, attribute_code, display_name, data_type, unit)
    VALUES (16, 'CASH_HANDLING', 'Cash Handling', 'STRING', NULL)
SELECT 1 FROM dual;

INSERT ALL
  -- SAV-REG v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1001, 101, 1, '3.000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1002, 101, 2, '1000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1003, 101, 3, '0')

  -- SAV-SENIOR v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1101, 201, 1, '3.500')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1102, 201, 2, '1000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1103, 201, 3, '0')

  -- CUR-BASIC v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1201, 301, 1, '0.000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1202, 301, 2, '10000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1203, 301, 3, '500')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1204, 301, 16, 'BRANCH_ONLY')

  -- FD-12M v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1301, 401, 1, '6.800')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1302, 401, 4, '12')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1303, 401, 5, '5000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1304, 401, 3, '0')

  -- CARD-DEBIT-CLASSIC v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1401, 501, 11, '0')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1402, 501, 12, '50000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1403, 501, 13, '0')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1404, 501, 15, '60')

  -- CARD-CREDIT-GOLD v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1501, 601, 11, '50000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1502, 601, 12, '200000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1503, 601, 13, '999')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1504, 601, 14, '36.000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1505, 601, 15, '36')

  -- RD-24M v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1601, 701, 1, '7.000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1602, 701, 4, '24')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1603, 701, 6, '500')

  -- PL-STANDARD v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1701, 801, 1, '10.500')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1702, 801, 7, '50000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1703, 801, 8, '2000000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1704, 801, 9, '60')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1705, 801, 10, '1.000')

  -- HL-STANDARD v1
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1801, 901, 1, '8.250')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1802, 901, 7, '500000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1803, 901, 8, '50000000')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1804, 901, 9, '360')
  INTO product_version_attributes (id, product_version_id, attribute_id, attribute_value) VALUES (1805, 901, 10, '0.500')
SELECT 1 FROM dual;

INSERT ALL
  INTO accounts (id, account_number, branch_id, product_id, product_version_id, cif, account_type, status, available_balance, opened_at, closed_at, currency)
    VALUES (8001, '100011000101', 501, 1, 101, 'CIF900101', 'SAVINGS', 'OPEN', 45250.00, TIMESTAMP '2026-02-02 11:00:00', NULL, 'INR')
  INTO accounts (id, account_number, branch_id, product_id, product_version_id, cif, account_type, status, available_balance, opened_at, closed_at, currency)
    VALUES (8002, '100011000102', 502, 1, 101, 'CIF900102', 'SAVINGS', 'OPEN', 12800.00, TIMESTAMP '2026-02-11 10:30:00', NULL, 'INR')
  INTO accounts (id, account_number, branch_id, product_id, product_version_id, cif, account_type, status, available_balance, opened_at, closed_at, currency)
    VALUES (8003, '100011000103', 501, 3, 301, 'CIF900103', 'CURRENT', 'OPEN', 87500.75, TIMESTAMP '2026-03-01 09:15:00', NULL, 'INR')
  INTO accounts (id, account_number, branch_id, product_id, product_version_id, cif, account_type, status, available_balance, opened_at, closed_at, currency)
    VALUES (8004, '100011000104', 503, 2, 201, 'CIF900104', 'SAVINGS', 'OPEN', 23400.00, TIMESTAMP '2026-03-10 12:00:00', NULL, 'INR')
  INTO accounts (id, account_number, branch_id, product_id, product_version_id, cif, account_type, status, available_balance, opened_at, closed_at, currency)
    VALUES (8005, '100011000105', 504, 1, 101, 'CIF900105', 'SAVINGS', 'OPEN', 6400.00, TIMESTAMP '2026-03-18 15:30:00', NULL, 'INR')
  INTO accounts (id, account_number, branch_id, product_id, product_version_id, cif, account_type, status, available_balance, opened_at, closed_at, currency)
    VALUES (8006, '100011000106', 502, 3, 301, 'CIF900106', 'CURRENT', 'OPEN', 30210.00, TIMESTAMP '2026-03-20 10:10:00', NULL, 'INR')
SELECT 1 FROM dual;

INSERT ALL
  INTO account_holders (id, account_id, cif, holder_role, is_primary, created_at) VALUES (1, 8001, 'CIF900101', 'PRIMARY', 'Y', TIMESTAMP '2026-02-02 11:05:00')
  INTO account_holders (id, account_id, cif, holder_role, is_primary, created_at) VALUES (2, 8002, 'CIF900102', 'PRIMARY', 'Y', TIMESTAMP '2026-02-11 10:35:00')
  INTO account_holders (id, account_id, cif, holder_role, is_primary, created_at) VALUES (3, 8003, 'CIF900103', 'PRIMARY', 'Y', TIMESTAMP '2026-03-01 09:20:00')
  INTO account_holders (id, account_id, cif, holder_role, is_primary, created_at) VALUES (4, 8004, 'CIF900104', 'PRIMARY', 'Y', TIMESTAMP '2026-03-10 12:05:00')
  INTO account_holders (id, account_id, cif, holder_role, is_primary, created_at) VALUES (5, 8005, 'CIF900105', 'PRIMARY', 'Y', TIMESTAMP '2026-03-18 15:35:00')
  INTO account_holders (id, account_id, cif, holder_role, is_primary, created_at) VALUES (6, 8006, 'CIF900106', 'PRIMARY', 'Y', TIMESTAMP '2026-03-20 10:15:00')
SELECT 1 FROM dual;

INSERT ALL
  INTO secondary_link_products (account_id, product_id, product_version_id, linked_at)
    VALUES (8001, 5, 501, TIMESTAMP '2026-02-02 11:20:00')
  INTO secondary_link_products (account_id, product_id, product_version_id, linked_at)
    VALUES (8002, 5, 501, TIMESTAMP '2026-02-11 10:45:00')
  INTO secondary_link_products (account_id, product_id, product_version_id, linked_at)
    VALUES (8003, 6, 601, TIMESTAMP '2026-03-01 09:35:00')
  INTO secondary_link_products (account_id, product_id, product_version_id, linked_at)
    VALUES (8004, 5, 501, TIMESTAMP '2026-03-10 12:20:00')
  INTO secondary_link_products (account_id, product_id, product_version_id, linked_at)
    VALUES (8005, 5, 501, TIMESTAMP '2026-03-18 15:50:00')
  INTO secondary_link_products (account_id, product_id, product_version_id, linked_at)
    VALUES (8006, 6, 601, TIMESTAMP '2026-03-20 10:30:00')
SELECT 1 FROM dual;

INSERT ALL
  INTO transactions (id, txn_ref, account_id, counter_account_id, txn_type, amount, txn_mode, description, status, created_at, created_by) VALUES (90001, 'TXN20260801001', 8001, NULL, 'CREDIT', 50000.00, 'CASH', 'Initial deposit', 'SUCCESS', TIMESTAMP '2026-02-02 11:15:00', 103)
  INTO transactions (id, txn_ref, account_id, counter_account_id, txn_type, amount, txn_mode, description, status, created_at, created_by) VALUES (90002, 'TXN20260802045', 8001, 8002, 'DEBIT', 4750.00, 'UPI', 'Transfer to Ananya', 'SUCCESS', TIMESTAMP '2026-08-04 15:30:00', 106)
  INTO transactions (id, txn_ref, account_id, counter_account_id, txn_type, amount, txn_mode, description, status, created_at, created_by) VALUES (90003, 'TXN20260802046', 8002, NULL, 'DEBIT', 1800.00, 'CASH', 'Branch cash withdrawal', 'SUCCESS', TIMESTAMP '2026-08-04 16:05:00', 103)
  INTO transactions (id, txn_ref, account_id, counter_account_id, txn_type, amount, txn_mode, description, status, created_at, created_by) VALUES (90004, 'TXN20260803012', 8003, 8004, 'DEBIT', 22000.00, 'NEFT', 'Vendor payment', 'SUCCESS', TIMESTAMP '2026-08-05 10:25:00', 102)
  INTO transactions (id, txn_ref, account_id, counter_account_id, txn_type, amount, txn_mode, description, status, created_at, created_by) VALUES (90005, 'TXN20260803013', 8005, NULL, 'CREDIT', 3000.00, 'CARD', 'Card refund credit', 'SUCCESS', TIMESTAMP '2026-08-05 13:45:00', 110)
  INTO transactions (id, txn_ref, account_id, counter_account_id, txn_type, amount, txn_mode, description, status, created_at, created_by) VALUES (90006, 'TXN20260803014', 8006, NULL, 'DEBIT', 1250.00, 'CHEQUE', 'Cheque presented for clearing', 'PENDING', TIMESTAMP '2026-08-05 16:10:00', 103)
SELECT 1 FROM dual;

INSERT ALL
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70001, 90001, 8001, 'CREDIT', 50000.00, DATE '2026-02-02', 'Cash deposit - Branch 501', TIMESTAMP '2026-02-02 11:15:10')
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70002, 90002, 8001, 'DEBIT', 4750.00, DATE '2026-08-04', 'UPI outward to 100011000102', TIMESTAMP '2026-08-04 15:30:10')
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70003, 90002, 8002, 'CREDIT', 4750.00, DATE '2026-08-04', 'UPI inward from 100011000101', TIMESTAMP '2026-08-04 15:30:11')
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70004, 90003, 8002, 'DEBIT', 1800.00, DATE '2026-08-04', 'Cash withdrawal at branch', TIMESTAMP '2026-08-04 16:05:10')
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70005, 90004, 8003, 'DEBIT', 22000.00, DATE '2026-08-05', 'NEFT vendor payment posted', TIMESTAMP '2026-08-05 10:25:10')
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70006, 90004, 8004, 'CREDIT', 22000.00, DATE '2026-08-05', 'NEFT inward from 100011000103', TIMESTAMP '2026-08-05 10:25:11')
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70007, 90005, 8005, 'CREDIT', 3000.00, DATE '2026-08-05', 'Card refund credit', TIMESTAMP '2026-08-05 13:45:10')
  INTO ledger_entries (id, txn_id, account_id, entry_type, amount, posting_date, narration, created_at) VALUES (70008, 90006, 8006, 'DEBIT', 1250.00, DATE '2026-08-05', 'Cheque clearance pending', TIMESTAMP '2026-08-05 16:10:10')
SELECT 1 FROM dual;

COMMIT;

-- Helpful sanity queries for local development:
-- SELECT COUNT(*) FROM users;
-- SELECT COUNT(*) FROM customers;
-- SELECT COUNT(*) FROM accounts;
-- SELECT COUNT(*) FROM transactions;
