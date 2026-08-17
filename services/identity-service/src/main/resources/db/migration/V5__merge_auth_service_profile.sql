-- Fields and default role imported from the standalone auth-service. Existing
-- MoneyBags user, role and permission tables remain authoritative.
ALTER TABLE users
    ADD COLUMN first_name VARCHAR(100) NULL AFTER full_name,
    ADD COLUMN last_name VARCHAR(100) NULL AFTER first_name,
    ADD COLUMN date_of_birth DATE NULL AFTER last_name,
    ADD COLUMN gender VARCHAR(20) NULL AFTER date_of_birth;

INSERT INTO roles (role_name, description)
SELECT 'CUSTOMER', 'Self-registered digital banking customer'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'CUSTOMER');
