# MoneyBags service and schema division

## Core rule

Every service owns and writes only its own database. A field that identifies a
record owned by another service is a logical reference, validated through an
API or domain event, not a cross-database foreign key.

## Service ownership

| Service | Tables owned | Existing project action |
|---|---|---|
| Identity and Access | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `login_audit` | JWT authentication and access control |
| Customer | `customers`, `customer_addresses`, `kyc_documents`, `beneficiaries` | Extend current `customer-service` |
| Product | `products`, `product_charges` | Keep current `product-service` |
| Bank Organisation | `branches`, `employees` | New service; extract from current `security-service` |
| Account | `accounts`, `account_approvals` | Keep current `account-service` |
| Transaction | `transactions`, `transaction_rail_details`, `transaction_legs`, `funds_holds`, `journal_entries`, `journal_lines`, `clearing_instructions`, `outbox_events`, `transaction_status_history`, `idempotency_records`, `callback_receipts`, `transaction_limit_rules`, `reconciliation_exceptions` | Implemented in `transaction-service` |
| Statement | `moneybags_statement` (optional) | Optional read models: `account_statement_lines`, `statement_exports` | Keep current `statement-service` read-only |
| API Gateway | None | None | Keep current `api-gateway` |
| Eureka Server | None | None | Keep current `eureka-server` |

## Identity and Access Service

Tables: `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `login_audit`.

Local relationships:

```text
users 1 -> many user_roles
roles 1 -> many user_roles
users 1 -> many login_audit records
```

Outbound logical references held by other services:

```text
customers.user_id -> users.user_id
employees.user_id -> users.user_id
transactions.posted_by_user_id -> users.user_id
```

`customers.user_id` is unique. CIF remains owned by Customer Service; do not
store CIF as the main customer association in `users`.

## Customer Service

Tables: `customers`, `customer_addresses`, `kyc_documents`, `beneficiaries`.

Local relationships:

```text
customers 1 -> many customer_addresses
customers 1 -> many kyc_documents
customers 1 -> many beneficiaries
```

Logical references:

```text
customers.user_id -> Identity / users.user_id
customers.relationship_manager_emp_id -> Bank / employees.emp_id
kyc_documents.verified_by_emp_id -> Bank / employees.emp_id
```

`beneficiaries` belongs here because a beneficiary is a customer-maintained
payee. A future Payment Service can use it but should not own it.

## Product Service

Tables: `products`, `product_charges`.

Local relationship:

```text
products 1 -> many product_charges
```

Logical consumer:

```text
accounts.product_code -> products.product_code
```

## Bank Organisation Service

Tables: `branches`, `employees`.

Local relationship:

```text
branches 1 -> many employees
```

Logical references:

```text
employees.user_id -> Identity / users.user_id
customers.relationship_manager_emp_id -> employees.emp_id
kyc_documents.verified_by_emp_id -> employees.emp_id
account_approvals.emp_id -> employees.emp_id
transactions.performed_by_emp_id -> employees.emp_id
accounts.branch_code -> branches.branch_code
```

## Account Service

Tables: `accounts`, `account_approvals`.

Local relationship:

```text
accounts 1 -> many account_approvals
```

Logical references:

```text
accounts.cif_no -> Customer / customers.cif_no
accounts.product_code -> Product / products.product_code
accounts.branch_code -> Bank / branches.branch_code
account_approvals.emp_id -> Bank / employees.emp_id
```

The account is the required branch relationship. A customer does not require a
direct branch relationship unless the product later needs a home or servicing
branch independent of account ownership.

## Transaction Service

Transaction Service is the financial-orchestration and accounting-fact service. Its Flyway migration is the schema source of truth. Account Service remains authoritative for live ledger and available balances.

Local relationships:

```text
transactions 1 -> many transaction_legs
transactions 1 -> zero/one funds_holds
transactions 1 -> many journal_entries -> many journal_lines
transactions 1 -> zero/one clearing_instructions
transactions 1 -> many outbox_events
transactions 1 -> many transaction_status_history records
transactions.reversal_of_transaction_id -> transactions.transaction_id
```

Logical references:

```text
transactions.source_account_id -> Account Service
transactions.destination_account_id -> Account Service
transactions.maker_user_id / checker_user_id -> Identity Service
```

Ledger rules:

```text
Transaction legs describe customer/account effects.
Journal lines describe accounting effects and each line is debit or credit, never both.
Each posted journal has equal positive debit and credit totals.
Posted financial facts are immutable; reversals use a new linked compensating transaction.
Cross-service balance instructions are committed through the transactional outbox.
```

Keep transaction facts, journals, clearing, history, and outbox records in one service consistency boundary. Ledger account mappings are configuration/reference data; live customer balances are never stored here.

## Statement Service

Statement Service is read-only. It obtains account metadata from Account
Service and posted transaction/ledger data from Ledger Service. It does not
write account balances or ledger entries. Add local read-model tables only when
statement generation needs independent query scale or stored exports.

## Services not required yet

Do not create separate Payment or Loan services yet. Add them only when they
have their own workflow, external integration, or independent lifecycle.

Notification and Audit **have** since been built: both have an independent
lifecycle and must not participate in a financial commit, which is exactly the
bar above. They consume domain events through a database outbox plus a scheduled
HTTP push, because this deployment has no message broker.

**Card is deliberately still not a service.** transaction-service has a
`CardClient` expecting `GET /internal/v1/cards/{cardId}/payment-context`, and
Account Service serves that contract from its `linked_cards` table. A card is a
secondary product linked to an account, which is how the endpoint catalogue
models it (`/api/v1/accounts/{accountId}/linked-products`); it does not have a
lifecycle of its own yet.

## Current module layout

Thirteen modules under `services/`, built by the root aggregator `pom.xml`.

| Port | Module | Eureka name | Schema |
|---|---|---|---|
| 8080 | eureka-server | — | — |
| 8081 | branch-employee-service | `branch-employee-service` | `moneybags_branch` |
| 8082 | customer-service | `customer-service` | `moneybags_customer` |
| 8083 | account-service | `account-service` | `moneybags_account` |
| 8084 | transaction-service | `transaction-service` | `moneybags_transaction` |
| 8085 | ledger-service | `ledger-service` | `moneybags_ledger` |
| 8086 | statement-reporting-service | `statement-reporting-service` | `moneybags_statement` |
| 8087 | identity-service | **`security-service`** | `moneybags_identity` |
| 8088 | product-service | `product-service` | `moneybags_product` |
| 8089 | notification-service | `notification-service` | `moneybags_notification` |
| 8090 | api-gateway | `api-gateway` | — |
| 8091 | audit-service | `audit-service` | `moneybags_audit` |
| 8092 | configuration-service | `configuration-service` | `moneybags_configuration` |

Two things worth knowing before changing any of it:

- **identity-service registers as `security-service`**, not as its module name.
  customer-service's `SecurityClient` resolves that id. The service logs an error
  at startup if the name is ever changed.
- **Port 8083 is pinned.** statement-reporting-service hardcodes
  `http://localhost:8083` as its account-service default.

## Authentication

identity-service issues a signed, short-lived JWT containing the authenticated
user's roles, permissions, employee id and branch. The gateway validates its
signature, issuer, audience and expiry locally, then injects `X-User-Id`,
`X-Employee-Id`, `X-Branch-Code`, `X-Branch-Id`, `X-Permissions` and
`X-Correlation-Id` downstream. It strips all of those from inbound requests
first, so a client cannot forge them.

`X-Branch-Code` and `X-Branch-Id` deliberately carry the same value:
transaction-service reads the first, statement-reporting-service the second.

**Only employees authenticate.** Customers are data in this system, never API
callers; every mutation is performed by an employee on behalf of a CIF.

Consequence: the service ports must not be exposed beyond localhost. A direct
request to `:8084` with hand-written actor headers bypasses the gateway entirely.
