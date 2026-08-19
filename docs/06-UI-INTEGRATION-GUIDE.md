# 06 — UI Integration Guide

How to actually build the front end against this platform: conventions that apply
everywhere, then worked screen-by-screen flows.

## Ground rules

1. **One origin.** Everything goes to `http://localhost:8090`. Never call a service port.
2. **Staff app, not a customer portal.** Every screen is operated by a logged-in employee
   acting on behalf of a CIF.
3. **Never send actor headers.** The gateway strips and re-injects `X-User-Id`,
   `X-Employee-Id`, `X-Branch-Code`, `X-Permissions`. Sending them yourself does nothing.
4. **Do send `X-Correlation-Id`.** One UUID per user action, propagated across every
   service, queryable afterwards.

## Suggested HTTP client

```js
const BASE = 'http://localhost:8090';

async function api(path, { method = 'GET', body, idempotencyKey } = {}) {
  const headers = {
    'Content-Type': 'application/json',
    'X-Correlation-Id': crypto.randomUUID(),
  };
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;

  const res = await fetch(BASE + path, {
    method,
    headers,
    credentials: 'include',          // sends the HttpOnly access-token cookie
    body: body ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401) { redirectToLogin(); throw new Error('unauthenticated'); }
  if (res.status === 204) return null;

  const payload = await res.json().catch(() => null);
  if (!res.ok) throw new ApiError(payload, res.status);
  return payload;
}
```

`credentials: 'include'` is what makes the cookie flow work. If you prefer the header
instead, keep the token in memory (not `localStorage`) and set
`Authorization: Bearer ${token}`.

## Error handling

Service errors use a consistent envelope:

```json
{
  "timestamp": "2026-08-19T10:15:00Z",
  "status": 422,
  "code": "INSUFFICIENT_FUNDS",
  "message": "Available balance 4500.00 is less than requested 5000.00",
  "path": "/api/v1/transactions/withdrawals",
  "correlationId": "9f2c..."
}
```

Gateway rejections are shorter — `{ code, message, correlationId }` only.

| Status | Typical `code` | What it means | UI response |
|---|---|---|---|
| 400 | `REQUEST_VALIDATION_FAILED` | Bean-validation failure; `message` is `field: reason; field: reason` | Map onto form fields |
| 401 | `JWT_REQUIRED` / `JWT_INVALID` | No or bad token | Redirect to login |
| 403 | — | Missing permission or out of branch scope | "You do not have permission" |
| 403 | `INTERNAL_ROUTE_BLOCKED` | Called an `/internal/**` path | Bug in your code |
| 404 | — | Not found, or outside your scope | "Not found" |
| 409 | — | Idempotency conflict or invalid state transition | Refresh and re-read state |
| 422 | domain-specific | Business rule failed | Show `message` verbatim |
| 500 | `INTERNAL_ERROR` | Unexpected | Generic error + the `correlationId` |

**Always surface `correlationId` on an error screen.** It is the one thing that lets
someone trace what happened via `GET /api/v1/audit-events/trace/{correlationId}`.

Parse `REQUEST_VALIDATION_FAILED` messages by splitting on `; ` then `: `:

```js
function fieldErrors(message) {
  return Object.fromEntries(
    message.split('; ').map(part => {
      const i = part.indexOf(': ');
      return [part.slice(0, i), part.slice(i + 2)];
    })
  );
}
```

## Idempotency

Financial POSTs **require** an `Idempotency-Key` header. Without it the request fails.

Endpoints requiring it:

- every `POST /api/v1/transactions/*` — including `/approve`, `/reject`, `/cancel`, `/reversals`
- `POST /api/v1/statements/accounts/{accountId}`
- `POST /api/v1/notifications` (optional, but honoured as the dedup key)

**Generate the key when the user opens the form, not when they click submit.** That way a
double-click, a flaky network retry and a browser refresh all reuse the same key.

```js
function TransferForm() {
  const [idempotencyKey] = useState(() => crypto.randomUUID());  // once per form instance
  const submit = (values) =>
    api('/api/v1/transactions/transfers/internal',
        { method: 'POST', body: values, idempotencyKey });
}
```

Replaying the same key with the same body returns the original result. Replaying it with a
*different* body is a `409` — the request hash no longer matches.

## Pagination

Two envelopes. Normalise at the client boundary so your table components see one shape.

```js
function normalisePage(payload) {
  if ('items' in payload) {                       // identity, account, audit, notification, statement
    return { rows: payload.items, page: payload.page,
             size: payload.size, total: payload.totalItems };
  }
  return { rows: payload.content, page: payload.number,   // transaction, reconciliation
           size: payload.size, total: payload.totalElements };
}
```

Spring `Page` endpoints also accept `?sort=field,desc`. Custom-envelope endpoints do not.

## Money

Amounts are `DECIMAL(19,4)` in the database and arrive as JSON numbers everywhere except
statement-reporting-service, which wraps them as `{ "amount": "4750.00", "currency": "INR" }`.

**Do not do arithmetic in JavaScript floats.** Format for display; let the server compute.
Never recompute `availableBalance` client-side — read it from
`GET /api/v1/accounts/{id}/balance`, which derives it as
`ledgerBalance − heldAmount − minBalance + overdraftLimit`.

---

# Screen flows

## Login

```
POST /api/v1/auth/login   { username, password }
  → store permissions[] and expiresAt in client state
  → the access-token cookie is set automatically
```

Drive navigation from `permissions`. A teller has no `ACCOUNT_APPROVE`, so hide the
approvals queue rather than letting them click into a 403.

There is no refresh token. Watch `expiresAt` (15 min default) and warn or redirect before
the first 401.

## Customer onboarding

```
1.  POST /api/v1/customers                      → { cifNo }
2.  PUT  /api/v1/customers/{cif}/addresses
3.  POST /api/v1/kyc/sessions                   → { sessionId }   (KYC_VERIFY)
4.  POST /api/v1/kyc/sessions/{id}/documents    multipart/form-data
5.  POST /api/v1/kyc/sessions/{id}/frames       multipart/form-data
6.  GET  /api/v1/kyc/sessions/{id}/result       poll
7.  POST /api/v1/kyc/sessions/{id}/approve      (CHECKER or above)
        → kyc-service pushes the decision to customer-service
8.  GET  /api/v1/customers/{cif}/summary        kyc_status is now VERIFIED
```

Steps 4 and 5 are `multipart/form-data` — do **not** set `Content-Type` yourself, let the
browser set the boundary:

```js
const form = new FormData();
form.append('file', fileInput.files[0]);
form.append('documentType', 'AADHAAR');
await fetch(`${BASE}/api/v1/kyc/sessions/${id}/documents`,
            { method: 'POST', body: form, credentials: 'include' });
```

Drive a progress stepper from `KycSessionStatus` — the nine values map cleanly onto stages.

Show `GET /api/v1/customers/{cif}/completeness` as a profile-completeness meter.

## Opening an account

This is a **maker-checker** flow. It needs two different users.

```
As teller1 (ACCOUNT_OPEN):
  GET  /api/v1/customers/{cif}/eligibility     ← check BEFORE showing the form
  GET  /api/v1/products?status=ACTIVE          ← product dropdown
  POST /api/v1/accounts/applications           → status PENDING_APPROVAL

As checker1 (ACCOUNT_APPROVE):
  GET  /api/v1/accounts/applications?status=PENDING_APPROVAL
  POST /api/v1/accounts/applications/{id}/approve   { remarks }
       → response carries createdAccountId
  GET  /api/v1/accounts/{createdAccountId}
```

Three things to build in:

- **Check eligibility first.** A customer with `kyc_status = PENDING` cannot open an
  account. Use `CIF900102` to test the denial path.
- **The maker cannot approve their own application.** If the logged-in employee created it,
  disable the approve button — the server rejects it anyway, but a disabled button is
  better UX than a 403.
- **Products with `requiresFunding`** (all FD/RD) create the account as
  `PENDING_ACTIVATION` rather than `ACTIVE`. Say so on the confirmation screen.

Reject requires a reason: `POST .../reject { "reason": "..." }`.

## Making a transfer

```
1. GET  /api/v1/accounts?cifNo={cif}                    ← pick source
2. GET  /api/v1/accounts/{sourceId}/balance             ← show available
3. GET  /api/v1/transactions/limits/quote?...           ← pre-flight
4. POST /api/v1/transactions/transfers/internal         Idempotency-Key required
5. GET  /api/v1/transactions/{id}/status                poll until terminal
```

Step 3 is the one people skip and then wonder why the submit failed. It returns:

```json
{ "allowed": true, "approvalRequired": false,
  "minAmount": 200000, "maxAmount": null, "approvalThreshold": 1000000, "reason": null }
```

- `allowed: false` → disable submit, show `reason`
- `approvalRequired: true` → warn the user this will go to a checker rather than settle
  immediately

The seeded rule makes RTGS reject under ₹200 000 and require approval over ₹1 000 000.

After submit, the transaction is **not** necessarily done. Poll
`GET /api/v1/transactions/{id}/status` until `TransactionStatus` is terminal (`COMPLETED`
`FAILED` `REJECTED` `CANCELLED` `REVERSED`). Use `/status`, not the full
`GET /api/v1/transactions/{id}` — the latter returns legs, journals, clearing and history,
which is a lot of payload to poll.

Externally-cleared rails (NEFT/RTGS/IMPS/UPI/CHEQUE/CARD) complete via an asynchronous
callback and can sit in `PROCESSING` or `SETTLED` for a while. Cash and internal transfers
usually reach `COMPLETED` immediately.

## Transaction approval queue

```
GET /api/v1/transactions/approvals?branch=BR001&sort=createdAt,asc
POST /api/v1/transactions/{id}/approve    Idempotency-Key required
POST /api/v1/transactions/{id}/reject     { reason }
```

Default sort is oldest-first, which is correct for a work queue — do not flip it.

## Reversing a transaction

Requires `TRANSACTION_REVERSE` (BRANCH_MANAGER or OPS_ADMIN).

```
POST /api/v1/transactions/{id}/reversals   { reason }   → 201, a NEW transaction
```

The original transaction is **never mutated**. The reversal is a separate compensating
transaction linked by `reversalOfTransactionId`. Show both rows in the history, linked —
do not hide the original or overwrite its status in your UI cache.

## Account dashboard

For one account, in parallel:

```
GET /api/v1/accounts/{id}                    header — name, product, status
GET /api/v1/accounts/{id}/balance            balance tiles
GET /api/v1/accounts/{id}/mini-statement     recent activity (routed to transaction-service)
GET /api/v1/accounts/{id}/holds              active holds
GET /api/v1/accounts/{id}/products           owned catalogue products
```

Show `heldAmount` prominently when it is non-zero — "why is my available balance lower
than my ledger balance" is the most common support question, and the holds list answers it.

Gate the lifecycle buttons (freeze/block/close) on `ACCOUNT_STATUS_MANAGE`, and on the
current status: `allowsDebit` is true only for `ACTIVE`, and `CLOSED` is terminal.

## Statements

Generation is asynchronous.

```
1. POST /api/v1/statements/accounts/{id}   { fromDate, toDate, outputFormat }
       Idempotency-Key required            → 202 Accepted, status PENDING
2. GET  /api/v1/statements/requests/{id}   poll until status READY
3. POST /api/v1/statements/requests/{id}/download-link   → { url, expiresAt }
4. navigate to url
```

Do not treat the `202` as success. Poll step 2 — status moves
`PENDING → GENERATING → READY`, or to `FAILED` with `errorCode` and `errorMessage`.

Download links are short-lived; re-request rather than caching the URL. Every download is
recorded in `statement_download_history` with an outcome of `SUCCESS` `DENIED` `EXPIRED`
or `FAILED`.

For a quick on-screen view use `GET /api/v1/statements/accounts/{id}/mini?size=10`, which
is synchronous.

## Audit trail

```
GET /api/v1/audit-events/trace/{correlationId}       one request across all services
GET /api/v1/audit-events/accounts/{accountId}        account lifecycle
GET /api/v1/audit-events/transactions/{transactionId} transaction activity
```

Note the path is `/api/v1/audit-events`, not `/api/v1/audit/events`.

If you set `X-Correlation-Id` on every request, the trace endpoint becomes a genuine
debugging tool: one id, the complete cross-service story.

---

## Screen-to-endpoint map

| Screen | Endpoints | Permission |
|---|---|---|
| Login | `POST /auth/login` | public |
| Dashboard | `GET /users/me` | JWT |
| Customer search | `GET /customers/search?query=` | `CUSTOMER_READ` |
| Customer detail | `GET /customers/{cif}`, `/summary`, `/completeness` | `CUSTOMER_READ` |
| Customer create/edit | `POST /customers`, `PATCH /customers/{cif}` | `CUSTOMER_UPDATE` |
| KYC workflow | `/kyc/sessions/**` | `KYC_VERIFY` |
| KYC review queue | `GET /customers/kyc/pending` | `KYC_VERIFY` |
| Product catalogue | `GET /products` | `PRODUCT_READ` |
| Product admin | `POST/PATCH /products/**` | `PRODUCT_MANAGE` |
| Account search | `GET /accounts` | `ACCOUNT_VIEW` |
| Account detail | `GET /accounts/{id}` + `/balance` `/holds` `/products` | `ACCOUNT_VIEW` |
| Open account | `POST /accounts/applications` | `ACCOUNT_OPEN` |
| Application queue | `GET /accounts/applications?status=PENDING_APPROVAL` | `ACCOUNT_APPROVE` |
| Account lifecycle | `POST /accounts/{id}/freeze` etc. | `ACCOUNT_STATUS_MANAGE` |
| Teller operations | `POST /transactions/deposits`, `/withdrawals` | `TRANSACTION_CREATE` |
| Transfers | `POST /transactions/transfers/*` | `TRANSACTION_CREATE` |
| Transaction history | `GET /transactions`, `/accounts/{id}/transactions` | `TRANSACTION_VIEW` |
| Approval queue | `GET /transactions/approvals` | `TRANSACTION_APPROVE` |
| Reversals | `POST /transactions/{id}/reversals` | `TRANSACTION_REVERSE` |
| Reconciliation | `/reconciliation/exceptions/**` | `RECONCILIATION_MANAGE` |
| Statements | `/statements/**` | `STATEMENT_VIEW` |
| Reports | `/reports/**` | `REPORT_VIEW` |
| Report schedules | `/report-schedules/**` | `REPORT_ADMIN` |
| Ledger explorer | `/ledger/**` | staff |
| Notifications | `/notifications/**` | `NOTIFICATION_MANAGE` |
| Audit search | `/audit-events/**` | `AUDIT_VIEW` |
| User admin | `/users/**` | `USER_MANAGE` |
| Role admin | `/roles/**`, `/permissions/**` | `ROLE_PERMISSION_MANAGE` |
| Configuration | `/configuration/**` | `CONFIG_MANAGE` |
| Branches & staff | `/branches/**`, `/employees/**` | `BRANCH_MANAGE`, `EMPLOYEE_MANAGE` |

## Gaps to plan around

These have database tables but **no HTTP endpoint** — see
[04 — Not implemented](04-API-REFERENCE.md#not-implemented):

- **Beneficiaries.** Fully migrated and seeded, entity and repository exist, but no
  controller. External transfers are addressed by account id, not by a stored payee. If the
  design calls for a beneficiary picker, that API has to be built first.
- **Interest accruals and postings.** Recorded by the EOD job, not readable over HTTP.
- **Linked cards.** Only reachable over the internal card-context route.
- **Password reset, password change, OTP.** No handler exists, despite the gateway
  whitelisting two of the paths. There is no self-service password recovery — an admin
  must use `POST /api/v1/users/{userId}/unlock` or reset via user administration.
- **Account closure requests.** `POST /accounts/{id}/close` is a direct status change; there
  is no settlement-calculation workflow.

## End-to-end smoke test

Verifies the full financial vertical against a running stack:

```bash
./run-all.ps1 -Smoke
```

Or manually, from login through to a settled transfer:

```bash
curl -c cookies.txt -X POST http://localhost:8090/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"teller1","password":"Password@123"}'
```

```bash
curl -b cookies.txt -X POST http://localhost:8090/api/v1/transactions/transfers/internal -H "Content-Type: application/json" -H "Idempotency-Key: $(uuidgen)" -d '{"sourceAccountId":"a0000000-0000-0000-0000-000000000101","destinationAccountId":"a0000000-0000-0000-0000-000000000102","amount":100.00,"currency":"INR","paymentChannel":"BRANCH","paymentMethod":"ACCOUNT","narration":"smoke test"}'
```
