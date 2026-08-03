# MoneyBags service and schema division

## Core rule

Every service owns and writes only its own database. A field that identifies a
record owned by another service is a logical reference, validated through an
API or domain event, not a cross-database foreign key.

## Service ownership

| Service | Tables owned | Existing project action |
|---|---|---|
| Identity and Access | `users`, `roles`, `user_roles`, `user_sessions`, `login_audit` | Extract from current `security-service` |
| Customer | `customers`, `customer_addresses`, `kyc_documents`, `beneficiaries` | Extend current `customer-service` |
| Product | `products`, `product_charges` | Keep current `product-service` |
| Bank Organisation | `branches`, `employees` | New service; extract from current `security-service` |
| Account | `accounts`, `account_approvals` | Keep current `account-service` |
| Ledger | `transactions`, `ledger_entries`, `gl_accounts` | Evolve and rename current `transaction-service` |
| Statement | `moneybags_statement` (optional) | Optional read models: `account_statement_lines`, `statement_exports` | Keep current `statement-service` read-only |
| API Gateway | None | None | Keep current `api-gateway` |
| Eureka Server | None | None | Keep current `eureka-server` |

## Identity and Access Service

Tables: `users`, `roles`, `user_roles`, `user_sessions`, `login_audit`.

Local relationships:

```text
users 1 -> many user_roles
roles 1 -> many user_roles
users 1 -> many user_sessions
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

## Ledger Service

Tables: `transactions`, `ledger_entries`, `gl_accounts`.

Local relationships:

```text
transactions 1 -> many ledger_entries
transactions.reversal_of -> transactions.txn_id
gl_accounts.parent_gl_code -> gl_accounts.gl_code
ledger_entries.gl_code -> gl_accounts.gl_code
```

Logical references:

```text
ledger_entries.account_no -> Account / accounts.account_no
transactions.posted_by_user_id -> Identity / users.user_id
transactions.performed_by_emp_id -> Bank / employees.emp_id
```

Ledger rules:

```text
Each posted transaction has two or more ledger entries.
Sum of debit entries must equal sum of credit entries.
Each ledger entry uses either account_no or gl_code, never both and never neither.
Amounts are positive; dr_cr determines debit or credit.
Posted ledger entries are immutable; reversals use a new transaction.
```

Keep the three ledger tables in one service and one database transaction. Do
not split `ledger_entries` or `gl_accounts` into separate services.

## Statement Service

Statement Service is read-only. It obtains account metadata from Account
Service and posted transaction/ledger data from Ledger Service. It does not
write account balances or ledger entries. Add local read-model tables only when
statement generation needs independent query scale or stored exports.

## Services not required yet

Do not create separate Notification, Payment, Reporting, Card, or Loan
services yet. Add them only when they have their own workflow, external
integration, or independent lifecycle.
