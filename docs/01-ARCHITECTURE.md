# 01 — Architecture

## Module map

Fourteen modules under `services/`, built by the root aggregator `pom.xml`. Each business
service owns exactly one MySQL schema and writes only to that schema.

| Port | Module | Eureka registration | Schema | Role |
|---|---|---|---|---|
| 8080 | `eureka-server` | — | — | Service registry |
| 8081 | `branch-employee-service` | `branch-employee-service` | `moneybags_branch` | Branches, employees, approval authority |
| 8082 | `customer-service` | `customer-service` | `moneybags_customer` | Customer master, canonical KYC status |
| 8083 | `account-service` | `account-service` | `moneybags_account` | Accounts, applications, balances, holds |
| 8084 | `transaction-service` | `transaction-service` | `moneybags_transaction` | Financial orchestration, journals, reversals |
| 8085 | `ledger-service` | `ledger-service` | `moneybags_ledger` | General ledger, double-entry journals |
| 8086 | `statement-reporting-service` | `statement-reporting-service` | `moneybags_statement` | Statements, reports, certificates (read-only) |
| 8087 | `identity-service` | **`security-service`** | `moneybags_identity` | Users, roles, permissions, JWT issuance |
| 8088 | `product-service` | `product-service` | `moneybags_product` | Product catalogue and versioned terms |
| 8089 | `notification-service` | `notification-service` | `moneybags_notification` | Email/SMS/push delivery |
| 8090 | `api-gateway` | `api-gateway` | — | **The only origin the UI talks to** |
| 8091 | `audit-service` | `audit-service` | `moneybags_audit` | Append-only audit event store |
| 8092 | `configuration-service` | `configuration-service` | `moneybags_configuration` | Limits, policies, feature flags |
| 8093 | `kyc-service` | `kyc-service` | `moneybags_kyc` | KYC workflow and binary evidence |

### Two facts that are load-bearing

**`identity-service` registers under the Eureka id `security-service`, not its module
name.** The gateway route and customer-service's `SecurityClient` both resolve
`security-service`. The service logs an error at startup if the name is changed.

**Port 8083 is pinned.** `statement-reporting-service` hardcodes `http://localhost:8083`
as its account-service default. Moving account-service breaks statement generation.

## Request flow

```
Browser / UI
     │  Authorization: Bearer <JWT>
     ▼
┌─────────────────────────────────────────────────────┐
│  api-gateway  :8090                                 │
│  1. resolve or generate X-Correlation-Id            │
│  2. reject anything starting /internal/  → 403      │
│  3. strip client-supplied actor headers             │
│  4. validate JWT (signature, issuer, audience, exp) │
│  5. inject X-User-Id, X-Employee-Id,                │
│     X-Branch-Code, X-Branch-Id, X-Permissions       │
└─────────────────────────────────────────────────────┘
     │  lb://<eureka-id>
     ▼
  business service — trusts the injected headers
```

Step 3 is what makes step 5 safe. The gateway removes `X-User-Id`, `X-Employee-Id`,
`X-Branch-Code`, `X-Branch-Id`, `X-Permissions`, `X-Customer-Id` and `X-Service-Name` from
every inbound request before adding its own, so a client cannot forge an identity.

> **Consequence:** the service ports must not be exposed beyond localhost. A direct request
> to `:8084` with hand-written actor headers bypasses the gateway entirely. In any shared
> environment, only `:8090` should be reachable.

## Gateway routing table

Routes are matched by `order` first, then declaration order. Two routes carry `order: -10`
because they are narrower than a route that would otherwise swallow them.

| Path pattern | Target | Note |
|---|---|---|
| `/api/v1/accounts/*/transactions` | transaction-service | `order: -10` |
| `/api/v1/accounts/*/mini-statement` | transaction-service | `order: -10` |
| `/api/v1/statements/accounts/**` | statement-reporting-service | `order: -10` |
| `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/roles/**`, `/api/v1/permissions/**`, `/api/v1/admin/**` | security-service | |
| `/api/v1/configuration/**` | configuration-service | |
| `/api/v1/customers/**` | customer-service | |
| `/api/v1/kyc/**` | kyc-service | |
| `/api/v1/branches/**`, `/api/v1/employees/**` | branch-employee-service | |
| `/api/v1/products/**` | product-service | |
| `/api/v1/accounts/**` | account-service | catch-all, after the two above |
| `/api/v1/transactions/**`, `/api/v1/reconciliation/**` | transaction-service | |
| `/api/v1/ledger/**` | ledger-service | |
| `/api/v1/statements/**`, `/api/v1/reports/**`, `/api/v1/report-schedules/**` | statement-reporting-service | |
| `/api/v1/notifications/**`, `/api/v1/notification-templates/**` | notification-service | |
| `/api/v1/audit-events/**` | audit-service | |

`spring.cloud.gateway.discovery.locator.enabled` is deliberately **off**. Enabling it would
auto-expose every registered service under `/<service-id>/**`, including their
`/internal/**` routes, defeating the internal-path block.

### Paths reachable without a JWT

```
/api/v1/auth/login          /api/v1/auth/register
/api/v1/auth/password/forgot  /api/v1/auth/password/reset
/actuator                   /swagger-ui
/v3/api-docs                /webjars
/<service>/api-docs         (Swagger aggregation, all twelve)
```

Everything else requires a valid JWT.

> `/api/v1/auth/password/forgot` and `/api/v1/auth/password/reset` are listed as public at
> the gateway but **no controller implements them** — see
> [04 — API Reference](04-API-REFERENCE.md#not-implemented). Do not build UI against them.

## Cross-service dependencies

Services never join across databases. A field pointing at another service's record is a
*logical* reference, resolved over HTTP.

```
customer-service ──► security-service      validate users.user_id exists
kyc-service      ──► customer-service      validate CIF; push KYC decision
account-service  ──► customer-service      eligibility / KYC summary
account-service  ──► product-service       resolve effective product terms
transaction-svc  ──► account-service       authorise debit, hold, consume, release
transaction-svc  ──► ledger-service        post balanced journal
transaction-svc  ──► account-service       card payment context
statement-svc    ──► account-service       account metadata (hardcoded :8083)
account-service  ──► statement-svc         outbox: account + transaction read models
* ──► notification-service, audit-service  outbox + scheduled HTTP push
```

There is **no message broker** in this deployment. Services that need to publish events use
a database outbox table plus a scheduled HTTP push (`account_outbox`,
`outbox_events`, `customer_domain_events`).

## Startup

`run-all.ps1` builds and starts everything in dependency-ordered waves, gating each wave on
`/actuator/health` returning `UP` — not merely on the TCP port being bound, because Flyway
may still be migrating behind a bound socket.

```bash
./run-all.ps1
```

| Flag | Effect |
|---|---|
| `-Reset` | Drops and recreates all 12 `moneybags_*` schemas first |
| `-SkipBuild` | Starts existing jars without rebuilding |
| `-Smoke` | Runs `smoke-test.ps1` end-to-end afterwards |

Wave order:

1. **Registry** — eureka-server
2. **Foundation** — identity, product, branch-employee, configuration, customer, kyc, ledger, notification, audit
3. **Accounts** — account-service
4. **Financial** — transaction-service, statement-reporting-service
5. **Edge** — api-gateway (last, so its route table resolves against a populated registry)

After the last wave the script waits up to 90s for every service to appear in the Eureka
registry. `lb://` routes return 503 until the gateway's client has fetched the registry, so
skipping that wait makes the first request look like a bug.

**Prerequisites:** JDK 17+, Maven on `PATH`, MySQL reachable on `localhost:3306`.
Credentials and ports are overridable via `.env` (see `.env.example`).

Useful endpoints once running:

| URL | What |
|---|---|
| `http://localhost:8090` | Gateway |
| `http://localhost:8090/swagger-ui.html` | Aggregated API explorer |
| `http://localhost:8080` | Eureka dashboard |
| `logs/` | Per-service stdout and stderr |

## Configuration

Every service reads the same environment variables with sensible localhost defaults:

| Variable | Default | Used by |
|---|---|---|
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | all |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `password` | all |
| `EUREKA_URL` | `http://localhost:8080/eureka/` | all |
| `JWT_SECRET` | dev-only literal | identity-service **and** api-gateway — must match |
| `JWT_ISSUER` | `moneybags-identity` | identity-service, api-gateway |
| `JWT_AUDIENCE` | `moneybags-api` | identity-service, api-gateway |
| `JWT_EXPIRATION_MINUTES` | `15` | identity-service |
| `AUTH_COOKIE_NAME` | `access-token` | identity-service, api-gateway |
| `AUTH_COOKIE_SECURE` | `false` | identity-service — set `true` behind HTTPS |
| `<SERVICE>_SERVICE_PORT` | see module map | respective service |

`JWT_SECRET` must be identical in identity-service and api-gateway or every authenticated
request fails validation at the gateway.
