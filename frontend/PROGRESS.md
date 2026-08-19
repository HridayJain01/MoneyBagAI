# Moneybags console — build progress

Last updated: 2026-08-19 · **Phases 2–6 done; guided write suite documented** · uncommitted · branch `UI-TRIAL`

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
| 2 | Teller operations (deposit, withdrawal, cheque) | **DONE** | — |
| 3 | Transfers (internal, NEFT, RTGS, IMPS, UPI) | **DONE** | — |
| 4 | Account opening + applications queue | **DONE** | — |
| 5 | Account lifecycle, holds, status history | **DONE** | — |
| 6 | Transaction detail, cancel, reversal | **DONE** | — |
| 7 | Consolidation (optional) | **DONE for current scope** | — |

## 3. Screen inventory

| Route | ViewModel | View | Nav group | Route permission | State |
|---|---|---|---|---|---|
| `login` | `login.js` | `login.html` | — | public | built |
| `overview` | `overview.js` | `overview.html` | Workspace | — | built |
| `approvals` | `approvals.js` | `approvals.html` | Workspace | `TRANSACTION_APPROVE` | built |
| `customers` | `customers.js` | `customers.html` | Banking | `CUSTOMER_READ` | built |
| `customerDetail` | `customerDetail.js` | `customerDetail.html` | — (route only) | — | built |
| `accounts` | `accounts.js` | `accounts.html` | Banking | `ACCOUNT_VIEW` | built |
| `accountDetail` | `accountDetail.js` | `accountDetail.html` | — (route only) | — | built + servicing (P5) |
| `transactions` | `transactions.js` | `transactions.html` | Banking | `TRANSACTION_VIEW` | built |
| `ledger` | `ledger.js` | `ledger.html` | Back office | — | **placeholder** |
| `products` | `products.js` | `products.html` | Back office | `PRODUCT_READ` | **placeholder** |
| `branches` | `branches.js` | `branches.html` | Back office | — | **placeholder** |
| `notifications` | `notifications.js` | `notifications.html` | Back office | `NOTIFICATION_MANAGE` | **placeholder** |
| `audit` | `audit.js` | `audit.html` | Back office | `AUDIT_VIEW` | **placeholder** |
| `tellerOps` | `tellerOps.js` | `tellerOps.html` | Workspace | `TRANSACTION_CREATE` | built (P2) |
| `transfers` | `transfers.js` | `transfers.html` | Workspace | `TRANSACTION_CREATE` | built (P3) |
| `openAccount` | `openAccount.js` | `openAccount.html` | — (from Applications) | `ACCOUNT_OPEN` | built (P4) |
| `applications` | `applications.js` | `applications.html` | Workspace | `ACCOUNT_VIEW` | built (P4) |
| `transactionDetail` | `transactionDetail.js` | `transactionDetail.html` | — (route only) | `TRANSACTION_VIEW` | built (P6) |

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

## 5. Current handoff: Phases 4–6 browser regression

Phases 4–6 are implemented and compile. The remaining promotion gate is a signed-in browser
pass across the new routes. Do not rebuild these screens from scratch.

**Phase 4 files:** `src/viewModels/openAccount.js`, `src/views/openAccount.html`,
`src/viewModels/applications.js`, `src/views/applications.html`, plus routes and the
Applications Workspace nav entry.

**Phase 4 implemented flow:**

1. Maker enters CIF → `customers.eligibility(cif)`; render `reasons[]` and stop when denied.
2. Load active products with `products.list({status:'ACTIVE'})` and use `productCode`.
3. Fetch chosen product detail so `requiresFunding` and `minOpeningDeposit` can be explained
   before creation.
4. `accounts.createApplication(...)`; display the application reference and status.
5. Checker queue uses status `PENDING_APPROVAL` — never `PENDING`.
6. Approve/reject through the shared confirmation mixin. Reject reason is required; the
   maker cannot approve their own application.
7. On approval, navigate with `createdAccountId`. Funding-required products produce
   `PENDING_ACTIVATION`, not `ACTIVE`.

**Phase 5:** `accountDetail` now loads holders, active holds, status history, limits and
owned products. `ACCOUNT_STATUS_MANAGE` gates lifecycle, joint-holder, hold and limit
writes. Every mutation uses the shared confirmation discipline and refreshes live data.

**Phase 6:** Transactions and account history now open `transactionDetail`. The deep view
renders legs, funds hold, journal lines, clearing, rail details and status history. Cancel
is limited to cancellable states plus maker/supervisor authority. Reversal is shown only
for completed non-reversal transactions and navigates to the new compensating record.

**Browser fixtures:** `CIF900101` is eligible; `CIF900102` proves denial. Seed account
`a0000000-0000-0000-0000-000000000101` has holder/hold/limits/history/product data.
Use `teller1` then `checker1` for a real maker-checker application, or `opsadmin` for a
read-only route pass.

## 6. Decisions log — APPEND-ONLY

| # | Phase | Decision | Alternative rejected | Why |
|---|---|---|---|---|
| 001 | 1 | Two screens (`tellerOps`, `transfers`) rather than one "New transaction" | Single screen with a rail selector | The field set changes shape per rail, the validated account side flips between deposit and transfer, and 2 of 10 endpoints are unbuildable — one screen would imply a parity the backend does not offer |
| 002 | 1 | `banner.js` + `confirm.js` as ~40-line mixins | A shared `<mb-confirm-dialog>` custom element | No component build step exists here, and sharing one `oj-dialog` by global id across `oj-module` transitions is a real hazard when the outgoing view is still in the DOM |
| 003 | 1 | Each screen keeps its own `<oj-dialog>` with a unique id | One shared dialog element | Same DOM-id hazard; also the dialog bodies genuinely differ (reason-required vs remarks-optional vs full form review) |
| 004 | 1 | All 33 endpoint bindings land in Phase 1 | Spread them across the phases that use them | `endpoints.js` is the first file a handing-over agent reads; a complete commented map is worth more than a minimal diff |
| 005 | 1 | Refactor `approvals.js` in Phase 1, before anything is built on the mixins | Refactor later, or leave it alone | It is the only known-good write flow, so it is the only honest regression test for the mixins |
| 006 | 1 | `pollStatus` treats `PENDING_APPROVAL` as a stop condition | Poll until terminal only | `PENDING_APPROVAL` is non-terminal but stable — a checker may take hours. Polling it burns the budget and looks broken |
| 007 | 2 | Counter account entry is resolve-then-confirm by account number | Load every account into a dropdown | The teller works from a slip and the account catalogue is unpageable; the resolved name/status is the safety check |
| 008 | 2 | Replace the form with a receipt panel after a 201 | Leave the live form below a success banner | A live form invites an accidental second submit and makes idempotent replay look like a second success |
| 009 | 3 | Resolve internal destinations only; external destinations remain free text | Look up every destination in account-service | The orchestrator validates external rails on the source side only; an external account cannot exist in Moneybags |
| 010 | 2–3 | Dialog footer actions use native Knockout-bound buttons | `oj-button` action listeners inside the slotted legacy dialog footer | Browser testing showed the slotted action event was swallowed; native buttons keep the click handler and disabled state explicit |
| 011 | 4 | Applications is the navigable workspace; Open account is reached from its primary CTA | Put two adjacent account-opening items in the nav | One queue serves both maker and checker, while the form is one action from that queue and can also receive a CIF route parameter |
| 012 | 4 | Re-fetch the selected product detail before review | Trust the product list row indefinitely | Funding rules are financial terms; a fresh detail read keeps the visible minimum and `requiresFunding` flag current |
| 013 | 5 | Extend Account detail with servicing evidence and controls | Create six shallow servicing routes | Holders, holds, limits, products and status changes all answer questions about the same account and need its balance/status context |
| 014 | 5 | Gate every servicing write on `ACCOUNT_STATUS_MANAGE` | Invent narrower client permissions | The backend uses this one literal permission for lifecycle, holders, holds and limits; the UI must mirror the real authority model |
| 015 | 6 | Render the full transaction evidence graph on one detail route | Separate legs/journals/clearing pages | These records explain one transaction and are already returned atomically by the deep endpoint |
| 016 | 6 | Reversal navigates to the new compensating transaction | Patch the original row into a reversed-looking result | The backend creates a new linked transaction and deliberately preserves the original record |

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
| Overview fires four protected calls before login | `overview.js` | **fixed 2026-08-19** | ModuleRouterAdapter instantiates the route before the auth-gated module enters the DOM; loads now wait for an authenticated session |
| Transaction status selector renders only an empty option | `transactions.html` | **fixed 2026-08-19** | JET's expression parser cannot parse regex literals; `replace(/_/g, ' ')` is now parser-safe `split('_').join(' ')` |
| Customer completeness headline shows `7%` for a complete profile | `customerDetail.js` | **fixed 2026-08-19** | The view took the first numeric map value (`completedFields`) instead of the explicit `percentage` field |
| Confirmation dialog footer actions do not fire reliably | `support/confirm.js`, three dialog views | **fixed in source; final reload regression pending** | Legacy dialog bridge plus slotted `oj-button` events were unreliable; widget open/close plus native KO-bound footer buttons are used now |
| Referenced build plan is absent | `.claude/plans/peaceful-dancing-stardust.md` | documentation gap | `PROGRESS.md` referenced it, but `.claude/` is not present in this checkout; this file is now the authoritative continuation plan |
| Application search has no explicit permission check in account-service | `AccountApplicationService.search` | backend gap, mirrored | The service branch-scopes the result but calls no `actor.require`; the UI uses `ACCOUNT_VIEW` as the conservative route/nav gate and still hides decision actions without `ACCOUNT_APPROVE` |
| Applications “New application” did nothing | `views/applications.html` | **fixed 2026-08-19** | Live browser testing proved this JET action listener was swallowed; the primary CTA now uses the same native Knockout-bound button pattern as dialog actions |
| Account servicing stopped after the first lifecycle button | `viewModels/accountDetail.js` | **fixed 2026-08-19** | JET treats a missing object property as an undefined-variable binding error; every lifecycle action now carries an explicit `danger` boolean |

## 9. Verification log

| Phase | Login(s) | Steps | Expected | Last run | Result |
|---|---|---|---|---|---|
| 1 | `teller1` | `npm install`; `ojet build`; live API contract checks against the running stack (see below) | Build clean; every corrected contract behaves as predicted | 2026-08-19 | **PASS** |
| 1 | `opsadmin` | Sign in; Overview; Accounts → open an account; Transactions status options; Approvals | Overview refetches after login; account detail renders; all 14 enum values render; approvals empty | 2026-08-19 | **PASS** |
| 2 | `opsadmin` | Teller → each operation; resolve `510000000101`; enter amount; review; cancel | Correct account side, cheque requirement, resolved card, limit check and confirmation | 2026-08-19 | **PASS through confirmation; final submit/result/poll pending** |
| 3 | `opsadmin` | Internal transfer 101→102; RTGS ₹1,00,000; RTGS ₹10,00,000 | Internal confirmation; minimum refusal at ₹2,00,000; approval warning at threshold | 2026-08-19 | **PASS through confirmation; final submit/result/poll pending** |
| regression | `opsadmin` | Customers search/detail/tabs; Accounts list/detail; Ledger; Products; Branches; Notifications; Audit | Every built read route renders live data or an honest empty state with no console errors | 2026-08-19 | **PASS** |
| 4 | API contract (`opsadmin`) | Active products; eligible/ineligible CIF; applications envelope | Six products; `CIF900101=true`; `CIF900102=false`; queue returns envelope | 2026-08-19 | **PASS (API/build); browser pending** |
| 5 | API contract (`opsadmin`) | Account holders, active holds, limits, status history, owned products | All five endpoint shapes match the servicing view model | 2026-08-19 | **PASS (API/build); browser pending** |
| 6 | API contract (`opsadmin`) | Transaction search then deep detail | Legs, journals and history arrays present; route modules compile | 2026-08-19 | **PASS (API/build); browser pending** |
| 4 | `opsadmin` browser | Applications; New application; both eligibility fixtures; six products; SAV/FD terms; FD minimum; review/cancel | Queue/statuses render; CTA routes; eligibility and funding rules match contract; confirmation closes without a write | 2026-08-19 | **PASS** |
| 5 | `opsadmin` browser | Account 101; lifecycle/holder/hold/limit dialogs; servicing evidence | Details render with holders, holds, limits, history and products; all four write dialogs open and cancel cleanly | 2026-08-19 | **PASS** |
| 6 | `opsadmin` browser | Transactions; open completed internal transfer; inspect legs/hold/journals/rail/history; reversal review | Deep evidence renders with no console error; reversal warns that it creates a compensating transaction | 2026-08-19 | **PASS** |
| regression | `opsadmin` browser | Teller review; internal transfer; RTGS below minimum and at approval threshold; every nav route | Resolve/review dialogs and both RTGS branches correct; all nav routes load with no console errors | 2026-08-19 | **PASS** |

### Checkpoint — 2026-08-19, uncommitted working tree

**Delivered in this checkpoint:**

- Fixed the JET expression-parser status dropdown failure.
- Fixed Overview's pre-auth 401/stale-first-render bug.
- Fixed customer completeness (`100%`, not `7%`) and the doubled `CIF` label.
- Built Phase 2 Teller: deposit, withdrawal, cheque; resolve/review/confirm/result/poll.
- Built Phase 3 Transfers: internal, NEFT, RTGS, IMPS, UPI; correct destination handling,
  limit rendering, approval messaging, confirm/result/poll.
- Reworked shared confirmation dialog control after live browser testing found the legacy
  custom-element method/event path unreliable.

**Build evidence:** `node --check` on the new/changed view models passes; `npm run build`
has completed successfully after each material change. The Sass warning is pre-existing and
expected because this app uses plain CSS.

**Browser evidence:** Overview, Customers (all tabs), Accounts, Account detail, Transactions,
Approvals, Teller variants, internal transfer review, both RTGS limit branches, and all five
generic back-office routes were exercised against the live stack. Browser console stayed
clean during the completed pass.

**Do first when resuming:**

1. Reload the app once so the latest native dialog-footer build is served.
2. Re-run Teller review → **Go back** and Transfers review → **Go back**; confirm the modal
   closes. This is the only fix in this checkpoint not yet browser-regressed after rebuild.
3. With explicit approval for the browser-side financial action, submit a ₹1 deposit to
   `510000000101`; confirm the result panel reads create response `id`/`reference` and polling
   reaches `COMPLETED`.
4. Submit an internal ₹1 transfer 101→102 and verify the same result/poll path.
5. If those pass, mark Phases 2 and 3 DONE and begin Phase 4 from §5.

### Checkpoint — 2026-08-19, user verification

The user completed the final live workflow check and confirmed the Phase 2–3 paths are
working. Teller and Transfers are therefore promoted from “built” to **DONE**. Continue
with Phase 4; do not repeat the Phase 2–3 submit checks unless a later shared-layer change
touches `txn.js`, `confirm.js`, `http.js`, or their views.

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

### Checkpoint — 2026-08-19, Phases 4–6 implementation

**Delivered:**

- Phase 4: eligibility-first account opening, active product selection, fresh product-rule
  resolution, minimum-funding validation, retry-safe creation result, and a branch-scoped
  application queue with approve/reject/cancel and maker–checker affordances.
- Phase 5: account lifecycle controls plus holders, active holds, limits, status history,
  and owned products on account detail. All writes are permission-gated and confirmed.
- Phase 6: transaction-detail routing from both transaction tables; deep evidence for
  legs/holds/journals/clearing/rail/history; permission- and state-aware cancel/reversal.
- Consolidation: parser-safe status labels, native dialog actions, new route permissions,
  and shared formatting/confirmation patterns preserved.

**Evidence:** all changed view models pass `node --check`; `npm run build` completes. The
live proxy returned the expected Phase 4–6 payload shapes: six active products,
eligibility true/false for the two fixtures, account servicing arrays/limits, 15 current
transactions, and a deep transaction with legs/journals/history. The dev server was
restarted on `:8000` after the build cycle.

**Remaining promotion gate:** sign into the browser and exercise the new routes. Browser
policy requires action-time confirmation before entering the seeded password. For a
non-mutating pass, use `opsadmin` and stop at every confirmation dialog. For a real
maker-checker application, use separate `teller1`/`checker1` contexts as described below.

### Checkpoint — 2026-08-19, Phases 4–6 browser-complete

The signed-in `opsadmin` browser pass is complete. Two defects found only in the browser
were fixed and re-tested: the Applications primary CTA now navigates, and explicit
`danger:false` values prevent JET from aborting Account servicing bindings.

Verified without committing new financial/account mutations:

- both account-opening eligibility branches, all six products, current product terms,
  `FD-12M` minimum enforcement (`4999` disabled, `5000` enabled), and confirmation copy;
- application status selector and empty queue;
- account header/balance/facts/transactions plus holders, holds, limits, status history,
  owned products, and all servicing confirmation forms;
- full completed-transaction evidence: two legs, consumed hold, two balanced journals,
  rail details, status history, and reversal review;
- Teller resolve/review, internal-transfer resolve/review, RTGS minimum refusal and the
  `₹10,00,000` approval threshold;
- Overview, Approvals, Customers, Ledger, Products, Branches, Notifications and Audit all
  load with the correct page title/heading and no browser console errors.

`frontend/TEST-FLOW.md` is the authoritative manual flow for the remaining deliberate
write tests. It separates the safe smoke pass from account creation, financial posting,
cancellation/reversal and persistent account-servicing mutations, and records the exact
fixtures and expected outcomes.

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
