# Moneybags console — build progress

Last updated: 2026-08-19 · **Phase 8B customer + organization administration browser-complete** · uncommitted · branch `UI-TRIAL`

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
| 2 | Teller operations (cash deposit and withdrawal only) | **DONE** | — |
| 3 | Moneybags-to-Moneybags internal transfer only | **DONE** | — |
| 4 | Account opening + applications queue | **DONE** | — |
| 5 | Account lifecycle, holds, status history | **DONE** | — |
| 6 | Transaction detail, cancel, reversal | **DONE** | — |
| 7 | Purpose-built Back Office explorers | **DONE** | — |
| 8 | Administration writes | **IN PROGRESS — 8A product lifecycle and 8B customer/branch/employee/role administration built; notification/reporting/ledger work remains** | — |

## 3. Screen inventory

| Route | ViewModel | View | Nav group | Route permission | State |
|---|---|---|---|---|---|
| `login` | `login.js` | `login.html` | — | public | built |
| `overview` | `overview.js` | `overview.html` | Workspace | — | built |
| `approvals` | `approvals.js` | `approvals.html` | Workspace | `TRANSACTION_APPROVE` | built |
| `customers` | `customers.js` | `customers.html` | Banking | `CUSTOMER_READ` | built |
| `customerDetail` | `customerDetail.js` | `customerDetail.html` | — (route only) | `CUSTOMER_READ` | built |
| `accounts` | `accounts.js` | `accounts.html` | Banking | `ACCOUNT_VIEW` | built |
| `accountDetail` | `accountDetail.js` | `accountDetail.html` | — (route only) | `ACCOUNT_VIEW` | built + servicing (P5) |
| `transactions` | `transactions.js` | `transactions.html` | Banking | `TRANSACTION_VIEW` | built |
| `ledger` | `ledger.js` | `ledger.html` | Back office | `OPS_ADMIN` role | built (P7 explorer) |
| `products` | `products.js` | `products.html` | Back office | `PRODUCT_READ` | built (P7 explorer) |
| `branches` | `branches.js` | `branches.html` | Back office | `BRANCH_MANAGER` or `OPS_ADMIN` role | built + administration (P8B) |
| `notifications` | `notifications.js` | `notifications.html` | Back office | `NOTIFICATION_MANAGE` | built (P7 explorer) |
| `audit` | `audit.js` | `audit.html` | Back office | `AUDIT_VIEW` | built (P7 explorer) |
| `tellerOps` | `tellerOps.js` | `tellerOps.html` | Workspace | `TRANSACTION_CREATE` + `TELLER` role | built (P2) |
| `transfers` | `transfers.js` | `transfers.html` | Workspace | `TRANSACTION_CREATE` + `TELLER` role | built (P3) |
| `openAccount` | `openAccount.js` | `openAccount.html` | — (from Applications) | `ACCOUNT_OPEN` | built (P4) |
| `applications` | `applications.js` | `applications.html` | Workspace | `ACCOUNT_VIEW` | built (P4) |
| `transactionDetail` | `transactionDetail.js` | `transactionDetail.html` | — (route only) | `TRANSACTION_VIEW` | built (P6) |

The Back Office routes are deliberately read-only in Phase 7, but no longer generic:
each exposes the service's domain evidence and supported filters. Administrative writes
are separated into Phase 8 so creation, activation, retry and destructive actions receive
their own confirmation, permission and regression work.

## 4. What Phase 1 delivered

Nothing user-visible except two bug fixes. Its purpose is to make Phases 2–6 pure
"add a screen" diffs.

### Created

| File | What |
|---|---|
| `src/services/txn.js` | Money-movement domain module: UI catalogue for cash deposit, cash withdrawal and internal transfer, plus `bodyFor`, `quote`, `quoteAccountFor`, `quoteHasBounds`, `pollStatus`, `isTerminal`, `awaitsHuman`. External/instrument rails are intentionally not offered |
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

The NEFT/RTGS/IMPS/UPI/cheque endpoint bindings remain as backend contract helpers only;
no current route presents them. Do not infer UI availability from this historical wiring list.

Deliberately **not** wired: `transactions.cardPayment` (see §8), `customers.beneficiaries`
(no controller).

## 5. Current handoff: continue Phase 8 after the Phase 8B checkpoint

Phase 7 explorers and Phase 8A/8B administration are promoted. Continue with notification
administration, statement/reporting workflows, then manual ledger work. Preserve the role
matrix and internal-only money-movement boundary recorded in the newest checkpoint below.

**Delivered:**

1. Ledger — GL balance summary; transaction/customer-account filters; balanced journal
   inspection down to debit/credit lines.
2. Products — type/status filters; opening terms; funding rules; charges and product rules.
3. Branches — network/staff summary; branch details; assigned employees; working hours;
   branch holidays.
4. Notifications — status/CIF/recipient filters; delivery details; empty-queue handling;
   template catalogue.
5. Audit trail — service/event/aggregate/branch filters; exact correlation-ID trace mode;
   HTTP/actor context and formatted payload inspection.

`genericList.js` and the inline `#mbGenericList` template were removed because no routed
screen uses dynamic columns now. New endpoint bindings are limited to read operations:
branch hours, branch holidays and notification templates.

**Browser evidence:** Section F in `frontend/TEST-FLOW.md` passed as `opsadmin`: journal,
product, branch and audit Inspect/Close actions; all supported filter/clear paths; empty
notification state plus four templates; aggregate and trace audit modes; and a clean
post-login browser console. One row-binding defect was found and fixed during this gate.

**Phase 8A built:** `PRODUCT_MANAGE` now exposes New product, Edit terms and
Activate/Deactivate controls on the existing Products explorer. Create validates every
required commercial term before posting. Edit sends only the public mutable fields and is
labelled “Save new version” because product history is immutable. Inspect also loads and
renders `/products/{code}/versions`. Lifecycle copy explicitly says existing accounts are
not changed.

**Promotion remaining:** the New product dialog rendered with all fields and its empty-form
validation produced “Product name is required” without a request. During the resumed pass,
the 608 px in-app viewport exposed a real shell defect: the sticky single-column nav covered
Products and intercepted its buttons. The mobile shell now uses a non-sticky horizontal,
scrollable nav and the production build passes. A fresh live retest is still required.

The backend cannot currently restart because Java's selector probe fails in Windows with
`Unable to establish loopback connection` / `UnixDomainSockets.connect0: Invalid argument`.
Run `netsh winsock reset` from an elevated terminal and reboot, then run `run-all.ps1
-SkipBuild`, start the frontend, sign in as `opsadmin`, and complete Section H. Do not
submit Create, Save new version, Activate or Deactivate during that safe pass.

**After 8A promotion:** continue with branch/staff administration, then notification
queue/retry/template creation, statement/reporting screens, and manual ledger posting last.
Treat ledger posting, retry and deletion as separate confirmation-heavy workflows rather
than adding casual buttons to the Phase 7 explorers.

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
| 017 | 7 | Give every Back Office service an explicit explorer | Keep one dynamic auto-column table | Nested journal lines, product rules and audit payloads are the useful evidence; runtime-derived columns hid them |
| 018 | 7 | Keep Phase 7 read-only | Mix administration writes into the explorer pass | Product, branch, notification and ledger writes have different permissions and risk; they need dedicated confirmation and regression work |
| 019 | 7 | Correlation ID switches Audit into trace mode | Add correlation ID to ordinary event filters | The search controller does not accept correlation ID; the dedicated trace endpoint is the authoritative cross-service view |
| 020 | 7 | Bind row actions directly on decorated rows | Reach back through `$parent` from each table row | Live JET module contexts rendered the buttons but swallowed the parent-method binding without an error; row-owned actions are explicit and browser-proven |
| 021 | 8A | Product edits create visible version evidence | Present Edit as an in-place overwrite | The service records immutable versions; the UI labels the commit “Save new version” and reloads history after success |
| 022 | 8A | Product lifecycle copy protects existing-account expectations | Say only Activate/Deactivate | Deactivation stops new sales but deliberately leaves existing accounts unchanged; the confirmation states that consequence |
| 023 | 8A | Use a horizontal, non-sticky nav below 900 px | Keep the desktop rail sticky in a one-column shell | At narrow widths the sticky full-height rail remained over later grid rows and intercepted page actions; horizontal overflow keeps every route reachable without covering content |

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
| Five Back Office screens were generic auto-column tables | `ledger`, `products`, `branches`, `notifications`, `audit` | **replaced in P7, 2026-08-19** | Each route now exposes domain filters, summaries and nested evidence; the unused generic view model/template were removed |
| Back Office row Inspect buttons rendered but did nothing | five P7 explorer views | **fixed 2026-08-19** | `$parent.inspect…` did not resolve reliably through the JET/Knockout module bridge; each decorated row now owns stable `inspect` and `closeDetail` actions |
| Narrow shell navigation covers page controls | `styles/layout.css` at `max-width: 900px` | **fixed in source 2026-08-19; live retest pending** | A sticky 100vh nav in the single-column shell stayed over the main row; the responsive nav is now horizontal, non-sticky and scrollable |
| Local backend cannot restart in this Windows boot | host Winsock / Java NIO selector | **environment blocked; reboot required** | `run-all.ps1 -SkipBuild` stops at its selector probe with `UnixDomainSockets.connect0: Invalid argument`; the existing launcher already prescribes elevated `netsh winsock reset`, then reboot |

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
| 7 | API contract (`opsadmin`) | GL accounts/journals; products; branches/employees/hours/holidays; notification queue/templates; audit page | All five screen models match live response fields and envelopes | 2026-08-19 | **PASS** |
| 7 | build | `node --check` on five view models and endpoint/format helpers; `npm run build`; `git diff --check` | Parse and production bundle complete without source errors | 2026-08-19 | **PASS** |
| 7 | `opsadmin` browser | Section F of `TEST-FLOW.md` | All five purpose-built explorers render, filter and inspect details with a clean console | 2026-08-19 | **PASS** |
| 8A | API contract/build | FD-12M version history; five product lifecycle bindings; JS parse/build | Version response matches immutable history model; source compiles and bundles | 2026-08-19 | **PASS** |
| 8A | `opsadmin` browser | Section H of `TEST-FLOW.md` | Create validation; version history; prefilled edit; lifecycle confirmation; no write committed | 2026-08-19 | **PARTIAL — create form/validation pass; narrow-nav interception fixed; final live retest blocked by host restart** |

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

### Checkpoint — 2026-08-19, Phase 7 Back Office implementation

**Delivered:** the five Back Office placeholders are now purpose-built read-only explorers.
Ledger exposes GL balances and balanced journal lines; Products exposes commercial terms,
charges and rules; Branches combines the directory with assigned employees, working hours
and holidays; Notifications combines delivery monitoring with the template catalogue; Audit
supports normal event search and exact correlation tracing with payload inspection.

**Contract evidence:** live `opsadmin` reads returned six GL accounts, posted balanced
journals with lines, six detailed products, two branches/four employees, seven operating
days and two holidays for BR001, four notification templates with an empty delivery queue,
and a paged audit stream. The branch service uses `isClosed: 'Y'|'N'` and holiday
`description`; both were verified from live payloads rather than inferred from docs.

**Source/build evidence:** all five new view models parse; `endpoints.js` and `format.js`
parse; `git diff --check` is clean; `npm run build` succeeds. A transient Windows `EBUSY`
against a generated Redwood image cleared after the stale dev-server listener exited; the
source was not implicated, and the subsequent clean build passed. The dev server was
restarted and returns HTTP 200 on `:8000`.

**Remaining promotion gate:** the browser tab has no authenticated session. Sign in as
`opsadmin`, then run Section F in `frontend/TEST-FLOW.md`. Do not mark Phase 7 DONE until
the five Inspect actions, filters, empty states and browser console have been checked.

**Next phase after promotion:** Phase 8 Back Office administration. Implement product and
branch/staff administration first; notification creation/retry next; statement/reporting
screens next; manual ledger posting last because it is a direct accounting mutation.

### Checkpoint — 2026-08-19, Phase 7 browser-complete

The authenticated `opsadmin` promotion pass is complete. The browser found one silent
interaction defect: all five explorer tables displayed Inspect buttons, but `$parent`
method bindings did not change the selected-detail observable. Each decorated row now owns
its `inspect` and `closeDetail` actions; Ledger, Products, Branches and Audit were reloaded
and their detail panels opened successfully after the fix. Notifications currently has no
delivery rows, so its row detail remains fixture-dependent; its filters, empty state and
four-template catalogue passed.

**Focused evidence:** Ledger showed six GL accounts and eight journals; transaction filter
returned one, clear returned eight and account `…0101` returned five. A journal showed two
lines and BALANCED. Product type TERM_DEPOSIT returned FD-12M/FD-24M; FD-12M and SAV-REG
terms, charges and rules matched the service. BR001 showed three assigned employees, all
seven working days and both holidays; BR002 showed only EMP-003. Audit showed 17 events,
formatted JSON detail, exact aggregate results and an honest empty Trace mode for a missing
correlation ID.

**Regression evidence:** Overview, Teller, Transfers, Approvals, Applications, Customers,
Accounts, Transactions, Open account and all five Back Office routes loaded with the
expected route title and H1. The only console error in the retained log predates sign-in
and is the already-recorded deep-link permission guard; no error was emitted by the Phase 7
pass. A viewport screenshot confirmed the new filters, summary cards and tables use the
existing navigation, typography, spacing, card and button language.

**Next action:** begin Phase 8 with product administration and branch/staff administration.
Keep the existing explorer routes as the read surface and add permission-gated edit flows
with explicit confirmation rather than replacing their operational evidence.

### Checkpoint — 2026-08-19, Phase 8A Product administration built

Product administration is implemented without disturbing the Phase 7 catalogue. Ops users
with `PRODUCT_MANAGE` get create, edit and lifecycle controls; read-only users keep the
same explorer. The shared confirmation discipline supplies one retry-stable intent, native
footer buttons avoid the known slotted-action defect, and all success paths refresh both
catalogue state and immutable version history.

**Created/updated surface:** endpoint bindings for versions/create/update/activate/
deactivate; permission and confirmation support in `products.js`; create/edit/lifecycle
dialog plus version table in `products.html`; responsive two-column administration form in
`layout.css`.

**Evidence so far:** the live FD-12M history endpoint returns version 1 ACTIVE. The browser
rendered all create fields, PRODUCT_MANAGE gating and the validation message without
submitting. The remaining safe browser checks are recorded in Section H. No product was
created, edited, activated or deactivated in this checkpoint.

### Checkpoint — 2026-08-19, Phase 8A promotion pause

The resumed `opsadmin` pass confirmed six active catalogue rows and management-control
visibility. It also isolated a responsive-shell defect rather than a product-binding bug:
at the in-app browser's 608 px width, hit testing showed New product underneath the sticky
navigation (`elementFromPoint` returned the Banking nav section). The `max-width: 900px`
shell now renders navigation as a horizontal, non-sticky strip; `npm run build` passes.

No product write was submitted. Version history, prefilled Edit terms, Deactivate/Go back
and the final console check remain at Section H steps 3–7. They could not be repeated after
the build because all backend listeners were already down and Java's NIO selector preflight
now exposes a broken Windows AF_UNIX/Winsock provider. Frontend `:8000` can start, but the
gateway `:8090` cannot until the host is reset and rebooted. Resume here after recovery;
do not revisit completed Phases 1–7.

### Checkpoint — 2026-08-19, Phase 8B customer and organization administration

The host was recovered and the full stack was verified healthy. This checkpoint replaces
the promotion pause above as the current handoff.

**Role and rail boundary:** Teller and Internal transfers are visible and routable only for
`TELLER`; checker, branch-manager and ops sessions cannot retain those pages through a
stale URL. A successful login reloads the authorized route once so a module created for a
previous identity cannot retain its permission snapshot. `CUSTOMER` identities are rejected
from this employee console with a clear message. Cash deposit, cash withdrawal and internal
Moneybags transfer are the only create rails shown; cheque, card, NEFT, RTGS, IMPS and UPI
are deliberately absent from the UI.

**Customer onboarding:** Customers now has a New customer flow that creates the CUSTOMER
identity, customer/CIF and current residential address, with validation and partial-failure
guidance. A live end-to-end test created `CIF607FAE22DE86` / `Codex Customer` with KYC
`PENDING`; its customer login was then refused by the employee-console boundary as expected.
No account or financial transaction was created for this test identity.

**Branch, employee and access administration:** Opsadmin can create/edit/status branches,
replace weekly hours, add/remove holidays, create staff identity + employee records, edit
employment/manager/role/status, transfer branch, replace approval authority, and create/edit
roles with permission mappings. Branch managers receive the branch/employee directory only;
write buttons and Roles & access are absent. Employee edit excludes the selected employee
from manager choices. Employee transfer now also updates the identity-service branch so the
next login carries the correct branch.

**Server boundary:** branch and employee mutations now require trusted `BRANCH_MANAGE` and
`EMPLOYEE_MANAGE` headers inside branch-employee-service; the gateway is no longer the only
enforcement point. The rebuilt service is live on `:8081`; teller probes against nonexistent
branch/employee IDs returned `403 PERMISSION_DENIED` before domain lookup. Spring tests pass.

**Browser evidence:** all four seeded employee logins passed. Teller sees the two cash
operations and one internal-transfer flow; checker sees Approvals but no teller routes;
manager sees read-only Branches/Employees; ops sees the full administration tabs and forms.
Branch/customer empty-submit validation, employee create/edit, and role-permission forms were
exercised without unintended writes. The one intended customer fixture write is recorded
above. The frontend production build and changed JavaScript syntax checks pass.

**Next work:** Phase 8C notification queue/retry/template administration, then statement and
reporting screens, then manual ledger posting/reversal last. Product charge/rule editing is
also still separate from the completed product lifecycle surface.

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

### Approvals fixture note

The employee UI no longer exposes RTGS or any other external rail, so it cannot seed a new
`PENDING_APPROVAL` transaction. Approvals can still inspect/act on existing rows created by
backend fixtures or API-level tests. Do not re-add an external transfer control merely to
create a checker fixture.

### Cheap denial paths (no funding needed)

internal transfer to the same account · withdrawal above the available balance ·
`openAccount` for `CIF900102` (KYC not verified) ·
`close` on any seeded account (balance not settled).
