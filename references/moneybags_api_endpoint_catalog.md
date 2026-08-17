# Moneybags Banking System - API Endpoint Catalogue

## Scope and decisions

This catalogue turns the two supplied designs into a versioned REST API surface. It covers every entity explicitly shown in the schema and the operational capabilities required in the revised specification.

- Public base path: `/api/v1`; internal service-to-service paths use `/internal/v1` and are not exposed through the gateway.
- All endpoints require `Authorization: Bearer <JWT>` unless marked **public**.
- Every mutating call carries `X-Correlation-Id`; every financial POST also requires a unique `Idempotency-Key` (persisted as `requestRef` / `txnRef`).
- A customer may access only resources owned by their CIF. Staff access is branch-scoped. Maker and approver must be different users.
- `DELETE` is intentionally absent for financial, ledger and audit records. Use status changes, cancellation, closure, or compensating reversal.
- **Schema** means a direct mapping to a supplied table. **Derived** means it is required by a named responsibility in the revised specification and needs its own service-owned table/read model.

## Common response and query conventions

List endpoints accept `page`, `size`, `sort`, and `status` unless a more specific filter is shown. Return a standard page envelope:

```json
{ "items": [], "page": 0, "size": 25, "totalItems": 0, "totalPages": 0 }
```

Use `201 Created` for new resources, `202 Accepted` for queued approval/export/job work, `409 Conflict` for duplicate idempotency keys or invalid state transitions, and `422 Unprocessable Entity` for business-rule failures. Monetary amounts are decimal strings with ISO currency, for example `{ "amount": "4750.00", "currency": "INR" }`.

## 1. Platform and Gateway

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/health` | public / platform | Liveness and dependency status. |
| GET | `/ready` | public / platform | Readiness probe. |
| GET | `/api/v1/openapi` | authenticated | Aggregated OpenAPI index. |
| GET | `/api/v1/openapi/{service}` | authenticated | A service OpenAPI document. |
| GET | `/api/v1/me` | authenticated | JWT subject, roles, permissions, CIF/employee and branch scope. |
| GET | `/api/v1/platform/maintenance` | public | Active maintenance window. |

## 2. Identity and Access Service

**Schema:** `USERS`, `ROLES`, `PERMISSIONS`, `ROLE_PERMISSIONS`, `LOGIN_AUDIT`; JWT access tokens are stateless.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | Register a customer identity and start CIF/KYC onboarding. |
| POST | `/api/v1/auth/login` | public | Authenticate username/password and issue a short-lived JWT access token. |
| POST | `/api/v1/auth/otp/challenges/{challengeId}/verify` | public with challenge | Complete MFA/OTP login. |
| POST | `/api/v1/auth/logout` | authenticated | Record logout; the client discards its stateless JWT. |
| POST | `/api/v1/auth/password/forgot` | public | Create and send password-reset OTP/link. |
| POST | `/api/v1/auth/password/reset` | public with reset proof | Set a new password after reset verification. |
| POST | `/api/v1/auth/password/change` | authenticated | Change own password; validates password policy/history. |
| POST | `/api/v1/auth/otp/challenges` | authenticated | Request OTP for a sensitive operation. |
| GET | `/api/v1/users` | `USER_MANAGE` | Search/filter users by email, role, status, created date. |
| POST | `/api/v1/users` | `USER_MANAGE` | Create staff/service user. |
| GET | `/api/v1/users/{userId}` | owner or `USER_MANAGE` | Retrieve a user profile (never password hash). |
| PATCH | `/api/v1/users/{userId}` | owner-limited or `USER_MANAGE` | Update permitted profile fields. |
| POST | `/api/v1/users/{userId}/lock` | `USER_MANAGE` | Lock user after review. |
| POST | `/api/v1/users/{userId}/unlock` | `USER_MANAGE` | Unlock user and clear lock state. |
| POST | `/api/v1/users/{userId}/disable` | `USER_MANAGE` | Disable user; revoke sessions. |
| POST | `/api/v1/users/{userId}/enable` | `USER_MANAGE` | Re-enable user. |
| POST | `/api/v1/users/{userId}/roles/{roleId}` | `USER_MANAGE` | Assign role. |
| DELETE | `/api/v1/users/{userId}/roles/{roleId}` | `USER_MANAGE` | Remove role where policy allows. |
| GET | `/api/v1/roles` | authenticated | List roles. |
| POST | `/api/v1/roles` | `ROLE_PERMISSION_MANAGE` | Create role. |
| GET | `/api/v1/roles/{roleId}` | authenticated | Get role and effective permissions. |
| PATCH | `/api/v1/roles/{roleId}` | `ROLE_PERMISSION_MANAGE` | Rename/update role description. |
| GET | `/api/v1/roles/{roleId}/permissions` | authenticated | List role-permission mappings. |
| PUT | `/api/v1/roles/{roleId}/permissions/{permissionId}` | `ROLE_PERMISSION_MANAGE` | Add mapping. |
| DELETE | `/api/v1/roles/{roleId}/permissions/{permissionId}` | `ROLE_PERMISSION_MANAGE` | Remove mapping. |
| GET | `/api/v1/permissions` | authenticated | List available permissions. |
| POST | `/api/v1/permissions` | `ROLE_PERMISSION_MANAGE` | Define permission. |
| GET | `/api/v1/permissions/{permissionId}` | authenticated | Get permission. |
| PATCH | `/api/v1/permissions/{permissionId}` | `ROLE_PERMISSION_MANAGE` | Update permission metadata. |
| POST | `/api/v1/auth/introspect` | internal | Validate token/subject for trusted internal callers if needed. |
| GET | `/.well-known/jwks.json` | public | JWT verification keys for gateway/services. |

## 3. Customer and KYC Services

**Customer schema:** `CUSTOMERS`, `CUSTOMER_ADDRESSES`, legacy customer-owned KYC document metadata, and `BENEFICIARIES`. **KYC schema:** `KYC_SESSIONS`, `KYC_DOCUMENTS`, `KYC_FRAMES`, and `KYC_VERIFICATIONS`. Customer Service owns the canonical customer KYC status; KYC Service owns the workflow and binary evidence.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/customers` | `CUSTOMER_READ` | Search by CIF, name, mobile, PAN, KYC status, customer type or branch scope. |
| POST | `/api/v1/customers` | onboarding/internal | Create customer and generate CIF; normally called after registration. |
| GET | `/api/v1/customers/{cif}` | owner or `CUSTOMER_READ` | Customer profile and masked identifiers. |
| PATCH | `/api/v1/customers/{cif}` | owner-limited or `CUSTOMER_UPDATE` | Update permitted profile/contact/risk fields. |
| GET | `/api/v1/customers/{cif}/summary` | owner or `CUSTOMER_READ` | Compact eligibility/KYC summary for Account Service. |
| GET | `/api/v1/customers/{cif}/eligibility` | internal | Product-opening eligibility decision. |
| GET | `/api/v1/customers/{cif}/addresses` | owner or `CUSTOMER_READ` | List addresses. |
| POST | `/api/v1/customers/{cif}/addresses` | owner or `CUSTOMER_UPDATE` | Add permanent/current address. |
| GET | `/api/v1/customers/{cif}/addresses/{addressId}` | owner or `CUSTOMER_READ` | Read address. |
| PATCH | `/api/v1/customers/{cif}/addresses/{addressId}` | owner or `CUSTOMER_UPDATE` | Edit address. |
| DELETE | `/api/v1/customers/{cif}/addresses/{addressId}` | owner or `CUSTOMER_UPDATE` | Remove a non-required address. |
| GET | `/api/v1/customers/{cif}/preferences` | owner | Communication preferences. |
| PUT | `/api/v1/customers/{cif}/preferences` | owner | Replace communication preferences. |
| GET | `/api/v1/customers/{cif}/documents` | owner or `CUSTOMER_READ` | List KYC metadata; never return unmasked document number. |
| POST | `/api/v1/customers/{cif}/documents` | owner/staff | Create secure-upload request and documentation metadata. |
| GET | `/api/v1/customers/{cif}/documents/{documentId}` | owner or `CUSTOMER_READ` | Read document metadata/status. |
| PATCH | `/api/v1/customers/{cif}/documents/{documentId}` | owner before review | Correct pending document metadata. |
| POST | `/api/v1/customers/{cif}/documents/{documentId}/verify` | `KYC_VERIFY` | Mark document verified; record verifier/time/remarks. |
| POST | `/api/v1/customers/{cif}/documents/{documentId}/reject` | `KYC_VERIFY` | Reject document with reason. |
| POST | `/api/v1/customers/{cif}/kyc/verify` | `KYC_VERIFY` | Mark customer KYC verified after all requirements pass. |
| POST | `/api/v1/customers/{cif}/kyc/reject` | `KYC_VERIFY` | Mark customer KYC rejected with reason. |
| POST | `/api/v1/customers/{cif}/kyc/resubmit` | owner/staff | Move rejected KYC back to pending after resubmission. |
| POST | `/api/v1/kyc/sessions` | `KYC_VERIFY` | Validate the CIF through Customer Service and create a KYC session. |
| GET | `/api/v1/kyc/sessions/{sessionId}` | `KYC_VERIFY` | Read the KYC session and current workflow status. |
| GET | `/api/v1/kyc/customers/{cif}/sessions/pending` | `KYC_VERIFY` | List pending KYC sessions for a customer. |
| POST | `/api/v1/kyc/sessions/{sessionId}/documents` | `KYC_VERIFY` | Store one identity document as multipart binary evidence. |
| GET | `/api/v1/kyc/sessions/{sessionId}/documents/{documentType}` | `KYC_VERIFY` | Download the KYC-owned identity document. |
| POST | `/api/v1/kyc/sessions/{sessionId}/frames` | `KYC_VERIFY` | Store one or more face frames as multipart binary evidence. |
| GET | `/api/v1/kyc/sessions/{sessionId}/frames` | `KYC_VERIFY` | List captured frame metadata. |
| GET | `/api/v1/kyc/sessions/{sessionId}/frames/{frameNumber}` | `KYC_VERIFY` | Download one captured frame. |
| GET | `/api/v1/kyc/sessions/{sessionId}/result` | `KYC_VERIFY` | Read the current verification result. |
| POST | `/api/v1/kyc/sessions/{sessionId}/approve` | `KYC_VERIFY` | Approve the session and idempotently synchronize Customer Service. |
| POST | `/api/v1/kyc/sessions/{sessionId}/reject` | `KYC_VERIFY` | Reject the session and idempotently synchronize Customer Service. |
| GET | `/api/v1/customers/{cif}/risk-classification` | owner or `CUSTOMER_READ` | Read risk classification. |
| PUT | `/api/v1/customers/{cif}/risk-classification` | `CUSTOMER_UPDATE` | Set risk class and assessment rationale. |
| GET | `/api/v1/customers/{cif}/beneficiaries` | owner or staff scope | List registered transfer beneficiaries. |
| POST | `/api/v1/customers/{cif}/beneficiaries` | owner | Register beneficiary; starts activation/verification. |
| GET | `/api/v1/customers/{cif}/beneficiaries/{beneficiaryId}` | owner or staff scope | Read beneficiary. |
| PATCH | `/api/v1/customers/{cif}/beneficiaries/{beneficiaryId}` | owner | Update pending/active beneficiary details. |
| POST | `/api/v1/customers/{cif}/beneficiaries/{beneficiaryId}/activate` | policy/internal | Mark beneficiary active after verification/cooling period. |
| POST | `/api/v1/customers/{cif}/beneficiaries/{beneficiaryId}/deactivate` | owner/staff | Disable beneficiary. |
| DELETE | `/api/v1/customers/{cif}/beneficiaries/{beneficiaryId}` | owner | Remove beneficiary when no pending payment references it. |
| GET | `/api/v1/customers/{cif}/nominees` | owner or `CUSTOMER_READ` | List customer-level nominees. |
| POST | `/api/v1/customers/{cif}/nominees` | owner | Add nominee. |
| PATCH | `/api/v1/customers/{cif}/nominees/{nomineeId}` | owner | Update nominee. |
| DELETE | `/api/v1/customers/{cif}/nominees/{nomineeId}` | owner | Remove nominee. |

## 4. Branch and Employee Service

**Schema:** `BRANCHES`, `EMPLOYEES`; **Derived:** working hours, holiday calendar, approval authorities and branch transfer history.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/branches` | authenticated | Search branches by code, city, state, IFSC or status. |
| POST | `/api/v1/branches` | admin | Create branch. |
| GET | `/api/v1/branches/{branchId}` | authenticated | Read branch. |
| PATCH | `/api/v1/branches/{branchId}` | admin | Update branch master data. |
| POST | `/api/v1/branches/{branchId}/activate` | admin | Activate branch. |
| POST | `/api/v1/branches/{branchId}/deactivate` | admin | Deactivate branch after lifecycle checks. |
| GET | `/api/v1/branches/by-ifsc/{ifscCode}` | authenticated | Resolve IFSC. |
| GET | `/api/v1/branches/{branchId}/working-hours` | authenticated | Get branch working hours. |
| PUT | `/api/v1/branches/{branchId}/working-hours` | admin | Set weekly working hours. |
| GET | `/api/v1/branches/{branchId}/holidays` | authenticated | List branch holidays. |
| POST | `/api/v1/branches/{branchId}/holidays` | admin | Add holiday. |
| DELETE | `/api/v1/branches/{branchId}/holidays/{holidayId}` | admin | Remove holiday. |
| GET | `/api/v1/employees` | staff/admin | Search by employee code, user, branch, manager, designation or status. |
| POST | `/api/v1/employees` | admin | Create employee profile linked logically to user and branch. |
| GET | `/api/v1/employees/{employeeId}` | self/manager/admin | Read employee profile. |
| PATCH | `/api/v1/employees/{employeeId}` | admin | Update designation/status/profile. |
| GET | `/api/v1/employees/{employeeId}/reports` | self/manager/admin | Direct reports. |
| PUT | `/api/v1/employees/{employeeId}/manager` | admin | Set reporting manager. |
| POST | `/api/v1/employees/{employeeId}/transfer` | admin | Transfer employee to branch; retain transfer history. |
| GET | `/api/v1/employees/{employeeId}/approval-authority` | self/manager/admin | Get employee's approval limits and authorities. |
| PUT | `/api/v1/employees/{employeeId}/approval-authority` | admin | Configure limits/authorities. |
| GET | `/internal/v1/approval-authority` | internal | Validate approver for branch, action and amount. |

## 5. Product Master Service

**Schema:** `PRODUCTS`, optional `PRODUCT_RULES`; **Derived:** version history, charges, penalties and effective dates.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/products` | `PRODUCT_READ` | Public/internal catalogue; filter active type, code, tenure, eligibility date. |
| POST | `/api/v1/products` | `PRODUCT_MANAGE` | Create product master. |
| GET | `/api/v1/products/{productId}` | `PRODUCT_READ` | Read product and current effective version. |
| PATCH | `/api/v1/products/{productId}` | `PRODUCT_MANAGE` | Update non-versioned metadata. |
| GET | `/api/v1/products/code/{productCode}` | `PRODUCT_READ` | Resolve product by stable code. |
| GET | `/api/v1/products/{productId}/versions` | `PRODUCT_READ` | List effective-dated product versions. |
| POST | `/api/v1/products/{productId}/versions` | `PRODUCT_MANAGE` | Create future/current rate/tenure/minimum-balance version. |
| GET | `/api/v1/products/{productId}/versions/{versionId}` | `PRODUCT_READ` | Read version. |
| POST | `/api/v1/products/{productId}/activate` | `PRODUCT_MANAGE` | Activate a product. |
| POST | `/api/v1/products/{productId}/deactivate` | `PRODUCT_MANAGE` | Deactivate new sales without changing existing accounts. |
| GET | `/api/v1/products/{productId}/rules` | `PRODUCT_READ` | List product rules. |
| POST | `/api/v1/products/{productId}/rules` | `PRODUCT_MANAGE` | Add rule such as day-count, payout frequency or eligibility. |
| GET | `/api/v1/products/{productId}/rules/{ruleId}` | `PRODUCT_READ` | Read a rule. |
| PATCH | `/api/v1/products/{productId}/rules/{ruleId}` | `PRODUCT_MANAGE` | Update a rule. |
| DELETE | `/api/v1/products/{productId}/rules/{ruleId}` | `PRODUCT_MANAGE` | Retire unused rule. |
| GET | `/api/v1/products/{productId}/charges` | `PRODUCT_READ` | Charges and opening fee schedule. |
| PUT | `/api/v1/products/{productId}/charges` | `PRODUCT_MANAGE` | Replace effective charge schedule. |
| GET | `/api/v1/products/{productId}/penalties` | `PRODUCT_READ` | Penalty schedule. |
| PUT | `/api/v1/products/{productId}/penalties` | `PRODUCT_MANAGE` | Replace effective penalty schedule. |
| GET | `/internal/v1/products/{productId}/effective` | internal | Resolve product terms for opening/accrual on business date. |

## 6. Account Service

**Schema:** `ACCOUNTS`, `ACCOUNT_HOLDERS`, optional `SECONDARY_LINK_PRODUCTS`; **Derived:** applications/approvals, holds, liens, limits, operating instructions, balance history, interest accruals and closure settlements.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/accounts` | `ACCOUNT_VIEW` | List owned/permitted accounts; filter CIF, branch, product, status, account number. |
| POST | `/api/v1/accounts/applications` | `ACCOUNT_OPEN` | Create idempotent account-opening application. |
| GET | `/api/v1/accounts/applications` | staff scope | Search applications by status/CIF/branch/product. |
| GET | `/api/v1/accounts/applications/{applicationId}` | owner or staff scope | Read application, validation and approval state. |
| PATCH | `/api/v1/accounts/applications/{applicationId}` | maker before decision | Amend a pending application. |
| POST | `/api/v1/accounts/applications/{applicationId}/submit` | maker | Submit for approval. |
| POST | `/api/v1/accounts/applications/{applicationId}/approve` | `ACCOUNT_APPROVE` | Approve application; maker-checker and branch checks apply. |
| POST | `/api/v1/accounts/applications/{applicationId}/reject` | `ACCOUNT_APPROVE` | Reject application with reason. |
| POST | `/api/v1/accounts/applications/{applicationId}/cancel` | maker | Cancel an unapproved application. |
| GET | `/api/v1/accounts/{accountId}` | owner or `ACCOUNT_VIEW` | Account master and lifecycle status. |
| PATCH | `/api/v1/accounts/{accountId}` | authorised staff | Amend allowed servicing/profile fields. |
| GET | `/api/v1/accounts/by-number/{accountNumber}` | owner or staff scope | Resolve account number. |
| GET | `/api/v1/accounts/{accountId}/balance` | owner or `ACCOUNT_VIEW` | Ledger, available and held balance. |
| GET | `/api/v1/accounts/{accountId}/balance-history` | owner or `ACCOUNT_VIEW` | Daily/dated balance history. |
| GET | `/api/v1/accounts/{accountId}/status-history` | owner or staff scope | Lifecycle transitions/audit view. |
| POST | `/api/v1/accounts/{accountId}/freeze` | `ACCOUNT_STATUS_MANAGE` | Freeze debit/credit per policy. |
| POST | `/api/v1/accounts/{accountId}/unfreeze` | `ACCOUNT_STATUS_MANAGE` | Restore from frozen state. |
| POST | `/api/v1/accounts/{accountId}/block` | `ACCOUNT_STATUS_MANAGE` | Block account with reason. |
| POST | `/api/v1/accounts/{accountId}/unblock` | `ACCOUNT_STATUS_MANAGE` | Remove block subject to checks. |
| POST | `/api/v1/accounts/{accountId}/mark-dormant` | internal/ops | Mark inactive account dormant. |
| POST | `/api/v1/accounts/{accountId}/reactivate` | `ACCOUNT_STATUS_MANAGE` | Reactivate eligible dormant account. |
| POST | `/api/v1/accounts/{accountId}/closure-requests` | owner or staff scope | Initiate closure and calculate settlement. |
| GET | `/api/v1/accounts/{accountId}/closure-requests/{closureId}` | owner or staff scope | Read closure status/settlement. |
| POST | `/api/v1/accounts/{accountId}/closure-requests/{closureId}/approve` | `ACCOUNT_STATUS_MANAGE` | Approve closure where required. |
| POST | `/api/v1/accounts/{accountId}/close` | internal/approved staff | Close only after settlement and zero/eligible balance. |
| GET | `/api/v1/accounts/{accountId}/holders` | owner or `ACCOUNT_VIEW` | List primary/joint holders. |
| POST | `/api/v1/accounts/{accountId}/holders` | authorised staff | Add joint holder. |
| PATCH | `/api/v1/accounts/{accountId}/holders/{holderId}` | authorised staff | Update holder role/status. |
| DELETE | `/api/v1/accounts/{accountId}/holders/{holderId}` | authorised staff | Remove non-primary holder subject to policy. |
| GET | `/api/v1/accounts/{accountId}/operating-instructions` | owner or staff scope | Get single/joint operation instructions. |
| PUT | `/api/v1/accounts/{accountId}/operating-instructions` | authorised staff | Set operating instructions. |
| GET | `/api/v1/accounts/{accountId}/holds` | owner or `ACCOUNT_VIEW` | List active/released holds. |
| POST | `/api/v1/accounts/{accountId}/holds` | authorised staff/internal | Place hold with amount, reason, expiry and reference. |
| POST | `/api/v1/accounts/{accountId}/holds/{holdId}/release` | authorised staff/internal | Release a hold. |
| GET | `/api/v1/accounts/{accountId}/liens` | owner or `ACCOUNT_VIEW` | List liens. |
| POST | `/api/v1/accounts/{accountId}/liens` | authorised staff | Create lien. |
| PATCH | `/api/v1/accounts/{accountId}/liens/{lienId}` | authorised staff | Amend lien amount/expiry. |
| POST | `/api/v1/accounts/{accountId}/liens/{lienId}/release` | authorised staff | Release lien. |
| GET | `/api/v1/accounts/{accountId}/limits` | owner or `ACCOUNT_VIEW` | Account transaction limits. |
| PUT | `/api/v1/accounts/{accountId}/limits` | authorised staff | Set account-level limits. |
| GET | `/api/v1/accounts/{accountId}/linked-products` | owner or `ACCOUNT_VIEW` | List secondary card/other linked products. |
| POST | `/api/v1/accounts/{accountId}/linked-products` | authorised staff | Attach secondary product. |
| DELETE | `/api/v1/accounts/{accountId}/linked-products/{productId}` | authorised staff | Detach secondary product. |
| GET | `/api/v1/accounts/{accountId}/interest/accruals` | owner or staff scope | List daily interest accruals. |
| GET | `/api/v1/accounts/{accountId}/interest/postings` | owner or staff scope | List interest credit postings. |
| POST | `/internal/v1/accounts/{accountId}/authorise-debit` | transaction service | Check account state, balance, hold, limit and return debit authorisation. |
| POST | `/internal/v1/accounts/{accountId}/debits` | transaction service | Apply idempotent debit command. |
| POST | `/internal/v1/accounts/{accountId}/credits` | transaction service | Apply idempotent credit command. |
| POST | `/internal/v1/accounts/{accountId}/compensations` | transaction service | Apply compensating debit/credit after a failed saga. |
| POST | `/internal/v1/accounts/interest/accruals` | EOD job | Persist idempotent daily interest accrual. |
| POST | `/internal/v1/accounts/interest/postings` | EOD job | Post idempotent interest credit. |

## 7. Transaction and Payment Service

**Schema:** `TRANSACTIONS`; **Derived:** idempotency records, payment instructions, approval queue, reversal links, cheque items and reconciliation exceptions. The service owns transaction state and tells Account Service to change balances; it does not write Account tables directly.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/transactions` | owner/staff scope | Search history by account, reference, status, rail, type, amount/date range, created-by. |
| POST | `/api/v1/transactions/deposits` | `TRANSACTION_CREATE` | Cash/approved channel deposit. |
| POST | `/api/v1/transactions/withdrawals` | `TRANSACTION_CREATE` | Cash/approved channel withdrawal. |
| POST | `/api/v1/transactions/transfers/internal` | `TRANSACTION_CREATE` | Same-bank account-to-account transfer using source and counter account IDs. |
| POST | `/api/v1/transactions/transfers/neft` | `TRANSACTION_CREATE` | Initiate NEFT transfer to active beneficiary. |
| POST | `/api/v1/transactions/transfers/rtgs` | `TRANSACTION_CREATE` | Initiate RTGS transfer, including configured minimum. |
| POST | `/api/v1/transactions/transfers/imps` | `TRANSACTION_CREATE` | Initiate IMPS transfer. |
| POST | `/api/v1/transactions/transfers/upi` | `TRANSACTION_CREATE` | Initiate UPI transfer. |
| POST | `/api/v1/transactions/cheques` | `TRANSACTION_CREATE` | Submit cheque for clearing. |
| POST | `/api/v1/transactions/card-payments` | `TRANSACTION_CREATE` | Record card payment for linked card product. |
| GET | `/api/v1/transactions/{transactionId}` | owner/staff scope | Retrieve transaction, state and settlement details. |
| GET | `/api/v1/transactions/by-reference/{txnRef}` | owner/staff scope | Lookup idempotent transaction by business reference. |
| GET | `/api/v1/transactions/{transactionId}/status` | owner/staff scope | Lightweight status/polling resource. |
| POST | `/api/v1/transactions/{transactionId}/cancel` | maker while pending | Cancel an unexecuted permitted transaction. |
| POST | `/api/v1/transactions/{transactionId}/approve` | `TRANSACTION_APPROVE` | Approve a pending maker-checker transaction; maker cannot approve. |
| POST | `/api/v1/transactions/{transactionId}/reject` | `TRANSACTION_APPROVE` | Reject transaction before execution. |
| GET | `/api/v1/transactions/approvals` | `TRANSACTION_APPROVE` | List pending approval queue, filtered by branch/amount/rail. |
| POST | `/api/v1/transactions/{transactionId}/reversals` | `TRANSACTION_REVERSE` | Create a compensating reversal request for a successful reversible transaction. |
| GET | `/api/v1/transactions/{transactionId}/reversals` | owner/staff scope | List reversals associated with transaction. |
| GET | `/api/v1/accounts/{accountId}/transactions` | owner/staff scope | Account-specific transaction history. |
| GET | `/api/v1/accounts/{accountId}/mini-statement` | owner/staff scope | Latest permitted transaction list. |
| GET | `/api/v1/transactions/limits/quote` | authenticated | Calculate applicable customer/account/channel limit before submit. |
| POST | `/internal/v1/transactions/{transactionId}/settle` | rail adapter | Apply inbound settlement/callback idempotently. |
| POST | `/internal/v1/transactions/{transactionId}/fail` | rail adapter | Record definitive failure and trigger compensation state. |
| POST | `/internal/v1/transactions/{transactionId}/cheque-clearing` | clearing adapter | Update cheque clearing state. |
| GET | `/api/v1/reconciliation/exceptions` | operations | List ledger/account/transaction discrepancies. |
| GET | `/api/v1/reconciliation/exceptions/{exceptionId}` | operations | Read an exception. |
| POST | `/api/v1/reconciliation/exceptions/{exceptionId}/assign` | operations | Assign exception investigator. |
| POST | `/api/v1/reconciliation/exceptions/{exceptionId}/resolve` | operations | Resolve with evidence/notes. |
| POST | `/internal/v1/reconciliation/runs` | EOD/operations | Start or record daily reconciliation run. |

## 8. Ledger Service

**Schema:** `LEDGER_ENTRIES`. Ledger is append-only. Posting endpoints are internal only and must enforce a balanced posting set per financial transaction.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/ledger/entries` | finance/auditor scope | Search ledger by account, transaction, posting date, entry type and amount. |
| GET | `/api/v1/ledger/entries/{entryId}` | finance/auditor scope | Read immutable ledger entry. |
| GET | `/api/v1/ledger/transactions/{transactionId}/entries` | owner/staff scope | Ledger postings for a transaction. |
| GET | `/api/v1/ledger/accounts/{accountId}/entries` | owner/staff scope | Ledger view for account. |
| GET | `/api/v1/ledger/accounts/{accountId}/balance` | owner/staff scope | Ledger-derived balance as of date/time. |
| GET | `/api/v1/ledger/trial-balance` | finance/auditor | Trial balance by date/branch/product. |
| POST | `/internal/v1/ledger/posting-sets` | transaction/EOD service | Atomically append balanced debit/credit entries. |
| POST | `/internal/v1/ledger/posting-sets/{postingSetId}/validate` | internal/ops | Revalidate posting-set balance and invariants. |

## 9. Statement and Reporting Service

**Schema:** Derived read model, `STATEMENT_REQUESTS`, generated-file metadata, download history and report schedules. Source of truth remains Account/Transaction/Ledger services.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/statements/accounts/{accountId}/mini` | `STATEMENT_VIEW` | Current mini statement. |
| POST | `/api/v1/statements/accounts/{accountId}` | `STATEMENT_VIEW` | Request date-range/monthly/yearly statement in PDF, CSV or XLSX. |
| GET | `/api/v1/statements/requests` | owner/staff scope | List statement requests and statuses. |
| GET | `/api/v1/statements/requests/{requestId}` | owner/staff scope | Get request/status/checksum/expiry. |
| POST | `/api/v1/statements/requests/{requestId}/cancel` | requester while queued | Cancel ungenerated statement request. |
| POST | `/api/v1/statements/requests/{requestId}/download-link` | owner/staff scope | Issue short-lived authorised download URL. |
| GET | `/api/v1/statements/download-history` | owner/staff scope | Audit previous downloads. |
| GET | `/api/v1/statements/accounts/{accountId}/interest-certificate` | owner/staff scope | Request/download interest certificate for fiscal year. |
| GET | `/api/v1/statements/accounts/{accountId}/tds-certificate` | owner/staff scope | Request/download TDS certificate for fiscal year. |
| GET | `/api/v1/reports/daily-transactions` | operations/auditor | Daily transaction report. |
| GET | `/api/v1/reports/branches/{branchId}/transactions` | branch scope/auditor | Branch transaction report. |
| GET | `/api/v1/reports/dormant-accounts` | operations/auditor | Dormant account report. |
| GET | `/api/v1/reports/interest-accruals` | finance/auditor | Interest accrual/posting report. |
| GET | `/api/v1/reports/reconciliation` | operations/auditor | Reconciliation status and exception summary. |
| GET | `/api/v1/report-schedules` | owner/admin | List scheduled statement/report jobs. |
| POST | `/api/v1/report-schedules` | `STATEMENT_VIEW` / admin | Create a permitted recurring statement/report schedule. |
| GET | `/api/v1/report-schedules/{scheduleId}` | owner/admin | Read schedule. |
| PATCH | `/api/v1/report-schedules/{scheduleId}` | owner/admin | Update schedule. |
| DELETE | `/api/v1/report-schedules/{scheduleId}` | owner/admin | Cancel schedule. |

## 10. Notification Service

**Schema:** Derived notification templates, deliveries, retries, deduplication keys and provider responses. It consumes domain events and never participates in a financial commit.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| POST | `/api/v1/notifications` | `NOTIFICATION_MANAGE` | Queue an email, SMS or push notification; delivery is asynchronous and idempotent when `Idempotency-Key` is supplied. |
| GET | `/api/v1/notifications` | owner/operations | List notifications by recipient, type, status and date. |
| GET | `/api/v1/notifications/{notificationId}` | owner/operations | Read delivery status. |
| POST | `/api/v1/notifications/{notificationId}/resend` | owner/operations policy | Request resend of eligible notification. |
| GET | `/api/v1/notification-templates` | notification admin | List templates. |
| POST | `/api/v1/notification-templates` | notification admin | Create template. |
| GET | `/api/v1/notification-templates/{templateId}` | notification admin | Read template. |
| PATCH | `/api/v1/notification-templates/{templateId}` | notification admin | Update template/version. |
| POST | `/api/v1/notification-templates/{templateId}/activate` | notification admin | Activate template version. |
| POST | `/api/v1/notification-templates/{templateId}/deactivate` | notification admin | Deactivate template. |
| GET | `/api/v1/notifications/failures` | operations | List failed/delayed deliveries. |
| POST | `/api/v1/notifications/failures/{notificationId}/retry` | operations | Retry failed delivery. |

## 11. Audit Service

**Schema:** Derived append-only audit event store with actor, service, entity, old/new values, correlation ID, IP, event time, export and retention metadata.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/audit/events` | `AUDIT_READ` | Search security/business events by actor, service, entity, action, date, correlation ID, branch or outcome. |
| GET | `/api/v1/audit/events/{eventId}` | `AUDIT_READ` | Read immutable event and masked before/after values. |
| GET | `/api/v1/audit/correlation/{correlationId}` | `AUDIT_READ` | Trace a cross-service request. |
| GET | `/api/v1/audit/users/{userId}` | `AUDIT_READ` | User security and business activity. |
| GET | `/api/v1/audit/accounts/{accountId}` | `AUDIT_READ` | Account lifecycle/financial activity. |
| GET | `/api/v1/audit/transactions/{transactionId}` | `AUDIT_READ` | Transaction activity including approval/reversal. |
| POST | `/api/v1/audit/exports` | `AUDIT_READ` | Request export for date/filter range. |
| GET | `/api/v1/audit/exports/{exportId}` | `AUDIT_READ` | Export status and authorised download link. |
| POST | `/api/v1/audit/retention-runs` | records admin | Start/record approved archival process. |
| GET | `/api/v1/audit/retention-runs` | records admin | Retention and archival history. |
| POST | `/internal/v1/audit/events` | trusted services/event consumer | Append audit event; normal producer path is event bus. |

## 12. Configuration Service

**Schema:** Derived typed configuration, channel/rail limits, password/session/OTP policies, holidays, maintenance windows, maker-checker thresholds, feature flags and change history.

| Method | Endpoint | Permission / access | Purpose |
|---|---|---|---|
| GET | `/api/v1/configuration` | `CONFIG_MANAGE` | Search typed configuration by namespace/key/effective date. |
| GET | `/api/v1/configuration/{namespace}/{key}` | `CONFIG_MANAGE` | Read effective configuration value/history. |
| PUT | `/api/v1/configuration/{namespace}/{key}` | `CONFIG_MANAGE` | Create/replace versioned configuration value. |
| DELETE | `/api/v1/configuration/{namespace}/{key}` | `CONFIG_MANAGE` | Retire unused configuration key (not historical data). |
| GET | `/api/v1/configuration/limits` | `CONFIG_MANAGE` | Channel/rail limit rules. |
| POST | `/api/v1/configuration/limits` | `CONFIG_MANAGE` | Create limit rule. |
| PATCH | `/api/v1/configuration/limits/{limitId}` | `CONFIG_MANAGE` | Amend limit rule. |
| DELETE | `/api/v1/configuration/limits/{limitId}` | `CONFIG_MANAGE` | Retire limit rule. |
| GET | `/api/v1/configuration/policies/password` | `CONFIG_MANAGE` | Password policy. |
| PUT | `/api/v1/configuration/policies/password` | `CONFIG_MANAGE` | Set password policy. |
| GET | `/api/v1/configuration/policies/session` | `CONFIG_MANAGE` | Session policy. |
| PUT | `/api/v1/configuration/policies/session` | `CONFIG_MANAGE` | Set session policy. |
| GET | `/api/v1/configuration/policies/otp` | `CONFIG_MANAGE` | OTP expiry/retry policy. |
| PUT | `/api/v1/configuration/policies/otp` | `CONFIG_MANAGE` | Set OTP policy. |
| GET | `/api/v1/configuration/maker-checker-thresholds` | `CONFIG_MANAGE` | Thresholds that route work to approval. |
| PUT | `/api/v1/configuration/maker-checker-thresholds` | `CONFIG_MANAGE` | Replace effective threshold set. |
| GET | `/api/v1/configuration/maintenance-windows` | `CONFIG_MANAGE` | List planned maintenance. |
| POST | `/api/v1/configuration/maintenance-windows` | `CONFIG_MANAGE` | Create maintenance window. |
| PATCH | `/api/v1/configuration/maintenance-windows/{windowId}` | `CONFIG_MANAGE` | Update maintenance window. |
| DELETE | `/api/v1/configuration/maintenance-windows/{windowId}` | `CONFIG_MANAGE` | Cancel future maintenance window. |
| GET | `/api/v1/configuration/feature-flags` | `CONFIG_MANAGE` | List feature flags. |
| PUT | `/api/v1/configuration/feature-flags/{flagKey}` | `CONFIG_MANAGE` | Create/update feature flag and targeting. |
| POST | `/api/v1/configuration/feature-flags/{flagKey}/enable` | `CONFIG_MANAGE` | Enable flag. |
| POST | `/api/v1/configuration/feature-flags/{flagKey}/disable` | `CONFIG_MANAGE` | Disable flag. |
| GET | `/internal/v1/configuration/effective` | internal | Resolve cached effective limits, policies and flags for caller context. |

## Important implementation guards

1. Do not expose cross-service database joins. Resolve references through internal contracts, such as Account -> Customer eligibility and Transaction -> Account authorise/debit/credit.
2. `POST /transactions/*` must save the idempotency key before executing and return the original result on a retry. The same request reference must be carried into account commands and ledger posting.
3. Account balance changes must have a corresponding, balanced ledger posting set. A failed ledger post must prevent `SUCCESS` and must be reconciled using the request reference.
4. KYC documents are secure-storage references plus hashes; do not expose raw Aadhaar/document numbers or unrestricted file paths.
5. Approval, reversal, closure and configuration mutation routes require complete audit events. Financial and audit records are append-only.
6. Separate external rail callbacks and all `/internal/v1` routes from gateway/public routes using mTLS or workload identity in addition to JWT/service credentials.

## Suggested delivery order

1. Identity, Customer/KYC, Product and Branch/Employee master APIs.
2. Account application, approval, account read and holder APIs.
3. Internal account commands, transaction idempotency, and ledger posting sets as one financial vertical slice.
4. Transfers, approvals, reversals, reconciliation, statements and reports.
5. Notifications, audit, configuration and operational jobs.
