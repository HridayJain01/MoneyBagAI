-- Seed fixtures. See docs/SEED_FIXTURES.md.
--
-- branch_code BR001/BR002 and employee ids 1001-1004 are a CROSS-SERVICE contract:
--   * identity-service seeds users 1-4 with these exact employee_id / branch_code values
--   * customer-service names emp 1002 as the relationship manager for CIF900101
--   * account-service seeds accounts against branch_code BR001/BR002
-- Changing any of them here breaks branch scoping at the gateway and in statements.

INSERT INTO branches (id, branch_code, name, address, city, state, pincode, ifsc_code, status) VALUES
    (501, 'BR001', 'Mumbai Fort Main',  '14 Nariman Point, MG Road', 'Mumbai', 'Maharashtra', '400021', 'MBAG0000001', 'ACTIVE'),
    (502, 'BR002', 'Pune Hinjewadi',    '77 Rajiv Gandhi Infotech Park', 'Pune', 'Maharashtra', '411057', 'MBAG0000002', 'ACTIVE');

-- Monday-Friday 10:00-16:00, Saturday half day, Sunday closed.
INSERT INTO branch_working_hours (branch_id, day_of_week, open_time, close_time, is_closed) VALUES
    (501, 'MONDAY',    '10:00', '16:00', 'N'),
    (501, 'TUESDAY',   '10:00', '16:00', 'N'),
    (501, 'WEDNESDAY', '10:00', '16:00', 'N'),
    (501, 'THURSDAY',  '10:00', '16:00', 'N'),
    (501, 'FRIDAY',    '10:00', '16:00', 'N'),
    (501, 'SATURDAY',  '10:00', '13:00', 'N'),
    (501, 'SUNDAY',     NULL,    NULL,   'Y'),
    (502, 'MONDAY',    '10:00', '16:00', 'N'),
    (502, 'TUESDAY',   '10:00', '16:00', 'N'),
    (502, 'WEDNESDAY', '10:00', '16:00', 'N'),
    (502, 'THURSDAY',  '10:00', '16:00', 'N'),
    (502, 'FRIDAY',    '10:00', '16:00', 'N'),
    (502, 'SATURDAY',  '10:00', '13:00', 'N'),
    (502, 'SUNDAY',     NULL,    NULL,   'Y');

INSERT INTO branch_holidays (branch_id, holiday_date, description) VALUES
    (501, '2026-01-26', 'Republic Day'),
    (501, '2026-08-15', 'Independence Day'),
    (502, '2026-01-26', 'Republic Day'),
    (502, '2026-08-15', 'Independence Day');

-- user_id maps 1:1 onto identity-service seed users teller1/checker1/manager1/opsadmin.
-- 1001 and 1002 are BOTH at branch 501 so the maker-checker pair shares a branch;
-- an approval across branches would be rejected by branch scoping.
INSERT INTO employees (id, user_id, employee_code, dob, branch_id, designation,
                       reporting_manager_id, joining_date, status) VALUES
    (1001, 1, 'EMP-001', '1994-08-09', 501, 'Teller',                 1002, '2022-06-15', 'ACTIVE'),
    (1002, 2, 'EMP-002', '1988-11-22', 501, 'Branch Manager',         NULL, '2021-03-01', 'ACTIVE'),
    (1003, 3, 'EMP-003', '1985-05-14', 502, 'Branch Manager',         NULL, '2020-01-15', 'ACTIVE'),
    (1004, 4, 'EMP-004', '1990-02-20', 501, 'Operations Administrator', 1002, '2021-09-01', 'ACTIVE');

INSERT INTO employee_approval_authority (employee_id, action_type, max_amount, currency) VALUES
    (1001, 'TRANSACTION_APPROVE',      0.00, 'INR'),
    (1002, 'TRANSACTION_APPROVE', 500000.00, 'INR'),
    (1002, 'ACCOUNT_APPROVE',    1000000.00, 'INR'),
    (1002, 'TRANSACTION_REVERSE', 200000.00, 'INR'),
    (1003, 'TRANSACTION_APPROVE', 500000.00, 'INR'),
    (1003, 'ACCOUNT_APPROVE',    1000000.00, 'INR'),
    (1004, 'ACCOUNT_APPROVE',     250000.00, 'INR');

-- Explicit ids above would otherwise collide with the first runtime insert.
ALTER TABLE branch_working_hours        AUTO_INCREMENT = 1000;
ALTER TABLE branch_holidays             AUTO_INCREMENT = 1000;
ALTER TABLE employee_approval_authority AUTO_INCREMENT = 1000;
ALTER TABLE employee_branch_transfers   AUTO_INCREMENT = 1000;
