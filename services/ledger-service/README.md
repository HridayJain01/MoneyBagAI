# MoneyBags Ledger Service

The Ledger Service is the authoritative, independently auditable double-entry general ledger for MoneyBags. It owns GL accounts, posted journals, journal lines, GL balances, and reversals.

## Responsibility boundaries

```text
Transaction Service
"What business transaction happened?"

        ↓ posts a balanced journal

Ledger Service
"How is it represented in double-entry accounting?"

        ↓ accounting result/event (future integration)

Account Service
"What is the customer's current spendable/account balance?"
```

- Transaction Service orchestrates deposits, withdrawals, transfers, and other business workflows.
- Ledger Service records their immutable double-entry accounting representation.
- Account Service owns customer `ledgerBalance`, `availableBalance`, holds, and account status.

The `balance` on `ledger_accounts` is a bank GL balance. It is not a customer's balance. `customerAccountId` on a journal line is an audit/sub-ledger association only.

## Accounting model

Every journal has at least two lines and must satisfy:

```text
SUM(DEBIT lines) = SUM(CREDIT lines)
```

Balance changes follow the account's normal side:

| Normal side | Debit | Credit |
|---|---:|---:|
| DEBIT | Increase | Decrease |
| CREDIT | Decrease | Increase |

Posted lines are never edited or deleted. Corrections create a new reversal journal with opposite sides. The original journal status becomes `REVERSED`, while its amounts and lines remain unchanged.

Concurrent postings lock affected GL rows in code order and also use optimistic versions. Journal references are unique and identical retries return the existing journal without applying balances again. Reusing a reference with different accounting details returns `409 Conflict`.

## Seeded accounts

| Code | Name | Type | Normal side |
|---|---|---|---|
| `110100` | Cash and Settlement Asset | ASSET | DEBIT |
| `210000` | Customer Deposit Control | LIABILITY | CREDIT |
| `220100` | Internal Payment Clearing | CLEARING | CREDIT |
| `220200` | External Clearing | CLEARING | CREDIT |
| `410100` | Payment Fee Income | INCOME | CREDIT |

Flyway creates and seeds these definitions. Their symbolic codes are also exposed under `moneybags.ledger.accounts` in `application.yml` so callers do not need scattered magic values.

## Account Service abstraction

`AccountLookupPort` validates every customer-account association against the live Account Service. Ledger Service stores only the UUID association on a journal line; it never stores or calculates the customer's spendable balance.

## Run locally

Requirements: Java 17+, Maven, and MySQL. The default JDBC URL creates the `moneybags_ledger` database when the configured MySQL user has permission. Override these values when needed:

```text
LEDGER_DB_URL=jdbc:mysql://localhost:3306/moneybags_ledger?createDatabaseIfNotExist=true&serverTimezone=UTC
LEDGER_DB_USERNAME=root
LEDGER_DB_PASSWORD=password
```

Run only this service:

```powershell
mvn spring-boot:run
```

Or run the MoneyBags stack from the repository root:

```powershell
.\start.ps1
```

Direct service URL: `http://localhost:8085`. Gateway URL: `http://localhost:8090`.

- OpenAPI JSON: `http://localhost:8085/api-docs`
- Direct Swagger UI: `http://localhost:8085/swagger-ui.html`
- Gateway Swagger UI: `http://localhost:8090/swagger-ui.html` and select `ledger-service`

## Post a deposit journal

```http
POST /internal/v1/ledger/journals
X-Service-Name: transaction-service
Content-Type: application/json
```

```json
{
  "journalReference": "JRN-TXN-501-DEPOSIT",
  "transactionId": "0e73454e-9fa2-4e31-9d6e-f4678d083f7e",
  "journalType": "DEPOSIT",
  "description": "Customer cash deposit",
  "currencyCode": "INR",
  "createdBy": "transaction-service",
  "lines": [
    {
      "ledgerCode": "110100",
      "side": "DEBIT",
      "amount": 500.00,
      "description": "Settlement cash received"
    },
    {
      "ledgerCode": "210000",
      "customerAccountId": "a0000000-0000-0000-0000-000000000101",
      "side": "CREDIT",
      "amount": 500.00,
      "description": "Increase customer deposit liability"
    }
  ]
}
```

## Reverse a journal

```http
POST /api/v1/ledger/journals/{journalId}/reverse
Content-Type: application/json
```

```json
{
  "journalReference": "REV-JE-501-DEPOSIT",
  "description": "Reverse mistaken deposit",
  "createdBy": "operations-user"
}
```

The request body is optional; the service generates a `REV-...` reference and description when omitted.

## Query endpoints

```text
GET /api/v1/ledger/accounts
GET /api/v1/ledger/accounts/{code}
GET /api/v1/ledger/accounts/{code}/balance
GET /api/v1/ledger/journals/{id}
GET /api/v1/ledger/journals/reference/{reference}
GET /api/v1/ledger/journals?transactionId={transactionId}
GET /api/v1/ledger/journals?customerAccountId={accountId}
GET /api/v1/ledger/customer-accounts/{accountId}/entries
```

## Tests

Tests use H2 in MySQL compatibility mode and run the same Flyway migrations:

```powershell
mvn test
```
