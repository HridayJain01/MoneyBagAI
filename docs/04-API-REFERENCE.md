# 04 — API Reference

Every endpoint below is implemented and reachable. Base URL for all of them is the
gateway: **`http://localhost:8090`**.

Endpoints under `/internal/v1/**` are listed for completeness but are **blocked at the
gateway** (`403 INTERNAL_ROUTE_BLOCKED`). They exist for service-to-service calls.

> **Not in this document:** the aspirational surface in
> `references/moneybags_api_endpoint_catalog.md`. That file is a design catalogue and
> contains many endpoints that were never built. This document is derived from the
> `@RestController` classes. Where they disagree, the code wins.

## Conventions

**Two different page envelopes are in use.** Check which one an endpoint returns.

*Custom envelope* — identity, account, audit, notification, statement services:

```json
{ "items": [], "page": 0, "size": 25, "totalItems": 0, "totalPages": 0 }
```

*Spring `Page`* — transaction service and reconciliation:

```json
{ "content": [], "pageable": {...}, "totalElements": 0, "totalPages": 0,
  "number": 0, "size": 20, "first": true, "last": true }
```

Spring `Page` endpoints accept `?page=`, `?size=` and `?sort=field,asc|desc`. Custom
envelope endpoints accept `?page=` and `?size=` only.

Money is `DECIMAL` in the database and serialises as a JSON number, except in
statement-reporting-service, which wraps it: `{ "amount": "4750.00", "currency": "INR" }`.

Timestamps are ISO-8601 UTC (`2026-08-19T10:15:00Z`). Dates are `YYYY-MM-DD`.

---

## Identity — `security-service`

### Authentication

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | **public** | Self-register a `CUSTOMER` user → `201` |
| POST | `/api/v1/auth/login` | **public** | Authenticate, issue JWT + cookie → `200` |
| POST | `/api/v1/auth/logout` | JWT | Record logout, clear cookie → `204` |

`RegisterRequest`: `firstName`\* `lastName`\* `email`\* `password`\* (min 8) `dob` (past)
`gender` `mobile`
`LoginRequest`: `username`\* (alias `email`) `password`\*
`LoginResponse`: see [02](02-AUTHENTICATION.md#logging-in)

### Users

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/api/v1/users/me` | JWT | Current user profile |
| GET | `/api/v1/users` | `USER_MANAGE` | Search — `?status=&branchCode=&search=&page=&size=` |
| POST | `/api/v1/users` | `USER_MANAGE` | Create staff user → `201` |
| GET | `/api/v1/users/{userId}` | JWT | Existence probe — returns `{userId, username, status}` |
| GET | `/api/v1/users/{userId}/detail` | `USER_MANAGE` | Full profile |
| POST | `/api/v1/users/{userId}/lock` | `USER_MANAGE` | `?minutes=60` |
| POST | `/api/v1/users/{userId}/unlock` | `USER_MANAGE` | |
| POST | `/api/v1/users/{userId}/disable` | `USER_MANAGE` | |
| POST | `/api/v1/users/{userId}/enable` | `USER_MANAGE` | |
| POST | `/api/v1/users/{userId}/roles/{roleId}` | `USER_MANAGE` | Assign role |
| DELETE | `/api/v1/users/{userId}/roles/{roleId}` | `USER_MANAGE` | Remove role |
| POST | `/api/v1/admin/users` | `USER_MANAGE` | Create user, auth-service-compatible shape → `201` |
| PUT | `/api/v1/admin/users/{userId}/role` | `USER_MANAGE` | Replace all roles with one |

`CreateUserRequest`: `username`\* `email`\* `password`\* (min 8) `fullName`\* `mobile`
`employeeId` `branchCode` `roles[]`
`UserDetail`: `userId` `username` `email` `fullName` `mobile` `status` `employeeId`
`branchCode` `lastLoginAt` `createdAt` `roles[]` `permissions[]`

### Roles and permissions

Every one of these requires `ROLE_PERMISSION_MANAGE`. Paths under `/api/v1/roles` and
`/api/v1/admin/roles` are aliases for the same handler.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/roles` · `/api/v1/admin/roles` | List roles |
| GET | `/api/v1/roles/{roleId}` · `/api/v1/admin/roles/{roleId}` | Role + effective permissions |
| POST | `/api/v1/roles` · `/api/v1/admin/roles` | Create → `201` |
| PUT | `/api/v1/roles/{roleId}` · `/api/v1/admin/roles/{roleId}` | Update |
| DELETE | `/api/v1/roles/{roleId}` · `/api/v1/admin/roles/{roleId}` | Delete → `204` |
| GET | `/api/v1/permissions` · `/api/v1/admin/permissions` | List permissions |
| GET | `/api/v1/permissions/{permissionId}` · `/api/v1/admin/permissions/{id}` | Read |
| POST | `/api/v1/permissions` · `/api/v1/admin/permissions` | Create → `201` |
| PUT | `/api/v1/permissions/{permissionId}` · `/api/v1/admin/permissions/{id}` | Update |
| DELETE | `/api/v1/permissions/{permissionId}` · `/api/v1/admin/permissions/{id}` | Delete → `204` |
| POST | `/api/v1/admin/role-permissions` | Link — body `{roleId, permissionId}` |
| GET | `/api/v1/admin/role-permissions/{roleId}` | Role's permissions |
| PUT | `/api/v1/admin/role-permissions/{roleId}` | Replace — body `{permissionIds: []}` |
| DELETE | `/api/v1/admin/role-permissions/{roleId}/{permissionId}` | Unlink |

**Internal:** `PUT /internal/v1/users/{userId}/employment` — branch-employee-service pushes
`{employeeId, branchCode}` so the gateway can resolve employment in one hop.

---

## Customer

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/customers` | Create customer, generate CIF → `201` |
| GET | `/api/v1/customers` | List all customers |
| GET | `/api/v1/customers/search?query=` | Search |
| GET | `/api/v1/customers/{cifNo}` | Full customer entity |
| PATCH | `/api/v1/customers/{cif}` | Update profile fields |
| GET | `/api/v1/customers/{cif}/summary` | Compact summary (map) |
| GET | `/api/v1/customers/{cif}/completeness` | Profile completeness percentages |
| GET | `/api/v1/customers/{cif}/eligibility` | Product-opening eligibility decision |
| PATCH | `/api/v1/customers/{cif}/status` | Change customer status |
| GET | `/api/v1/customers/events` | Domain event log |

> `GET /api/v1/customers` returns **every** customer with no pagination. Filter client-side
> or use `/search` for anything beyond a demo dataset.

### Addresses

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/customers/{cif}/addresses` | List |
| PUT | `/api/v1/customers/{cif}/addresses` | Upsert an address |

### Relationship manager

| Method | Path | Purpose |
|---|---|---|
| PUT | `/api/v1/customers/{cif}/relationship-manager/{empId}` | Assign |
| DELETE | `/api/v1/customers/{cif}/relationship-manager` | Unassign |
| GET | `/api/v1/customers/relationship-manager/{empId}` | Customers for an employee |

### Preferences and risk

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/customers/{cif}/communication-preferences` | Read |
| PUT | `/api/v1/customers/{cif}/communication-preferences` | Replace |
| PUT | `/api/v1/customers/{cif}/risk-classification` | Set `LOW`/`MEDIUM`/`HIGH` |

### Customer-owned KYC documents *(legacy path)*

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/customers/{cif}/kyc-documents` | Submit document metadata |
| GET | `/api/v1/customers/{cif}/kyc-documents` | List for a customer |
| PUT | `/api/v1/customers/{cif}/kyc-documents/{documentId}/assignment/{empId}` | Assign reviewer |
| PATCH | `/api/v1/customers/{cif}/kyc-documents/{documentId}/decision` | Verify or reject |
| GET | `/api/v1/customers/{cif}/kyc-rejections` | Rejection history |
| GET | `/api/v1/customers/kyc/pending` | All pending documents |
| GET | `/api/v1/customers/kyc/re-kyc-required` | Customers needing re-KYC |
| POST | `/api/v1/customers/kyc/expiry-alerts/process` | Trigger the expiry-alert job |

New binary KYC evidence belongs to **kyc-service** (below). These endpoints remain for the
migration period.

**Internal:** `GET /internal/v1/customers/{cif}/kyc-context` ·
`PUT /internal/v1/customers/{cif}/kyc-decision`

---

## KYC

All require `KYC_VERIFY`. Session ids are UUIDs.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/kyc/sessions` | Validate CIF via customer-service, create session |
| GET | `/api/v1/kyc/sessions/{kycSessionId}` | Session + workflow status |
| GET | `/api/v1/kyc/customers/{cif}/sessions/pending` | Pending sessions for a customer |
| POST | `/api/v1/kyc/sessions/{id}/documents` | Upload one document — **`multipart/form-data`** |
| GET | `/api/v1/kyc/sessions/{id}/documents/{documentType}` | Download document bytes |
| POST | `/api/v1/kyc/sessions/{id}/frames` | Upload face frames — **`multipart/form-data`** |
| GET | `/api/v1/kyc/sessions/{id}/frames` | Frame metadata |
| GET | `/api/v1/kyc/sessions/{id}/frames/{frameNumber}` | Download one frame |
| GET | `/api/v1/kyc/sessions/{id}/result` | Current verification result |
| POST | `/api/v1/kyc/sessions/{id}/approve` | Approve + sync customer-service |
| POST | `/api/v1/kyc/sessions/{id}/reject` | Reject + sync customer-service |

`documentType`: `AADHAAR` `PAN` `PASSPORT` `DRIVING_LICENSE` `VOTER_ID`

Approve and reject update the KYC workflow **and** synchronise the canonical customer
status in one request, idempotently.

---

## Branch & Employee

### Branches

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/branches` | List all |
| GET | `/api/v1/branches/{id}` | Read |
| POST | `/api/v1/branches` | Create → `201` |
| PATCH | `/api/v1/branches/{id}` | Update |
| POST | `/api/v1/branches/{id}/activate` | |
| POST | `/api/v1/branches/{id}/deactivate` | |
| GET | `/api/v1/branches/by-ifsc/{ifsc}` | Resolve IFSC |
| GET | `/api/v1/branches/{id}/working-hours` | List |
| PUT | `/api/v1/branches/{id}/working-hours` | Replace the weekly set |
| GET | `/api/v1/branches/{id}/holidays` | List |
| POST | `/api/v1/branches/{id}/holidays` | Add → `201` |
| DELETE | `/api/v1/branches/{id}/holidays/{holidayId}` | Remove |

### Employees

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/employees` | List all |
| GET | `/api/v1/employees/{id}` | Read |
| POST | `/api/v1/employees` | Create → `201`; also pushes employment to identity-service |
| PATCH | `/api/v1/employees/{id}` | Update |
| GET | `/api/v1/employees/{id}/reports` | Direct reports |
| PUT | `/api/v1/employees/{id}/manager` | Set reporting manager |
| POST | `/api/v1/employees/{id}/transfer` | Transfer branch, retains history |
| GET | `/api/v1/employees/{id}/approval-authority` | Limits per action type |
| PUT | `/api/v1/employees/{id}/approval-authority` | Replace limits |

**Internal:** `GET /internal/v1/approval-authority` — validate an approver for branch,
action and amount.

---

## Product

Read operations need `PRODUCT_READ`, writes need `PRODUCT_MANAGE`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/products` | Catalogue — `?status=&type=` |
| GET | `/api/v1/products/{productCode}` | Read by code (the code **is** the id) |
| POST | `/api/v1/products` | Create → `201` |
| PATCH | `/api/v1/products/{productCode}` | Update metadata |
| POST | `/api/v1/products/{productCode}/activate` | |
| POST | `/api/v1/products/{productCode}/deactivate` | Stops new sales; existing accounts unaffected |
| GET | `/api/v1/products/{productCode}/versions` | Version history |
| GET | `/api/v1/products/{productCode}/versions/{versionNumber}` | One version |
| GET | `/api/v1/products/{productCode}/as-of?date=YYYY-MM-DD` | Terms effective on a date |
| GET | `/api/v1/products/{productCode}/charges` | List |
| PUT | `/api/v1/products/{productCode}/charges` | Replace the schedule |
| GET | `/api/v1/products/{productCode}/rules` | List |
| POST | `/api/v1/products/{productCode}/rules` | Add → `201` |
| DELETE | `/api/v1/products/{productCode}/rules/{ruleId}` | Retire |

**Internal:** `GET /internal/v1/products/{productCode}/effective` — resolve terms for
opening or accrual on a business date.

---

## Account

### Applications

| Method | Path | Permission | Purpose |
|---|---|---|---|
| POST | `/api/v1/accounts/applications` | `ACCOUNT_OPEN` | Create → `201` |
| GET | `/api/v1/accounts/applications` | scope | `?cifNo=&status=&page=&size=` |
| GET | `/api/v1/accounts/applications/{applicationId}` | scope | Read |
| POST | `/api/v1/accounts/applications/{id}/approve` | `ACCOUNT_APPROVE` | **Maker may not approve own work** |
| POST | `/api/v1/accounts/applications/{id}/reject` | `ACCOUNT_APPROVE` | Body `{reason}` required |
| POST | `/api/v1/accounts/applications/{id}/cancel` | maker | |

`CreateApplicationRequest`: `cifNo`\* `productCode`\* `accountName` `initialDeposit` `currency`
`ApplicationDetail`: `applicationId` `applicationReference` `cifNo` `productCode`
`branchCode` `currency` `accountName` `requestedInitialDeposit` `status`
`makerEmployeeId` `checkerEmployeeId` `rejectionReason` `createdAccountId` `createdAt`
`updatedAt`

Approve body is optional: `{"remarks": "..."}`. Reject body is required: `{"reason": "..."}`.

### Accounts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/accounts` | `?cifNo=&productCode=&status=&page=&size=` |
| GET | `/api/v1/accounts/{accountId}` | Full detail |
| GET | `/api/v1/accounts/by-number/{accountNumber}` | Resolve account number |
| GET | `/api/v1/accounts/{accountId}/balance` | Ledger, held **and available** balance |
| GET | `/api/v1/accounts/{accountId}/balance-history` | Paged |
| GET | `/api/v1/accounts/{accountId}/status-history` | Lifecycle transitions |
| GET | `/api/v1/accounts/{accountId}/products` | Catalogue products this account owns |

`AccountDetail` includes the derived `availableBalance` alongside `ledgerBalance` and
`heldAmount`, plus the snapshotted `minBalance` `overdraftLimit` `interestRate`
`tenureMonths` `maturityDate`.

`BalanceView`: `accountId` `currency` `ledgerBalance` `heldAmount` `availableBalance`
`minBalance` `overdraftLimit` `asOf`

### Lifecycle

All take an optional body `{"reason": "..."}` and require `ACCOUNT_STATUS_MANAGE`.

| Method | Path | Resulting status |
|---|---|---|
| POST | `/api/v1/accounts/{id}/freeze` | `FROZEN` |
| POST | `/api/v1/accounts/{id}/unfreeze` | `ACTIVE` |
| POST | `/api/v1/accounts/{id}/block` | `BLOCKED` |
| POST | `/api/v1/accounts/{id}/unblock` | `ACTIVE` |
| POST | `/api/v1/accounts/{id}/mark-dormant` | `DORMANT` |
| POST | `/api/v1/accounts/{id}/reactivate` | `ACTIVE` |
| POST | `/api/v1/accounts/{id}/close` | `CLOSED` |

### Holders, holds, limits

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/accounts/{id}/holders` | List |
| POST | `/api/v1/accounts/{id}/holders` | Add — `{cifNo, holderRole}` → `201` |
| GET | `/api/v1/accounts/{id}/holds` | List |
| POST | `/api/v1/accounts/{id}/holds` | Place — `{amount, reason, holdType}` → `201` |
| GET | `/api/v1/accounts/{id}/limits` | Read |
| PUT | `/api/v1/accounts/{id}/limits` | Set `{perTransactionLimit, dailyWithdrawalLimit}` |

`holderRole`: `PRIMARY` or `JOINT`. Manual `holdType`: `LIEN` or `MANUAL`
(`TRANSACTION` holds are created by transaction-service, not here).

**Internal:** `/internal/v1/accounts/{id}/transaction-context` ·
`/internal/v1/accounts/{id}/holds` (+ `/consume`, `/release`) ·
`/internal/v1/account-projections` · `/internal/v1/account-product-ownerships` ·
`/internal/v1/accounts/{id}/statement-context` · `/internal/v1/cards/{cardId}/payment-context`

---

## Transaction

### Creating transactions

**Every POST here requires an `Idempotency-Key` header.** All return `201` with the full
transaction.

| Method | Path | Type / Rail |
|---|---|---|
| POST | `/api/v1/transactions/deposits` | `DEPOSIT` / `CASH` |
| POST | `/api/v1/transactions/withdrawals` | `WITHDRAWAL` / `CASH` |
| POST | `/api/v1/transactions/transfers/internal` | `INTERNAL_TRANSFER` / `INTERNAL` |
| POST | `/api/v1/transactions/transfers/neft` | `NEFT` / `NEFT` |
| POST | `/api/v1/transactions/transfers/rtgs` | `RTGS` / `RTGS` |
| POST | `/api/v1/transactions/transfers/imps` | `IMPS` / `IMPS` |
| POST | `/api/v1/transactions/transfers/upi` | `UPI` / `UPI` |
| POST | `/api/v1/transactions/cheques` | `CHEQUE` / `CHEQUE` |
| POST | `/api/v1/transactions/card-payments` | `CARD_PAYMENT` / `CARD` |
| POST | `/api/v1/transactions/product-purchases` | `PRODUCT_PURCHASE` |

All except product-purchase take the same `CreateRequest`:

```json
{
  "sourceAccountId": "a0000000-...-101",
  "destinationAccountId": "a0000000-...-102",
  "cardId": null,
  "amount": 5000.00,
  "feeAmount": 0.00,
  "currency": "INR",
  "paymentChannel": "BRANCH",
  "paymentMethod": "ACCOUNT",
  "upiAddress": null,
  "chequeNumber": null,
  "narration": "Rent transfer",
  "clientReference": null
}
```

Required: `amount` (> 0.0001), `currency` (exactly 3 uppercase letters), `paymentChannel`,
`paymentMethod`. Which account fields you populate depends on the type — a deposit needs
`destinationAccountId`, a withdrawal needs `sourceAccountId`, a transfer needs both.

### Acting on transactions

All require an `Idempotency-Key` header.

| Method | Path | Permission | Body |
|---|---|---|---|
| POST | `/api/v1/transactions/{id}/approve` | `TRANSACTION_APPROVE` | none |
| POST | `/api/v1/transactions/{id}/reject` | `TRANSACTION_APPROVE` | `{reason}` |
| POST | `/api/v1/transactions/{id}/cancel` | `TRANSACTION_CANCEL` | `{reason}` |
| POST | `/api/v1/transactions/{id}/reversals` | `TRANSACTION_REVERSE` | `{reason}` required → `201` |

Approve rejects the maker approving their own transaction. A reversal creates a **new
compensating transaction** linked by `reversalOfTransactionId`; the original is never
mutated.

### Querying

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/transactions` | Spring `Page<TransactionView>` |
| GET | `/api/v1/transactions/{id}` | Full `TransactionView` |
| GET | `/api/v1/transactions/{id}/status` | Lightweight — for polling |
| GET | `/api/v1/transactions/by-reference/{reference}` | Idempotent lookup |
| GET | `/api/v1/transactions/{id}/product-purchase` | Purchase detail |
| GET | `/api/v1/transactions/approvals` | Pending approval queue |
| GET | `/api/v1/transactions/limits/quote` | Pre-flight limit check |
| GET | `/api/v1/accounts/{accountId}/transactions` | Account history (routed here, not to account-service) |
| GET | `/api/v1/accounts/{accountId}/mini-statement` | Latest N — `?size=` |

Search filters: `account` `reference` `status` `rail` `transactionType` `minAmount`
`maxAmount` `from` `to` (ISO date-time) `createdBy`, plus `page` `size` `sort`.
Default sort `createdAt,DESC`.

Approvals filters: `branch` `minAmount` `maxAmount` `rail`. Default sort `createdAt,ASC` —
oldest first, which is the right order for a work queue.

**Limit quote** — call this before enabling a submit button:

```
GET /api/v1/transactions/limits/quote
    ?accountId=...&transactionType=RTGS&rail=RTGS&channel=BRANCH&currency=INR&amount=250000
```

```json
{ "transactionType":"RTGS", "rail":"RTGS", "channel":"BRANCH", "currency":"INR",
  "requestedAmount":250000, "minAmount":200000, "maxAmount":null,
  "dailyLimit":null, "approvalThreshold":1000000,
  "allowed":true, "approvalRequired":false, "reason":null }
```

`TransactionView` is deep — it embeds `legs[]`, `hold`, `journals[]` (each with `lines[]`),
`clearing`, `railDetails` and `history[]`. Use `/status` when you only need the status.

### Reconciliation

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/reconciliation/exceptions` | `?status=` + paging |
| GET | `/api/v1/reconciliation/exceptions/{id}` | Read |
| POST | `/api/v1/reconciliation/exceptions/{id}/assign` | `{investigatorId}` |
| POST | `/api/v1/reconciliation/exceptions/{id}/resolve` | `{resolution, notes}` |

**Internal:** `/internal/v1/transactions/opening-deposits` ·
`/internal/v1/transactions/{id}/settle` · `/fail` · `/cheque-clearing` ·
`/internal/v1/reconciliation/runs`

---

## Ledger

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/ledger/accounts` | All GL accounts |
| GET | `/api/v1/ledger/accounts/{code}` | One GL account |
| GET | `/api/v1/ledger/accounts/{code}/balance` | GL balance |
| GET | `/api/v1/ledger/journals` | Search journals |
| GET | `/api/v1/ledger/journals/{id}` | Read by numeric id |
| GET | `/api/v1/ledger/journals/reference/{reference}` | Read by reference |
| POST | `/api/v1/ledger/journals` | Post a balanced journal |
| POST | `/api/v1/ledger/journals/{journalId}/reverse` | Reverse a posted journal |
| GET | `/api/v1/ledger/customer-accounts/{accountId}/entries` | Ledger view for a customer account |

Posting enforces balance: total debit must equal total credit and be > 0, and each line is
debit **or** credit, never both. An identical idempotent retry returns the existing journal.

**Internal:** `POST /internal/v1/ledger/journals` (requires `X-Service-Name`)

---

## Statement & Reporting

### Statements

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/statements/accounts/{accountId}/mini` | Mini statement — `?size=10` |
| POST | `/api/v1/statements/accounts/{accountId}` | Request generation → **`202 Accepted`**. Needs `Idempotency-Key` |
| GET | `/api/v1/statements/requests` | List own requests, paged |
| GET | `/api/v1/statements/requests/{id}` | Status + file metadata |
| POST | `/api/v1/statements/requests/{id}/cancel` | Cancel while queued |
| POST | `/api/v1/statements/requests/{id}/download-link` | Issue short-lived URL |
| GET | `/api/v1/statements/files/{fileId}/download?token=` | Download bytes |
| GET | `/api/v1/statements/download-history` | Audit of downloads |

`StatementRequestBody`: `fromDate`\* `toDate`\* `outputFormat`\* (`PDF` `CSV` `XLSX`)
`statementKind` (`DATE_RANGE` `MONTHLY` `YEARLY`)

Generation is **asynchronous**. `POST` returns `202` with status `PENDING`; poll
`GET /requests/{id}` until `status` is `READY`, then request a download link.

### Certificates

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/statements/accounts/{id}/interest-certificate?fiscalYear=` | Interest certificate bytes |
| GET | `/api/v1/statements/accounts/{id}/tds-certificate?fiscalYear=` | TDS certificate bytes |

### Reports

| Method | Path | Query |
|---|---|---|
| GET | `/api/v1/reports/daily-transactions` | `?date=YYYY-MM-DD` |
| GET | `/api/v1/reports/branches/{branchId}/transactions` | `?date=YYYY-MM-DD` |
| GET | `/api/v1/reports/dormant-accounts` | paged |
| GET | `/api/v1/reports/interest-accruals` | `?from=&to=` |
| GET | `/api/v1/reports/reconciliation` | `?date=YYYY-MM-DD` |

### Report schedules

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/report-schedules` | Create → `201` |
| GET | `/api/v1/report-schedules` | List, paged |
| GET | `/api/v1/report-schedules/{id}` | Read |
| PATCH | `/api/v1/report-schedules/{id}` | Update |
| DELETE | `/api/v1/report-schedules/{id}` | Cancel → `204` |

`ScheduleBody`: `accountId` `reportType`\* `outputFormat`\* `frequency`\* (`DAILY`
`WEEKLY` `MONTHLY` `YEARLY`) `nextRunAt`\*

**Internal:** `POST /internal/v1/statement-read-model/accounts` · `/transactions`

---

## Notification

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/notifications` | Queue → `201`. Honours `Idempotency-Key` as `dedup_key` |
| GET | `/api/v1/notifications` | `?status=` + paging |
| GET | `/api/v1/notifications/{notificationId}` | Delivery status |
| POST | `/api/v1/notifications/{notificationId}/retry` | Retry a failed delivery |
| GET | `/api/v1/notification-templates` | List |
| POST | `/api/v1/notification-templates` | Create → `201` |

Delivery is asynchronous. A notification whose recipient has opted out of that channel in
customer-service is recorded as `SUPPRESSED`, not `FAILED`.

---

## Audit

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/audit-events` | Search, paged |
| GET | `/api/v1/audit-events/{eventId}` | Read one |
| GET | `/api/v1/audit-events/trace/{correlationId}` | **Trace one request across every service** |
| GET | `/api/v1/audit-events/accounts/{accountId}` | Account activity |
| GET | `/api/v1/audit-events/transactions/{transactionId}` | Transaction activity |

Note the path is `/api/v1/audit-events`, **not** `/api/v1/audit/events`.

**Internal:** `POST /internal/v1/audit-events` (requires `X-Service-Name`)

---

## Configuration

All require `CONFIG_MANAGE`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/configuration/entries/{namespace}` | All keys in a namespace |
| GET | `/api/v1/configuration/entries/{namespace}/{configKey}` | Current value |
| GET | `/api/v1/configuration/entries/{namespace}/{configKey}/history` | All versions |
| POST | `/api/v1/configuration/entries` | New version → `201` |
| GET | `/api/v1/configuration/limits/channels` | Current channel limits |
| GET | `/api/v1/configuration/limits/channels/{channel}/{limitType}` | One limit |
| GET | `/api/v1/configuration/limits/channels/{channel}/{limitType}/history` | Versions |
| POST | `/api/v1/configuration/limits/channels` | New version → `201` |
| GET | `/api/v1/configuration/feature-flags` | List |
| GET | `/api/v1/configuration/feature-flags/{flagKey}` | Read |
| PUT | `/api/v1/configuration/feature-flags/{flagKey}` | Create or update |
| DELETE | `/api/v1/configuration/feature-flags/{flagKey}` | Delete |
| GET | `/api/v1/configuration/maintenance-windows` | List |
| GET | `/api/v1/configuration/maintenance-windows/{id}` | Read |
| POST | `/api/v1/configuration/maintenance-windows` | Create → `201` |
| PATCH | `/api/v1/configuration/maintenance-windows/{id}` | Update |
| POST | `/api/v1/configuration/maintenance-windows/{id}/status` | Transition status |
| GET | `/api/v1/configuration/maker-checker-thresholds` | Current set |
| GET | `/api/v1/configuration/maker-checker-thresholds/{actionType}` | One |
| GET | `/api/v1/configuration/maker-checker-thresholds/{actionType}/history` | Versions |
| POST | `/api/v1/configuration/maker-checker-thresholds` | New version → `201` |
| GET | `/api/v1/configuration/policies/password` \| `/session` \| `/otp` | Current policy |
| GET | `/api/v1/configuration/policies/{policy}/history` | Versions |
| POST | `/api/v1/configuration/policies/password` \| `/session` \| `/otp` | New version → `201` |

**Configuration is versioned, never edited.** Every change is a `POST` that creates a new
effective-dated version; the newest `effective_from` wins. There is no `PUT` on policies.

**Internal:** `GET /internal/v1/configuration/effective`

---

## Not implemented

Listed in the design catalogue, and in some cases in gateway config, but **no controller
exists**. Do not build UI against these:

| Endpoint | Note |
|---|---|
| `POST /api/v1/auth/password/forgot` | Whitelisted as public at the gateway, but unimplemented |
| `POST /api/v1/auth/password/reset` | Same |
| `POST /api/v1/auth/password/change` | No handler |
| `POST /api/v1/auth/otp/challenges` and `/verify` | No OTP flow exists |
| `GET /.well-known/jwks.json` | JWT is HMAC-signed with a shared secret, not RSA — there is no JWKS |
| `GET /api/v1/me` | Use `GET /api/v1/users/me` |
| `/api/v1/customers/{cif}/nominees` | Not built |
| `/api/v1/customers/{cif}/beneficiaries` | Table exists; **no controller** |
| `/api/v1/accounts/{id}/liens` | Use `/holds` with `holdType: LIEN` |
| `/api/v1/accounts/{id}/closure-requests` | Use `POST /close` |
| `/api/v1/accounts/{id}/linked-products` | `linked_cards` is served only over the internal card-context route |
| `/api/v1/accounts/{id}/interest/accruals` and `/postings` | Table exists; no public endpoint |
| `/api/v1/audit/exports`, `/retention-runs` | Not built |
| `PATCH /api/v1/accounts/applications/{id}` | No amend endpoint |
| `POST /api/v1/accounts/applications/{id}/submit` | No separate submit step |

Beneficiaries are the notable gap: the `beneficiaries` and `beneficiary_change_history`
tables are fully migrated and seeded, but nothing exposes them over HTTP, and
transaction-service stopped referencing `beneficiary_id` in V2. External transfers are
addressed by account id, not by stored beneficiary.
