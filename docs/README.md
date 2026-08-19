# MoneyBags — Developer Documentation

Complete reference for the MoneyBags banking platform: 14 Spring Boot modules, 12 MySQL
schemas, one API gateway. **This documentation is derived from the code as it exists**
(Flyway migrations for schema, `@RestController` classes for endpoints), not from design
intent — everything here is implemented and reachable.

## Read in this order

| Doc | What it answers |
|---|---|
| [01 — Architecture](01-ARCHITECTURE.md) | What runs where, on which port, how requests are routed, how to start it |
| [02 — Authentication & Authorization](02-AUTHENTICATION.md) | How to log in, what the JWT contains, which permission gates which call |
| [03 — Database Schema](03-DATABASE-SCHEMA.md) | Every table, column, type and constraint in all 12 schemas |
| [04 — API Reference](04-API-REFERENCE.md) | Every endpoint, with request and response shapes |
| [05 — Enums & Constants](05-ENUMS.md) | Every enum value — your dropdown and validation source |
| [06 — UI Integration Guide](06-UI-INTEGRATION-GUIDE.md) | Screen-by-screen data flows, conventions, worked examples |

Supporting documents already in this repo:

- [SEED_FIXTURES.md](SEED_FIXTURES.md) — the fixed test data (users, CIFs, accounts) every environment starts with
- [AUTH_SERVICE.md](AUTH_SERVICE.md) — notes on the merged standalone auth service
- [../SERVICE_SCHEMA_DIVISION.md](../SERVICE_SCHEMA_DIVISION.md) — why each table lives where it does

## The 30-second version

Everything the UI talks to goes through **one origin**: the API gateway on
`http://localhost:8090`. You never call a service port directly.

```bash
curl -X POST http://localhost:8090/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"teller1","password":"Password@123"}'
```

That returns a JWT. Send it as `Authorization: Bearer <accessToken>` on every subsequent
call. The gateway validates it and injects trusted actor headers (`X-User-Id`,
`X-Employee-Id`, `X-Branch-Code`, `X-Permissions`) into the downstream request — your UI
never sets those itself.

## Three things that will bite you if you skip them

1. **Only employees authenticate.** Customers are *data* in this system, not API callers.
   Every mutation is performed by a logged-in employee on behalf of a CIF. Build staff
   screens, not a customer portal — see [02](02-AUTHENTICATION.md#who-can-log-in).

2. **Financial POSTs require an `Idempotency-Key` header.** It is not optional; the request
   is rejected without it. Generate a UUID per user action and reuse it across retries —
   see [06](06-UI-INTEGRATION-GUIDE.md#idempotency).

3. **`/internal/v1/**` is blocked at the gateway** with `403 INTERNAL_ROUTE_BLOCKED`. Those
   routes exist for service-to-service calls only. They are documented here so you
   understand the system, but the UI cannot reach them.

## Live API explorer

With the stack running, aggregated Swagger UI is at
`http://localhost:8090/swagger-ui.html` — it lists all twelve service definitions.
