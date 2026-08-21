MERGE INTO permissions t USING (
  SELECT 1 id,'USER_MANAGE' code,'Manage users, status and role assignment' description,'identity-service' service_name,'MANAGE' action FROM dual UNION ALL
  SELECT 2,'ROLE_PERMISSION_MANAGE','Manage roles and permission mappings','identity-service','MANAGE' FROM dual UNION ALL
  SELECT 3,'CUSTOMER_READ','Read customer and KYC summary','customer-service','READ' FROM dual UNION ALL
  SELECT 4,'CUSTOMER_UPDATE','Update customer profile','customer-service','UPDATE' FROM dual UNION ALL
  SELECT 5,'KYC_VERIFY','Verify or reject KYC documents','kyc-service','APPROVE' FROM dual UNION ALL
  SELECT 6,'PRODUCT_READ','View product catalogue and rules','product-service','READ' FROM dual UNION ALL
  SELECT 7,'PRODUCT_MANAGE','Create and change products and rates','product-service','MANAGE' FROM dual UNION ALL
  SELECT 8,'ACCOUNT_OPEN','Create an account application','account-service','CREATE' FROM dual UNION ALL
  SELECT 9,'ACCOUNT_APPROVE','Approve or reject account opening','account-service','APPROVE' FROM dual UNION ALL
  SELECT 10,'ACCOUNT_VIEW','View permitted account details/balances','account-service','READ' FROM dual UNION ALL
  SELECT 11,'ACCOUNT_VIEW_ALL_BRANCHES','View accounts outside own branch','account-service','READ' FROM dual UNION ALL
  SELECT 12,'ACCOUNT_STATUS_MANAGE','Freeze, unfreeze, block or close account','account-service','MANAGE' FROM dual UNION ALL
  SELECT 13,'TRANSACTION_CREATE','Initiate an allowed financial transaction','transaction-service','CREATE' FROM dual UNION ALL
  SELECT 14,'TRANSACTION_APPROVE','Approve maker-checker transactions','transaction-service','APPROVE' FROM dual UNION ALL
  SELECT 15,'TRANSACTION_REVERSE','Create compensating reversal','transaction-service','REVERSE' FROM dual UNION ALL
  SELECT 16,'TRANSACTION_VIEW','View transaction history','transaction-service','READ' FROM dual UNION ALL
  SELECT 17,'TRANSACTION_VIEW_ALL_BRANCHES','View transactions outside own branch','transaction-service','READ' FROM dual UNION ALL
  SELECT 18,'TRANSACTION_CANCEL','Cancel own pending transaction','transaction-service','CANCEL' FROM dual UNION ALL
  SELECT 19,'TRANSACTION_CANCEL_ANY','Cancel any pending transaction','transaction-service','CANCEL' FROM dual UNION ALL
  SELECT 20,'RECONCILIATION_MANAGE','Manage reconciliation exceptions','transaction-service','MANAGE' FROM dual UNION ALL
  SELECT 21,'STATEMENT_VIEW','Generate permitted statements','statement-service','READ' FROM dual UNION ALL
  SELECT 22,'REPORT_VIEW','View operational reports','statement-service','READ' FROM dual UNION ALL
  SELECT 23,'REPORT_ADMIN','Administer report schedules','statement-service','MANAGE' FROM dual UNION ALL
  SELECT 24,'AUDIT_VIEW','Search and export audit records','audit-service','READ' FROM dual UNION ALL
  SELECT 25,'CONFIG_MANAGE','Change limits, policies and feature flags','configuration-service','MANAGE' FROM dual UNION ALL
  SELECT 26,'BRANCH_MANAGE','Create and change branches','branch-employee','MANAGE' FROM dual UNION ALL
  SELECT 27,'EMPLOYEE_MANAGE','Create and change employees','branch-employee','MANAGE' FROM dual UNION ALL
  SELECT 28,'NOTIFICATION_MANAGE','Retry and administer notifications','notification-service','MANAGE' FROM dual
) s ON (t.permission_id = s.id)
WHEN NOT MATCHED THEN INSERT (permission_id,permission_code,description,service_name,action)
VALUES (s.id,s.code,s.description,s.service_name,s.action);

MERGE INTO roles t USING (
  SELECT 1 id,'TELLER' name,'Branch teller for counter operations' description FROM dual UNION ALL
  SELECT 2,'CHECKER','Branch checker for maker-checker approvals' FROM dual UNION ALL
  SELECT 3,'BRANCH_MANAGER','Branch supervisor for approvals and overrides' FROM dual UNION ALL
  SELECT 4,'OPS_ADMIN','Platform administrator' FROM dual UNION ALL
  SELECT 5,'CUSTOMER','Self-registered digital banking customer' FROM dual
) s ON (t.role_name = s.name)
WHEN NOT MATCHED THEN INSERT (role_id,role_name,description) VALUES (s.id,s.name,s.description);

MERGE INTO role_permissions t USING (
  SELECT r.role_id, p.permission_id FROM roles r CROSS JOIN permissions p
  WHERE r.role_name = 'OPS_ADMIN'
     OR (r.role_name = 'TELLER' AND p.permission_id IN (3,4,6,8,10,13,16,18,21))
     OR (r.role_name = 'CHECKER' AND p.permission_id IN (3,4,5,6,8,9,10,13,14,16,18,21))
     OR (r.role_name = 'BRANCH_MANAGER' AND p.permission_id IN (3,4,5,6,8,9,10,11,12,13,14,15,16,17,18,19,21,22))
) s ON (t.role_id = s.role_id AND t.permission_id = s.permission_id)
WHEN NOT MATCHED THEN INSERT (role_id,permission_id) VALUES (s.role_id,s.permission_id);

MERGE INTO users t USING (
  SELECT 1 id,'teller1' username,'teller1@moneybags.test' email,'Amit Patel' full_name,'9000000001' mobile,'1001' employee_id,'BR001' branch_code FROM dual UNION ALL
  SELECT 2,'checker1','checker1@moneybags.test','Priya Nair','9000000002','1002','BR001' FROM dual UNION ALL
  SELECT 3,'manager1','manager1@moneybags.test','Rajesh Sharma','9000000003','1003','BR002' FROM dual UNION ALL
  SELECT 4,'opsadmin','opsadmin@moneybags.test','System Operator','9000000004','1004','BR001' FROM dual UNION ALL
  SELECT 5,'teller2','teller2@moneybags.test','Neha Kulkarni','9000000005','1005','BR002' FROM dual
) s ON (t.user_id = s.id)
WHEN NOT MATCHED THEN INSERT
  (user_id,username,email,password_hash,full_name,mobile,status,failed_attempts,employee_id,branch_code,
   password_changed_at,created_at,updated_at)
VALUES (s.id,s.username,s.email,'$2a$10$fXCPOVT2U/KYqZe3bL4WM.n7ZiJyxyVRCD1x6SRw000njIKxukGFO',s.full_name,
        s.mobile,'ACTIVE',0,s.employee_id,s.branch_code,SYSTIMESTAMP,SYSTIMESTAMP,SYSTIMESTAMP);

MERGE INTO user_roles t USING (
  SELECT 1 user_id,1 role_id FROM dual UNION ALL SELECT 2,2 FROM dual UNION ALL
  SELECT 3,3 FROM dual UNION ALL SELECT 4,4 FROM dual UNION ALL SELECT 5,1 FROM dual
) s ON (t.user_id=s.user_id AND t.role_id=s.role_id)
WHEN NOT MATCHED THEN INSERT (user_id,role_id) VALUES (s.user_id,s.role_id);
