MERGE INTO branches t USING (
  SELECT 501 id, 'BR001' code, 'Mumbai Fort Main' name, '14 Nariman Point, MG Road' address,
         'Mumbai' city, 'Maharashtra' state, '400021' pincode, 'MBAG0000001' ifsc FROM dual UNION ALL
  SELECT 502, 'BR002', 'Pune Hinjewadi', '77 Rajiv Gandhi Infotech Park', 'Pune', 'Maharashtra', '411057', 'MBAG0000002' FROM dual
) s ON (t.id = s.id)
WHEN NOT MATCHED THEN INSERT (id, branch_code, name, address, city, state, pincode, ifsc_code, status)
VALUES (s.id, s.code, s.name, s.address, s.city, s.state, s.pincode, s.ifsc, 'ACTIVE');

MERGE INTO branch_working_hours t USING (
  SELECT b.id branch_id, d.day_of_week,
         CASE WHEN d.day_of_week = 'SUNDAY' THEN NULL WHEN d.day_of_week = 'SATURDAY' THEN TO_TIMESTAMP('10:00','HH24:MI') ELSE TO_TIMESTAMP('10:00','HH24:MI') END open_time,
         CASE WHEN d.day_of_week = 'SUNDAY' THEN NULL WHEN d.day_of_week = 'SATURDAY' THEN TO_TIMESTAMP('13:00','HH24:MI') ELSE TO_TIMESTAMP('16:00','HH24:MI') END close_time,
         CASE WHEN d.day_of_week = 'SUNDAY' THEN 'Y' ELSE 'N' END is_closed
  FROM (SELECT 501 id FROM dual UNION ALL SELECT 502 FROM dual) b
  CROSS JOIN (SELECT 'MONDAY' day_of_week FROM dual UNION ALL SELECT 'TUESDAY' FROM dual UNION ALL
              SELECT 'WEDNESDAY' FROM dual UNION ALL SELECT 'THURSDAY' FROM dual UNION ALL
              SELECT 'FRIDAY' FROM dual UNION ALL SELECT 'SATURDAY' FROM dual UNION ALL SELECT 'SUNDAY' FROM dual) d
) s ON (t.branch_id = s.branch_id AND t.day_of_week = s.day_of_week)
WHEN NOT MATCHED THEN INSERT (branch_id, day_of_week, open_time, close_time, is_closed)
VALUES (s.branch_id, s.day_of_week, s.open_time, s.close_time, s.is_closed);

MERGE INTO branch_holidays t USING (
  SELECT 501 branch_id, DATE '2026-01-26' holiday_date, 'Republic Day' description FROM dual UNION ALL
  SELECT 501, DATE '2026-08-15', 'Independence Day' FROM dual UNION ALL
  SELECT 502, DATE '2026-01-26', 'Republic Day' FROM dual UNION ALL
  SELECT 502, DATE '2026-08-15', 'Independence Day' FROM dual
) s ON (t.branch_id = s.branch_id AND t.holiday_date = s.holiday_date)
WHEN NOT MATCHED THEN INSERT (branch_id, holiday_date, description)
VALUES (s.branch_id, s.holiday_date, s.description);

MERGE INTO employees t USING (
  SELECT 1001 id, 1 user_id, 'EMP-001' code, DATE '1994-08-09' dob, 501 branch_id, 'Teller' designation, 1002 manager_id, DATE '2022-06-15' joining_date FROM dual UNION ALL
  SELECT 1002, 2, 'EMP-002', DATE '1988-11-22', 501, 'Branch Manager', NULL, DATE '2021-03-01' FROM dual UNION ALL
  SELECT 1003, 3, 'EMP-003', DATE '1985-05-14', 502, 'Branch Manager', NULL, DATE '2020-01-15' FROM dual UNION ALL
  SELECT 1004, 4, 'EMP-004', DATE '1990-02-20', 501, 'Operations Administrator', 1002, DATE '2021-09-01' FROM dual UNION ALL
  SELECT 1005, 5, 'EMP-005', DATE '1996-04-17', 502, 'Teller', 1003, DATE '2024-07-01' FROM dual
) s ON (t.id = s.id)
WHEN NOT MATCHED THEN INSERT (id, user_id, employee_code, dob, branch_id, designation, reporting_manager_id, joining_date, status)
VALUES (s.id, s.user_id, s.code, s.dob, s.branch_id, s.designation, s.manager_id, s.joining_date, 'ACTIVE');

MERGE INTO employee_approval_authority t USING (
  SELECT 1001 employee_id, 'TRANSACTION_APPROVE' action_type, 0 max_amount FROM dual UNION ALL
  SELECT 1002, 'TRANSACTION_APPROVE', 500000 FROM dual UNION ALL SELECT 1002, 'ACCOUNT_APPROVE', 1000000 FROM dual UNION ALL
  SELECT 1002, 'TRANSACTION_REVERSE', 200000 FROM dual UNION ALL SELECT 1003, 'TRANSACTION_APPROVE', 500000 FROM dual UNION ALL
  SELECT 1003, 'ACCOUNT_APPROVE', 1000000 FROM dual UNION ALL SELECT 1004, 'ACCOUNT_APPROVE', 250000 FROM dual
) s ON (t.employee_id = s.employee_id AND t.action_type = s.action_type)
WHEN NOT MATCHED THEN INSERT (employee_id, action_type, max_amount, currency)
VALUES (s.employee_id, s.action_type, s.max_amount, 'INR');
