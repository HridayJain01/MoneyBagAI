# Moneybags Statement & Reporting Service

Standalone read-heavy service for statements, certificates and operational reports. It owns only its read projections, request/file/download metadata and schedules. `account_id`, `transaction_id`, and `ledger_entry_id` are indexed logical references; there are deliberately no cross-service database foreign keys.

## Source integration

- Account Service delivers idempotent account and balance events to `/internal/v1/statement-read-model/accounts`.
- Transaction Service delivers transaction entries to `/internal/v1/statement-read-model/transactions` only after the corresponding journal has been accepted by Ledger Service.
- There is no source fallback or mock-account path. A statement remains unavailable until its real account projection has arrived.
- Internal event endpoints require a trusted `X-Service-Name`; production ingress should additionally enforce mTLS/workload identity.

## Public scope

Implements mini and date/month/year statements; PDF/CSV/XLSX export; queued generation; signed protected downloads and history; schedules; daily/branch/dormant/interest/reconciliation reports; interest and TDS certificates. Audit is intentionally excluded.

The gateway-facing security adapter uses the repository's trusted identity headers: `X-User-Id`, `X-Customer-Id`, `X-Employee-Id`, `X-Branch-Id`, `X-Permissions`, and `X-Correlation-Id`. The gateway must strip user-supplied copies and inject verified claims.

For a complete copy-paste Swagger walkthrough, see [SWAGGER_TESTING.md](SWAGGER_TESTING.md).
