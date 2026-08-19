# Moneybags console — build progress

Last updated: 2026-08-19 · after **Phase 1** · commit `ad8eca5` · branch `UI-TRIAL`

## 1. Read this first

This file records **where the build has got to and why it got there that way**. It is not a
guide to how the app works — `frontend/README.md` is that, and it is excellent; read it
first.

Reading order for anyone picking this up cold:

| Order | File | What it gives you |
|---|---|---|
| 1 | `frontend/README.md` | How the app is built: JET gotchas, the three list envelopes, the design language, the dev proxy |
| 2 | `docs/04`, `docs/05`, `docs/06` | The backend contract — **but see §7, which records where these are wrong** |
| 3 | this file | Build state, decisions already settled, defects, verification |
| 4 | `docs/SEED_FIXTURES.md` | The fixed test data every environment starts with |

The plan this build follows is at `.claude/plans/peaceful-dancing-stardust.md`.

**§6 and §8 are append-only.** Never edit or delete a row in them — they exist so decisions
and known defects are not silently relitigated by the next person or tool.

## 2. Phase status

| Phase | Scope | Status | Commit |
|---|---|---|---|
| 1 | Foundations, bug fixes, `txn.js`, support mixins | **DONE** | `ad8eca5` |
| 2 | Teller operations (deposit, withdrawal, cheque) | NOT STARTED | — |
| 3 | Transfers (internal, NEFT, RTGS, IMPS, UPI) | NOT STARTED | — |
| 4 | Account opening + applications queue | NOT STARTED | — |
| 5 | Account lifecycle, holds, status history | NOT STARTED | — |
| 6 | Transaction detail, cancel, reversal | NOT STARTED | — |
| 7 | Consolidation (optional) | NOT STARTED | — |

## 3. Screen inventory

| Route | ViewModel | View | Nav group | Route permission | State |
|---|---|---|---|---|---|
| `login` | `login.js` | `login.html` | — | public | built |
| `overview` | `overview.js` | `overview.html` | Workspace | — | built |
| `approvals` | `approvals.js` | `approvals.html` | Workspace | `TRANSACTION_APPROVE` | built |
| `customers` | `customers.js` | `customers.html` | Banking | `CUSTOMER_READ` | built |
| `customerDetail` | `customerDetail.js` | `customerDetail.html` | — (route only) | — | built |
| `accounts` | `accounts.js` | `accounts.html` | Banking | `ACCOUNT_VIEW` | built |
| `accountDetail` | `accountDetail.js` | `accountDetail.html` | — (route only) | — | built |
| `transactions` | `transactions.js` | `transactions.html` | Banking | `TRANSACTION_VIEW` | built |
| `ledger` | `ledger.js` | `ledger.html` | Back office | — | **placeholder** |
| `products` | `products.js` | `products.html` | Back office | `PRODUCT_READ` | **placeholder** |
| `branches` | `branches.js` | `branches.html` | Back office | — | **placeholder** |
| `notifications` | `notifications.js` | `notifications.html` | Back office | `NOTIFICATION_MANAGE` | **placeholder** |
| `audit` | `audit.js` | `audit.html` | Back office | `AUDIT_VIEW` | **placeholder** |
| `tellerOps` | — | — | Workspace | `TRANSACTION_CREATE` | planned (P2) |
| `transfers` | — | — | Workspace | `TRANSACTION_CREATE` | planned (P3) |
| `openAccount` | — | — | — (route only) | — | planned (P4) |
| `applications` | — | — | Workspace | `ACCOUNT_VIEW` | planned (P4) |
| `transactionDetail` | — | — | — (route only) | — | planned (P6) |

"placeholder" = a generic auto-column table via `genericList.js` + the inline
`#mbGenericList` template in `index.html`. Read-only, deliberately shallow.

## 4. What Phase 1 delivered

Nothing user-visible except two bug fixes. Its purpose is to make Phases 2–6 pure
"add a screen" diffs.

### Created

| File | What |
|---|---|
| `src/services/txn.js` | Money-movement domain module: `RAILS` catalogue (8 buildable operations), `bodyFor`, `quote`, `quoteAccountFor`, `quoteHasBounds`, `pollStatus`, `isTerminal`, `awaitsHuman`. Owns every backend quirk in §7 |
| `src/viewModels/support/banner.js` | `Banner.call(self)` mixin — `banner`, `hasBanner`, `bannerClass`, `dismissBanner`, `notify`, `failed`. Replaces five hand-rolled copies |
| `src/viewModels/support/confirm.js` | `Confirm.call(self, {dialogId})` mixin — `pending`, `busy`, `confirmPayload`, `openConfirm`, `closeConfirm`, `cancelConfirm`, `runConfirm`. Encodes the idempotency-key discipline once |
| `frontend/PROGRESS.md` | This file |

### Modified

| File | Change |
|---|---|
| `src/viewModels/accountDetail.js` | **Bug fix.** `navigation` was used at line 32 but missing from `define()`; in Chrome it resolved to `window.navigation` and threw `TypeError: navigation.param is not a function`, breaking the entire screen |
| `src/viewModels/transactions.js` | **Bug fix.** Status filter offered `PENDING`, which is not in `TransactionStatus`; the param binds to the enum so it 400d. Replaced with all 14 real values |
| `src/services/http.js` | Added `beginIntent()` → `{idempotencyKey, correlationId}`, `fieldErrorsFor(err)`, and `opts.correlationId` support so one intent can span several requests under one trace id |
| `src/services/endpoints.js` | 33 new bindings (below); deleted `customers.beneficiaries` (no controller exists) |
| `src/viewModels/approvals.js` | Refactored onto both mixins. This is the regression proof — it was the only working write flow |
| `src/views/approvals.html` | Dialog id `confirmDialog` → `approvalsConfirmDialog` (ids must be unique per screen); `cancelDialog` → `cancelConfirm` |
| `src/styles/layout.css` | Added `.mb-banner--warning`; `fmt.toneFor` returns `'warning'` but no such class existed, so those banners rendered unstyled |
| `docs/04-API-REFERENCE.md` | Corrected `paymentMethod`, added the validated-side table, `?type=` → `?productType=`, flagged `/card-payments` as not callable |
| `docs/05-ENUMS.md` | Added the rail→method derivation table and the single-seeded-limit-rule warning |
| `smoke-test.ps1` | **Bug fix.** Three request bodies were invalid — see §8 |

### Endpoints newly wired

```
customers.eligibility                         GET  /customers/{cif}/eligibility
accounts.byNumber                             GET  /accounts/by-number/{n}
accounts.application                          GET  /accounts/applications/{id}
accounts.createApplication                    POST /accounts/applications
accounts.approveApplication|reject|cancel     POST /accounts/applications/{id}/...
accounts.markDormant|reactivate|close         POST /accounts/{id}/...
accounts.statusHistory|holders|holds|limits   GET  /accounts/{id}/...
accounts.addHolder|placeHold|setLimits        POST|PUT /accounts/{id}/...
accounts.ownedProducts                        GET  /accounts/{id}/products
transactions.status                           GET  /transactions/{id}/status
transactions.byReference                      GET  /transactions/by-reference/{ref}
transactions.limitQuote                       GET  /transactions/limits/quote
transactions.deposit|withdrawal|cheque        POST /transactions/...
transactions.internalTransfer|neft|rtgs|imps|upi
transactions.reverse                          POST /transactions/{id}/reversals
products.list(query)|products.get             GET  /products...
audit.forTransaction|audit.trace              GET  /audit-events/...
```

Deliberately **not** wired: `transactions.cardPayment` (see §8), `customers.beneficiaries`
(no controller).

## 5. Next up: Phase 2 — Teller operations

**Goal:** one screen with three counter operations — cash deposit, cash withdrawal, cheque
deposit — routed at `tellerOps`.

**Files:** create `src/viewModels/tellerOps.js` + `src/views/tellerOps.html`; modify
`src/services/navigation.js` (route) and `src/appController.js` (`SECTIONS` nav item).

**Decisions already made — do not re-open:**
- Account entry is **resolve-then-confirm** (type a number → `accounts.byNumber`, show the
  resolved account), not a dropdown. Tellers work from a number on a slip, and a dropdown of
  every account is unpageable.
- **No fee field.** No fee schedule exists server-side; a user-entered fee makes the quote
  and the create disagree.
- On 201 the form is **replaced** by a result panel. Leaving the form live invites a second
  submit that silently returns the first transaction.
- Nav: Workspace group, glyph `◧`, permission `TRANSACTION_CREATE`.

**Most likely to go wrong — two things:**

1. The `paymentMethod`/rail pairing and which account field to send. Do not assemble the
   request body by hand: call `txn.bodyFor(entry, form)`, which encodes both.
2. **The create response is a `Transaction` entity, not a `TransactionView`** — its id field
   is `id`, not `transactionId` (§7). The result panel must read `id` from the create
   response and then poll, where `/status` calls the same value `transactionId`. Getting
   this wrong polls `/transactions/undefined/status` and looks like a backend fault.

## 6. Decisions log — APPEND-ONLY

| # | Phase | Decision | Alternative rejected | Why |
|---|---|---|---|---|
| 001 | 1 | Two screens (`tellerOps`, `transfers`) rather than one "New transaction" | Single screen with a rail selector | The field set changes shape per rail, the validated account side flips between deposit and transfer, and 2 of 10 endpoints are unbuildable — one screen would imply a parity the backend does not offer |
| 002 | 1 | `banner.js` + `confirm.js` as ~40-line mixins | A shared `<mb-confirm-dialog>` custom element | No component build step exists here, and sharing one `oj-dialog` by global id across `oj-module` transitions is a real hazard when the outgoing view is still in the DOM |
| 003 | 1 | Each screen keeps its own `<oj-dialog>` with a unique id | One shared dialog element | Same DOM-id hazard; also the dialog bodies genuinely differ (reason-required vs remarks-optional vs full form review) |
| 004 | 1 | All 33 endpoint bindings land in Phase 1 | Spread them across the phases that use them | `endpoints.js` is the first file a handing-over agent reads; a complete commented map is worth more than a minimal diff |
| 005 | 1 | Refactor `approvals.js` in Phase 1, before anything is built on the mixins | Refactor later, or leave it alone | It is the only known-good write flow, so it is the only honest regression test for the mixins |
| 006 | 1 | `pollStatus` treats `PENDING_APPROVAL` as a stop condition | Poll until terminal only | `PENDING_APPROVAL` is non-terminal but stable — a checker may take hours. Polling it burns the budget and looks broken |

## 7. Backend facts the UI depends on

Verified against service source, not documentation. **Where `docs/` disagrees, the code
wins** — the "Docs?" column says whether `docs/` has since been corrected.

| Fact | Proof | Docs? |
|---|---|---|
| `paymentMethod` is derived from rail, never chosen. `method.name() == rail.name()`, except `INTERNAL`→`ACCOUNT` and `CASH`→`CASH` | `TransactionOrchestrator.java:406` | fixed in P1 |
| The validated account flips: DEPOSIT/CHEQUE validate destination; WITHDRAWAL/NEFT/RTGS/IMPS/UPI validate source; only INTERNAL_TRANSFER validates both | `TransactionOrchestrator.java:343-355` | fixed in P1 |
| External-rail destinations (`NEFT/RTGS/IMPS/UPI`) are **unvalidated free text** — do not try to resolve them | same | fixed in P1 |
| `INSUFFICIENT_FUNDS` is checked against `amount + feeAmount` | `TransactionOrchestrator.java:352` | fixed in P1 |
| The limit quote must use the same operating account the server uses, or it silently diverges | `TransactionOrchestrator.java:157` | not documented |
| **Only ONE limit rule is seeded** (RTGS/INR, min 200000, approval `>=` 1000000). Every other rail returns `allowed:true` with all bounds null — so nothing but a large RTGS can reach `PENDING_APPROVAL` | `V1__transaction_domain.sql:240` | fixed in P1 |
| `/card-payments` needs a `cardId` obtainable only from `/internal/v1/cards/...`, which the gateway blocks | `TransactionOrchestrator.java:414` | fixed in P1 |
| account-service declares **no** `Idempotency-Key` on any write | `AccountController.java` | not documented |
| Eligibility requires ACTIVE status **and** VERIFIED KYC **and** age ≥ 18 **and** an Indian resident address. Field is `customerStatus`; render `reasons[]` | `CustomerServiceImpl.java:196` | partially |
| `close` 422s unless ledger balance **and** held amount are both exactly zero | `AccountServicingService.java:129` | not documented |
| Products filter is `?productType=`, not `?type=`. `ProductDetail` exposes `requiresFunding` and `minOpeningDeposit`, so the opening form can validate client-side | `product-service/.../ProductController.java:27` | fixed in P1 |
| `BalanceView.asOf` is `Instant.now()` at read time — a read timestamp, **not** a data-currency marker | `AccountServicingService.java:73` | not documented |
| `ApplicationStatus` has no `PENDING`. An invalid enum value returns **500**, not 400 | `ApplicationStatus.java` | yes |
| Branch-scope asymmetry: `GET /accounts/by-number/{n}` enforces branch access, but `POST /transactions/deposits` does not | `AccountServicingService.java:44` | not documented |
| `manager1` can reverse a BR001 transaction from BR002 because BRANCH_MANAGER holds `TRANSACTION_VIEW_ALL_BRANCHES`; `checker1` cannot | `RequestActor.java` | yes |
| **Create and query return different shapes.** `POST /transactions/*` returns the `Transaction` entity — `id`, `reference`, `type`, `rail`, `channel`, `method`. `GET /{id}/status` and the search endpoints return `TransactionView` — `transactionId`, `transactionReference`. Reading `transactionId` off a create response yields `undefined` and polls `/transactions/undefined/status` | verified live 2026-08-19 | not documented |
| Balance updates flow through an outbox on a ~5s cycle — a balance is never a receipt | `application.yml` `ACCOUNT_OUTBOX_DELAY_MS` | yes |
| Two product services exist. `services/product-service` (`com.moneybags`) is the real one; root `product-service/` (`com.example`) is orphaned and not in the reactor | root `pom.xml` | not documented |

## 8. Known defects and deliberate omissions — APPEND-ONLY

| Item | Where | Status | Why |
|---|---|---|---|
| `/card-payments` unbuildable | `txn.js` `RAILS` | won't build | `cardId` exists only behind gateway-blocked `/internal/**` |
| `PRODUCT_PURCHASE` not in `RAILS` | `txn.js` | deferred | Different request shape; belongs with account opening, not a rail selector |
| `feeAmount` not exposed on any form | P2, P3 | deliberate | No fee schedule server-side; a typed fee diverges quote from create |
| Beneficiaries have no API | `endpoints.js` | backend gap | Tables + entity + repository exist, no controller. External transfers address an account number directly |
| `smoke-test.ps1` money-movement bodies were invalid | `smoke-test.ps1:129+` | **fixed in P1** | Posted `accountId` (not a field) and omitted `@NotNull` `paymentChannel`/`paymentMethod` → 400 before business logic. That whole section only ever appeared to pass |
| Five back-office screens are generic auto-column tables | `ledger`, `products`, `branches`, `notifications`, `audit` | deferred to P7 | Read-only placeholders; flagged as such since before this build |
| Branch-scope asymmetry not "fixed" | P2 picker | deliberate | A BR001 teller can deposit into a BR002 account but cannot look it up. That is the server's behaviour; the UI surfaces the 403 honestly rather than hiding it |
| No tests, no lint, no frontend CI | whole app | out of scope | Explicitly excluded from this build |
| Stale comments | `before_serve.js` (claims `UrlPathParamAdapter`), `tokens.css`/`app.css` (nonexistent SCSS theme), `package.json` (says VDOM/Preact) | cosmetic | Noted so the next reader is not misled |

## 9. Verification log

| Phase | Login(s) | Steps | Expected | Last run | Result |
|---|---|---|---|---|---|
| 1 | `teller1` | `npm install`; `ojet build`; live API contract checks against the running stack (see below) | Build clean; every corrected contract behaves as predicted | 2026-08-19 | **PASS** |
| 1 | `opsadmin` | Sign in; Accounts → open an account; Transactions with each status filter; Approvals | Account detail renders (previously threw); no filter 400s; approvals empty | — | **browser pass still owed** |

**Live contract checks run against the stack on 2026-08-19, all as predicted:**

| Check | Result |
|---|---|
| `limits/quote` RTGS ₹1,00,000 | `allowed:false`, `minAmount:200000`, `approvalThreshold:1000000`, reason "Amount is below the configured minimum" |
| `limits/quote` DEPOSIT ₹5,000 | `allowed:true`, all four bounds `null` — confirms `quoteHasBounds` should suppress the panel |
| Old smoke-test deposit body | **400** — confirms that section only ever appeared to pass |
| Corrected deposit body | **201**, status `PROJECTION_PENDING`, settling to `COMPLETED` |
| Deposit with `paymentMethod: ACCOUNT` on the CASH rail | **400 `INVALID_PAYMENT_METHOD`** — confirms the derivation rule |

Also confirmed: `npm install` clean, `ojet build` exits 0, all changed JS parses,
`smoke-test.ps1` parses.

Still owed for Phase 1: the browser pass (sign in, open account detail, exercise the status
filters). That needs `ojet serve` alongside the backend.

## 10. Running it right now

```bash
./run-all.ps1          # backend: 14 services, health-gated waves
cd frontend && ojet serve
```

App on `http://localhost:8000`, proxying `/api` → `127.0.0.1:8090`. Override the gateway
with `MB_GATEWAY`. **The proxy is not optional** — the gateway ships no CORS config at all.
If `express` or `http-proxy-middleware` is missing, `before_serve.js` warns and silently
skips the proxy, and every `/api` call 404s against the dev server, which looks exactly like
a backend outage.

### Seeded fixtures

| Login | Branch | Use for |
|---|---|---|
| `teller1` | BR001 | maker in every flow |
| `checker1` | BR001 | checker — BR001-scoped only |
| `manager1` | BR002 | reversals (has both `*_VIEW_ALL_BRANCHES`) |
| `opsadmin` | BR001 | lifecycle, holds, everything |

All `Password@123`. Customers: `CIF900101` (VERIFIED), `CIF900102` (PENDING — the denial
path). Accounts: `a0000000-0000-0000-0000-00000000010{1,2,3}` (101/102 are BR001,
103 is BR002). Products: `SAV-REG` `SAV-SENIOR` `CUR-BASIC` `FD-12M` `FD-24M` `RD-12M`.

### The two-window rule

Maker-checker flows need two identities at once. Use **one normal window and one incognito
window** — `sessionStorage` is per-context, but the server also sets an `access-token`
cookie and the gateway caches session resolution for 30 s, so sharing a browser profile
produces a confusing window where the wrong identity appears to work.

### Seeding the approvals queue

Nothing else produces a `PENDING_APPROVAL` transaction — RTGS/INR is the only seeded limit
rule (§7).

1. `teller1` → Teller → deposit **₹15,00,000** into `…-101` (cash has no limit rule).
2. Wait ~10 s for the outbox, reload Account detail, confirm the available balance moved.
3. `teller1` → Transfers → RTGS **₹10,00,000** from `…-101` → the quote shows the approval
   warning, and the create returns `PENDING_APPROVAL`.
4. `checker1` (incognito) → Approvals → the row is there → approve.

### Cheap denial paths (no funding needed)

RTGS ₹1,00,000 (below the ₹2,00,000 minimum) · internal transfer to the same account ·
withdrawal above the available balance · `openAccount` for `CIF900102` (KYC not verified) ·
`close` on any seeded account (balance not settled).
