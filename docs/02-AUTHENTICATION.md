# 02 — Authentication & Authorization

## Who can log in

**Only employees authenticate.** Customers are records in `moneybags_customer`, not API
callers. Every mutation is performed by a logged-in employee acting on behalf of a CIF.

There is a public `POST /api/v1/auth/register` endpoint that self-registers a user with the
`CUSTOMER` role, carried over from the merged standalone auth-service. That role is seeded
with **no permissions**, so a self-registered user can log in and read
`/api/v1/users/me` but is denied everything else. Treat registration as a legacy
compatibility surface, not a customer portal entry point.

Build **staff screens**: teller, checker, branch manager, ops admin.

## Logging in

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "username": "teller1", "password": "Password@123" }
```

`username` also accepts the JSON alias `email`, so `{"email": "...", "password": "..."}`
binds to the same field.

**200 OK**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-19T10:15:00Z",
  "userId": 1,
  "username": "teller1",
  "fullName": "Amit Patel",
  "employeeId": "1001",
  "branchCode": "BR001",
  "roles": ["TELLER"],
  "permissions": ["CUSTOMER_READ", "CUSTOMER_UPDATE", "PRODUCT_READ", "..."]
}
```

The response also sets an `HttpOnly` cookie named `access-token` carrying the same JWT.

### Two ways to send the token

The gateway accepts either, checking the header first:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

or the `access-token` cookie, which the browser sends automatically after login.

For a browser SPA the cookie is the better choice — it is `HttpOnly`, so XSS cannot read
it. Note the cookie defaults to `SameSite=Strict` and `Secure=false`; behind HTTPS set
`AUTH_COOKIE_SECURE=true`.

**Store `permissions` from the login response in your client state.** It is the list you
drive menu visibility and button enablement from. The server enforces independently — the
client-side check is only to avoid showing an action that will 403.

### Token lifetime

15 minutes by default (`JWT_EXPIRATION_MINUTES`). There is **no refresh token endpoint**.
When the token expires the user must log in again. Plan for this: watch `expiresAt` and
either warn the user or redirect to login before the first 401 lands.

### Logging out

```http
POST /api/v1/auth/logout
```

Returns `204 No Content` and clears the cookie. Authentication is stateless — the JWT
remains cryptographically valid until it expires. Logout records an audit event and clears
the cookie; **the client must discard its own copy of the token.** There is no server-side
revocation.

## What the gateway injects

After validating the JWT the gateway rewrites the request for the downstream service:

| Header | Source | Present when |
|---|---|---|
| `X-User-Id` | JWT subject | always |
| `X-Permissions` | JWT claim, comma-joined | always |
| `X-Employee-Id` | JWT claim | user has an employee record |
| `X-Branch-Code` | JWT claim | user has a branch |
| `X-Branch-Id` | same value as `X-Branch-Code` | user has a branch |
| `X-Correlation-Id` | inbound header, or a fresh UUID | always |

`X-Branch-Code` and `X-Branch-Id` deliberately carry the **same value**:
transaction-service reads the first, statement-reporting-service the second.

Your UI never sets any of these. The gateway strips them from inbound requests before
injecting its own.

**`X-Correlation-Id` is worth setting.** If you send one, the gateway propagates it through
every downstream hop and it is echoed on the response and stored on every audit event. It
turns "the transfer failed" into a single traceable query against
`GET /api/v1/audit-events/trace/{correlationId}`.

## Roles and permissions

Four seeded roles. Permissions are cumulative down the list.

| Role | Permissions |
|---|---|
| `TELLER` | `CUSTOMER_READ` `CUSTOMER_UPDATE` `PRODUCT_READ` `ACCOUNT_OPEN` `ACCOUNT_VIEW` `TRANSACTION_CREATE` `TRANSACTION_VIEW` `TRANSACTION_CANCEL` `STATEMENT_VIEW` |
| `CHECKER` | everything TELLER has, plus `KYC_VERIFY` `ACCOUNT_APPROVE` `TRANSACTION_APPROVE` |
| `BRANCH_MANAGER` | everything CHECKER has, plus `ACCOUNT_VIEW_ALL_BRANCHES` `ACCOUNT_STATUS_MANAGE` `TRANSACTION_REVERSE` `TRANSACTION_VIEW_ALL_BRANCHES` `TRANSACTION_CANCEL_ANY` `REPORT_VIEW` |
| `OPS_ADMIN` | all 28 permissions |
| `CUSTOMER` | none (self-registration default) |

### Full permission catalogue

| Permission | Owning service | Gates |
|---|---|---|
| `USER_MANAGE` | identity | User CRUD, lock/unlock, role assignment |
| `ROLE_PERMISSION_MANAGE` | identity | Role and permission administration |
| `CUSTOMER_READ` | customer | Read customer and KYC summary |
| `CUSTOMER_UPDATE` | customer | Update customer profile |
| `KYC_VERIFY` | customer / kyc | Verify or reject KYC documents and sessions |
| `PRODUCT_READ` | product | View catalogue and rules |
| `PRODUCT_MANAGE` | product | Create and change products and rates |
| `ACCOUNT_OPEN` | account | Create an account application |
| `ACCOUNT_APPROVE` | account | Approve or reject account opening |
| `ACCOUNT_VIEW` | account | View permitted account details and balances |
| `ACCOUNT_VIEW_ALL_BRANCHES` | account | View accounts outside own branch |
| `ACCOUNT_STATUS_MANAGE` | account | Freeze, unfreeze, block, close |
| `TRANSACTION_CREATE` | transaction | Initiate a financial transaction |
| `TRANSACTION_APPROVE` | transaction | Approve maker-checker transactions |
| `TRANSACTION_REVERSE` | transaction | Create compensating reversal |
| `TRANSACTION_VIEW` | transaction | View transaction history |
| `TRANSACTION_VIEW_ALL_BRANCHES` | transaction | View transactions outside own branch |
| `TRANSACTION_CANCEL` | transaction | Cancel own pending transaction |
| `TRANSACTION_CANCEL_ANY` | transaction | Cancel any pending transaction |
| `RECONCILIATION_MANAGE` | transaction | Manage reconciliation exceptions |
| `STATEMENT_VIEW` | statement | Generate permitted statements |
| `REPORT_VIEW` | statement | View operational reports |
| `REPORT_ADMIN` | statement | Administer report schedules |
| `AUDIT_VIEW` | audit | Search and export audit records |
| `CONFIG_MANAGE` | configuration | Change limits, policies, feature flags |
| `BRANCH_MANAGE` | branch-employee | Create and change branches |
| `EMPLOYEE_MANAGE` | branch-employee | Create and change employees |
| `NOTIFICATION_MANAGE` | notification | Retry and administer notifications |

These strings are matched **literally** by code across services. Renaming one breaks every
service that checks it.

## Scoping rules

Permission alone is not sufficient — access is also **branch-scoped**.

- A user sees accounts and transactions for their own `branchCode` only, unless they hold
  `ACCOUNT_VIEW_ALL_BRANCHES` / `TRANSACTION_VIEW_ALL_BRANCHES`.
- **Maker and checker must be different users.** `POST /api/v1/accounts/applications/{id}/approve`
  and `POST /api/v1/transactions/{id}/approve` both reject an approval by the employee who
  created the record. This is why the seed data pairs `teller1` and `checker1` in the same
  branch (BR001) — an approval across branches is also rejected.

## Test credentials

All seeded users share the password `Password@123`.

| Username | Employee | Branch | Role |
|---|---|---|---|
| `teller1` | 1001 | BR001 | TELLER |
| `checker1` | 1002 | BR001 | CHECKER |
| `manager1` | 1003 | BR002 | BRANCH_MANAGER |
| `opsadmin` | 1004 | BR001 | OPS_ADMIN |

Use `teller1` + `checker1` for any maker-checker flow — same branch, complementary roles.

## Failure responses

| Status | `code` | Meaning | UI action |
|---|---|---|---|
| 401 | `JWT_REQUIRED` | No token in header or cookie | Redirect to login |
| 401 | `JWT_INVALID` | Bad signature, wrong issuer/audience, or expired | Redirect to login |
| 403 | `INTERNAL_ROUTE_BLOCKED` | Called an `/internal/**` path | Bug — fix the call |
| 403 | — | Authenticated but missing permission or out of branch scope | Show "not permitted" |

Gateway rejections have a compact body:

```json
{ "code": "JWT_INVALID", "message": "...", "correlationId": "..." }
```

Service-level errors use the fuller envelope described in
[06 — UI Integration Guide](06-UI-INTEGRATION-GUIDE.md#error-handling).

## Account lockout

After `MAX_FAILED_ATTEMPTS` (default 5) failed logins the user is locked for
`LOCK_DURATION_MINUTES` (default 15). `users.failed_attempts` and `users.locked_until`
track this. An `OPS_ADMIN` can clear it early with
`POST /api/v1/users/{userId}/unlock`.
