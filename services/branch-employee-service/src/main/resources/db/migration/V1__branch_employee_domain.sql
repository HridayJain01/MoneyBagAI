-- Branch and employee schema, converted from the original Oracle model.
--
-- branches.id and employees.id are NOT auto-increment: the service assigns them with
-- findMaxId() + 1 (BranchService:45, EmployeeService:69), so the database must not
-- generate them.

CREATE TABLE branches (
    id          BIGINT       NOT NULL,
    branch_code VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    city        VARCHAR(255),
    state       VARCHAR(255),
    pincode     VARCHAR(255),
    ifsc_code   VARCHAR(255) NOT NULL,
    status      VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_branches_code UNIQUE (branch_code),
    CONSTRAINT uk_branches_ifsc UNIQUE (ifsc_code)
) ENGINE = InnoDB;

CREATE TABLE branch_working_hours (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id    BIGINT       NOT NULL,
    day_of_week  VARCHAR(255) NOT NULL,
    open_time    VARCHAR(255),
    close_time   VARCHAR(255),
    -- Stored as CHAR(1) 'Y'/'N' to match the entity's @JdbcTypeCode(Types.CHAR).
    is_closed    CHAR(1),
    PRIMARY KEY (id),
    KEY idx_working_hours_branch (branch_id),
    CONSTRAINT fk_working_hours_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
) ENGINE = InnoDB;

CREATE TABLE branch_holidays (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id    BIGINT       NOT NULL,
    holiday_date DATE         NOT NULL,
    description  VARCHAR(255),
    PRIMARY KEY (id),
    KEY idx_holidays_branch (branch_id),
    CONSTRAINT fk_holidays_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
) ENGINE = InnoDB;

CREATE TABLE employees (
    id                   BIGINT      NOT NULL,
    -- Logical reference to identity-service users.user_id.
    user_id              BIGINT      NOT NULL,
    employee_code        VARCHAR(30) NOT NULL,
    dob                  DATE,
    branch_id            BIGINT      NOT NULL,
    designation          VARCHAR(100),
    reporting_manager_id BIGINT,
    joining_date         DATE,
    status               VARCHAR(15) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employees_user UNIQUE (user_id),
    CONSTRAINT uk_employees_code UNIQUE (employee_code),
    KEY idx_employees_branch (branch_id),
    KEY idx_employees_manager (reporting_manager_id),
    CONSTRAINT fk_employees_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
) ENGINE = InnoDB;

CREATE TABLE employee_approval_authority (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    employee_id BIGINT        NOT NULL,
    action_type VARCHAR(50)   NOT NULL,
    max_amount  DECIMAL(18,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL,
    -- The entity maps this insertable=false/updatable=false, so the DB must supply it.
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                              ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_approval_authority UNIQUE (employee_id, action_type),
    CONSTRAINT fk_approval_authority_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT chk_approval_max_amount CHECK (max_amount >= 0)
) ENGINE = InnoDB;

CREATE TABLE employee_branch_transfers (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT      NOT NULL,
    from_branch_id BIGINT,
    to_branch_id   BIGINT      NOT NULL,
    -- Also insertable=false/updatable=false in the entity.
    transferred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    remarks        VARCHAR(200),
    PRIMARY KEY (id),
    KEY idx_transfers_employee (employee_id),
    CONSTRAINT fk_transfers_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
) ENGINE = InnoDB;
