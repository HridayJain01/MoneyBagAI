-- Seed fixtures. See docs/SEED_FIXTURES.md for the cross-service contract.
--
-- Every identifier here is a hardcoded literal. No seed migration may look up
-- another service: services start in parallel, so a runtime cross-service
-- dependency during migration would deadlock startup.
--
-- Password for all seed users is  Password@123
-- The BCrypt hash below is PRECOMPUTED and pasted as a literal, never hashed at
-- runtime -- a seed that produces a different hash on every run is not a fixture.

INSERT INTO permissions (permission_id, permission_code, description, service_name, action) VALUES
    ( 1, 'USER_MANAGE',                  'Manage users, status and role assignment', 'identity-service',     'MANAGE'),
    ( 2, 'ROLE_PERMISSION_MANAGE',       'Manage roles and permission mappings',      'identity-service',     'MANAGE'),
    ( 3, 'CUSTOMER_READ',                'Read customer and KYC summary',             'customer-service',     'READ'),
    ( 4, 'CUSTOMER_UPDATE',              'Update customer profile',                   'customer-service',     'UPDATE'),
    ( 5, 'KYC_VERIFY',                   'Verify or reject KYC documents',            'customer-service',     'APPROVE'),
    ( 6, 'PRODUCT_READ',                 'View product catalogue and rules',          'product-service',      'READ'),
    ( 7, 'PRODUCT_MANAGE',               'Create and change products and rates',      'product-service',      'MANAGE'),
    ( 8, 'ACCOUNT_OPEN',                 'Create an account application',             'account-service',      'CREATE'),
    ( 9, 'ACCOUNT_APPROVE',              'Approve or reject account opening',         'account-service',      'APPROVE'),
    (10, 'ACCOUNT_VIEW',                 'View permitted account details/balances',   'account-service',      'READ'),
    (11, 'ACCOUNT_VIEW_ALL_BRANCHES',    'View accounts outside own branch',          'account-service',      'READ'),
    (12, 'ACCOUNT_STATUS_MANAGE',        'Freeze, unfreeze, block or close account',  'account-service',      'MANAGE'),
    (13, 'TRANSACTION_CREATE',           'Initiate an allowed financial transaction', 'transaction-service',  'CREATE'),
    (14, 'TRANSACTION_APPROVE',          'Approve maker-checker transactions',        'transaction-service',  'APPROVE'),
    (15, 'TRANSACTION_REVERSE',          'Create compensating reversal',              'transaction-service',  'REVERSE'),
    (16, 'TRANSACTION_VIEW',             'View transaction history',                  'transaction-service',  'READ'),
    (17, 'TRANSACTION_VIEW_ALL_BRANCHES','View transactions outside own branch',      'transaction-service',  'READ'),
    (18, 'TRANSACTION_CANCEL',           'Cancel own pending transaction',            'transaction-service',  'CANCEL'),
    (19, 'TRANSACTION_CANCEL_ANY',       'Cancel any pending transaction',            'transaction-service',  'CANCEL'),
    (20, 'RECONCILIATION_MANAGE',        'Manage reconciliation exceptions',          'transaction-service',  'MANAGE'),
    (21, 'STATEMENT_VIEW',               'Generate permitted statements',             'statement-service',    'READ'),
    (22, 'REPORT_VIEW',                  'View operational reports',                  'statement-service',    'READ'),
    (23, 'REPORT_ADMIN',                 'Administer report schedules',               'statement-service',    'MANAGE'),
    (24, 'AUDIT_VIEW',                   'Search and export audit records',           'audit-service',        'READ'),
    (25, 'CONFIG_MANAGE',                'Change limits, policies and feature flags', 'configuration-service','MANAGE'),
    (26, 'BRANCH_MANAGE',                'Create and change branches',                'branch-employee',      'MANAGE'),
    (27, 'EMPLOYEE_MANAGE',              'Create and change employees',               'branch-employee',      'MANAGE'),
    (28, 'NOTIFICATION_MANAGE',          'Retry and administer notifications',        'notification-service', 'MANAGE');

INSERT INTO roles (role_id, role_name, description) VALUES
    (1, 'TELLER',         'Branch teller for counter operations'),
    (2, 'CHECKER',        'Branch checker for maker-checker approvals'),
    (3, 'BRANCH_MANAGER', 'Branch supervisor for approvals and overrides'),
    (4, 'OPS_ADMIN',      'Platform administrator');

-- TELLER: makes work, cannot approve it.
INSERT INTO role_permissions (role_id, permission_id) VALUES
    (1, 3), (1, 4), (1, 6), (1, 8), (1, 10), (1, 13), (1, 16), (1, 18), (1, 21);

-- CHECKER: teller plus the approval permissions.
INSERT INTO role_permissions (role_id, permission_id) VALUES
    (2, 3), (2, 4), (2, 6), (2, 8), (2, 10), (2, 13), (2, 16), (2, 18), (2, 21),
    (2, 5), (2, 9), (2, 14);

-- BRANCH_MANAGER: checker plus cross-branch visibility, reversal and status control.
INSERT INTO role_permissions (role_id, permission_id) VALUES
    (3, 3), (3, 4), (3, 6), (3, 8), (3, 10), (3, 13), (3, 16), (3, 18), (3, 21),
    (3, 5), (3, 9), (3, 14),
    (3, 11), (3, 12), (3, 15), (3, 17), (3, 19), (3, 22);

-- OPS_ADMIN: everything.
INSERT INTO role_permissions (role_id, permission_id)
    SELECT 4, permission_id FROM permissions;

-- employee_id / branch_code are logical references to branch-employee-service.
-- emp 1002 at BR001 is REQUIRED: customer-service seed data already names it as
-- the relationship manager for CIF900101.
INSERT INTO users (user_id, username, email, password_hash, full_name, mobile, status,
                   failed_attempts, employee_id, branch_code, password_changed_at,
                   created_at, updated_at) VALUES
    (1, 'teller1',  'teller1@moneybags.test',  '$2a$10$fXCPOVT2U/KYqZe3bL4WM.n7ZiJyxyVRCD1x6SRw000njIKxukGFO', 'Amit Patel',       '9000000001', 'ACTIVE', 0, '1001', 'BR001', NOW(6), NOW(6), NOW(6)),
    (2, 'checker1', 'checker1@moneybags.test', '$2a$10$fXCPOVT2U/KYqZe3bL4WM.n7ZiJyxyVRCD1x6SRw000njIKxukGFO', 'Priya Nair',       '9000000002', 'ACTIVE', 0, '1002', 'BR001', NOW(6), NOW(6), NOW(6)),
    (3, 'manager1', 'manager1@moneybags.test', '$2a$10$fXCPOVT2U/KYqZe3bL4WM.n7ZiJyxyVRCD1x6SRw000njIKxukGFO', 'Rajesh Sharma',    '9000000003', 'ACTIVE', 0, '1003', 'BR002', NOW(6), NOW(6), NOW(6)),
    (4, 'opsadmin', 'opsadmin@moneybags.test', '$2a$10$fXCPOVT2U/KYqZe3bL4WM.n7ZiJyxyVRCD1x6SRw000njIKxukGFO', 'System Operator',  '9000000004', 'ACTIVE', 0, '1004', 'BR001', NOW(6), NOW(6), NOW(6));

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1),   -- teller1  -> TELLER
    (2, 2),   -- checker1 -> CHECKER   (maker/checker pair with teller1, same branch BR001)
    (3, 3),   -- manager1 -> BRANCH_MANAGER
    (4, 4);   -- opsadmin -> OPS_ADMIN

-- Explicit ids above would otherwise collide with the first runtime insert.
ALTER TABLE users       AUTO_INCREMENT = 2000;
ALTER TABLE roles       AUTO_INCREMENT = 100;
ALTER TABLE permissions AUTO_INCREMENT = 100;
