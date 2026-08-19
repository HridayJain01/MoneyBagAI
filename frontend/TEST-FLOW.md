# Moneybags console — end-to-end test flow

Last verified: 2026-08-19. App: `http://localhost:8000`. Phase 8B role, customer and organization administration promotion passed. The test tab is left signed out.

This guide starts with a safe read/review pass, then lists the workflows that mutate the
local demo databases. Use it after `run-all.ps1` and `npm run serve` are healthy.

## Test identities and fixtures

All seeded users use `Password@123`.

| User | Role | Branch | Best used for |
|---|---|---|---|
| `opsadmin` | OPS_ADMIN | BR001 | Every read screen, servicing controls, reversal review |
| `teller1` | TELLER | BR001 | Teller/transfer maker and account-application maker |
| `checker1` | CHECKER | BR001 | Transaction and account-application checker |
| `manager1` | BRANCH_MANAGER | BR002 | Cross-branch reads and reversal permission |

| Fixture | Meaning |
|---|---|
| `CIF900101` | Eligible: ACTIVE, KYC VERIFIED, adult, resident address present |
| `CIF900102` | Ineligible: KYC PENDING; expected reason `KYC_NOT_VERIFIED` |
| `510000000101` | BR001 savings source/destination account |
| `510000000102` | BR001 current source/destination account |
| `520000000103` | BR002 account for branch-scope checks |

For maker–checker tests, use two browser contexts. `sessionStorage` is per context, while
the gateway also uses a cookie/cache, so one shared profile can briefly show the wrong
identity.

## A. Safe smoke pass — no writes committed

Sign in as `opsadmin` and stop at every confirmation dialog.

1. **Overview** — expect the four counters, recent activity, and queue-by-rail section.
2. **Customers** — search `CIF900101`, open it, and visit each detail tab. Completeness
   should be `100%`, and the CIF label should appear once.
3. **Accounts** — all three fixtures appear. The status selector has ACTIVE, FROZEN,
   BLOCKED, DORMANT, CLOSED.
4. Open account `…0101`. Expect balance, facts, trend, mini statement, transactions, then:
   - lifecycle buttons: Freeze, Block, Mark dormant, Close account;
   - one PRIMARY holder;
   - active holds (or an honest empty state);
   - account limits with values and updated/default text;
   - status history and owned products.
5. Open each servicing dialog and choose **Go back**:
   - Freeze — optional reason;
   - Add joint holder — CIF and role;
   - Place hold — amount, reason, MANUAL/LIEN;
   - Set limits — per-transaction and daily-withdrawal values.
6. **Transactions** — confirm all 14 real statuses render (no `PENDING` option). Open a
   COMPLETED row. Expect legs, funds hold when applicable, journals with balanced lines,
   clearing/rail details, and complete status history. Open Reverse and choose **Go back**.
7. **Approvals** — the empty queue is valid unless a large RTGS has been submitted.
8. **Applications** — all seven application statuses render. New application must navigate
   to Open account.
9. **Open account**:
   - check `CIF900102`: expect `KYC_NOT_VERIFIED` and no product form;
   - check `CIF900101`: expect Passed and six active products;
   - choose `SAV-REG`: funding is optional;
   - choose `FD-12M`: Review is disabled below `₹5,000` and enabled at `₹5,000`;
   - open the confirmation and choose **Go back**.
10. Run the role/rail pass in Section I as `teller1`, `checker1` and `manager1`.
11. Run the purpose-built Back Office pass in Section F. Each route must render live domain
    evidence or an honest empty state, with no red load banner or console error.

## B. Account application — creates an account

This mutates the demo database.

1. As `teller1`, Applications → New application.
2. Enter `CIF900101`, check eligibility, choose `SAV-REG`, optionally name it, then Create
   application. Record the `APP-…` reference; status should be `PENDING_APPROVAL`.
3. Still as `teller1`, the queue should label that row **Made by you** and offer Cancel,
   never Approve.
4. In a separate context as `checker1`, open Applications. Approve the recorded reference.
   The success banner should include `createdAccountId`.
5. Find the new account in Accounts. A non-funded savings account starts ACTIVE.

Funded variant: repeat with `FD-12M` and `₹5,000`. Approval creates the funded account and
its opening-funding workflow; the UI explains that it remains pending activation until
funding completes.

Rejection variant: create another application as teller, reject as checker, and verify the
required reason plus REJECTED filter. Cancellation variant: create as teller and cancel it
from the maker context; verify CANCELLED.

## C. Teller and transfer writes — moves demo money

1. As `teller1`, post a Cash deposit of `₹1` to `510000000101`.
2. The receipt replaces the form. Record `TXN-…`; polling should reach COMPLETED.
3. Post an internal transfer of `₹1` from `101` to `102`; expect the same receipt/poll path.
4. Open both rows from Transactions and verify their legs, journals and history.
5. Teller must offer only Cash deposit, Cash withdrawal and Internal account transfer.
   Cheque, card, NEFT, RTGS, IMPS and UPI must not appear anywhere in these create flows.

## D. Cancellation and reversal — creates permanent audit records

Cancellation requires an existing cancellable fixture because the employee UI no longer
creates external pending rails. Open such a transaction from Transactions, Cancel with a
reason, and verify the maker/supervisor policy. Do not restore RTGS UI solely for this test.

Reversal:

1. As `opsadmin` or `manager1`, open a COMPLETED deposit/internal transfer.
2. Reverse with a reason. A new compensating transaction is created.
3. The original remains present and moves through reversal state; the new record links back
   through **Open original**. Never expect the original row to be overwritten.

## E. Account servicing writes

Use a disposable/newly-created account where possible.

1. Lifecycle: ACTIVE → FROZEN → ACTIVE; ACTIVE → BLOCKED → ACTIVE; ACTIVE → DORMANT →
   ACTIVE. After each action, verify the status pill and newest status-history row.
2. Add `CIF900102` as a JOINT holder only if that customer is not already present. There is
   no public holder-removal endpoint.
3. Set limits to known test values, verify them, then restore the original values.
4. Place a small MANUAL or LIEN hold only in a disposable environment. There is no public
   release endpoint for a staff-placed hold, so this test intentionally persists.
5. Close only an account with ledger balance `0` and held amount `0`. Closing is terminal.
   A seeded funded account should refuse with BALANCE_NOT_SETTLED or HOLDS_OUTSTANDING.

## F. Back Office explorers — Phase 7, no writes

Sign in as `opsadmin`. These checks are read-only.

### Ledger

1. Open Ledger. Expect six GL accounts, including `110100` Cash and Settlement Asset and
   `210000` Customer Deposit Control. Asset/liability counts and balances should render.
2. In Journal history, inspect a POSTED row. Debit and credit totals must match, the
   Balance check pill must say BALANCED, and every line must show its ledger code and side.
3. Copy the row's transaction UUID into Transaction ID and Apply. Only its journals should
   remain. Clear must restore the full newest-first list.
4. Filter with customer account UUID `a0000000-0000-0000-0000-000000000101`; entries tied
   to that customer account should remain.

### Products

1. Expect six active catalogue products. Filter Product type to TERM DEPOSIT; expect
   `FD-12M` and `FD-24M`. Clear restores all six.
2. Inspect `FD-12M`. Expect 6.75%, a ₹5,000 opening minimum, 12-month tenure, funding
   required, the premature-closure charge and premature-penalty rule.
3. Inspect `SAV-REG`. Expect 3.50%, optional opening funding, ATM/SMS charges, and the
   day-count and interest-payout rules.

### Branches

1. Expect BR001 Mumbai Fort Main and BR002 Pune Hinjewadi, with four employees across the
   network.
2. Inspect BR001. Expect EMP-001, EMP-002 and EMP-004; Monday–Friday 10:00–16:00,
   Saturday 10:00–13:00, Sunday Closed; and the seeded Republic Day/Independence Day rows.
3. Inspect BR002 and verify only staff assigned to branch id 502 are shown.

### Notifications

1. An empty delivery queue is valid in a fresh environment and must show the explicit
   “No deliveries to show” state, not a blank table or error.
2. Status, CIF and recipient filters must apply and clear without errors.
3. The template catalogue should show four seeded templates: ACCOUNT_FROZEN,
   ACCOUNT_OPENED, KYC_VERIFIED and TXN_COMPLETED, with their channel and active state.
4. If Section B/C generated a notification, inspect it and verify recipient, delivery
   attempts, timestamps, correlation ID, last error and message content.

### Audit trail

1. Expect a paged event stream with service, event, aggregate, actor, branch and occurrence
   time. Inspect one row; metadata and formatted JSON payload must render.
2. Filter Source service to `account-service`, Apply, then Clear.
3. Filter Aggregate type `ACCOUNT` with a known account UUID. Only matching account events
   should remain.
4. If an event has a correlation ID, copy it into Correlation ID / trace and Search. The
   screen must switch to Trace mode and show the exact cross-service chain. A nonexistent
   correlation ID must produce the honest no-events state.
5. Finish by checking the browser console: no Knockout binding, JET expression, network or
   uncaught JavaScript errors should have appeared during the five-screen pass.

## G. Planned next work — not yet available in the UI

After Phase 8A, Phase 8 will add the remaining permission-gated administration in this order:

1. Product charge schedules and rule administration.
2. Notification queue/retry and template creation.
3. Statement requests, downloads, reports and schedules.
4. Manual journal posting/reversal last, with accounting-balance validation and explicit
   high-risk confirmations.

Do not test these as present functionality until `PROGRESS.md` promotes the relevant Phase
8 checkpoint.

## H. Product administration — Phase 8A promotion, stop before writes

Sign in as `opsadmin`; `PRODUCT_MANAGE` is required. Do not commit a configuration change
during the safe promotion pass.

The host restart is complete. Confirm both `:8090` and `:8000` are healthy before repeating
this pass; recheck step 1 once below 900 px to preserve the horizontal-nav regression gate.

1. Products must show **New product**. Open it and verify code, type, name, currency,
   description, rate, three monetary limits, free transactions, minimum age, optional
   tenure, effective date, overdraft and funding controls.
2. With product name blank, choose Create product. Expect “Product name is required” and no
   network mutation. Choose Go back.
3. Inspect `FD-12M`. Version history must show version 1, ACTIVE, 6.75% and its effective
   date.
4. Choose Edit terms. Product code/type/currency/tenure/funding are intentionally immutable;
   mutable metadata and commercial values must be prefilled. The commit label must say
   **Save new version**. Choose Go back.
5. Choose Deactivate. The confirmation must name FD-12M and state that existing accounts
   are not changed. Choose Go back.
6. A user with only `PRODUCT_READ` must not see New product, Edit terms or lifecycle
   controls.
7. Check the browser console and confirm no JET binding or uncaught JavaScript errors.

Deliberate write test for a disposable environment: create a uniquely coded inactive test
product, verify version 1, edit one term and verify version 2, deactivate/activate it, then
leave it INACTIVE. This is permanent catalogue data and requires explicit approval before
the browser commits it.

## I. Role and transaction boundary — Phase 8B

Sign out between each identity. Successful login deliberately reloads the requested route
once so no view model or permission snapshot survives from the previous user.

1. `teller1`: expect Overview, Teller, Internal transfers, Applications, Customers,
   Accounts, Transactions and Products. Teller offers exactly Cash deposit and Cash
   withdrawal. Internal transfers is a static same-bank flow; no rail selector appears.
2. `checker1`: expect Approvals but no Teller or Internal transfers. Sign out on
   `?ojr=transfers`, then sign back in as checker: the app must replace the URL with
   `?ojr=overview` and render Overview, never an empty or hidden transfer screen.
3. `manager1`: expect Approvals and Branches & staff, but no Teller/Internal transfers,
   Ledger, Notifications or Audit. Branches & staff contains Branches and Employees only;
   New branch, New employee and Roles & access are absent.
4. `opsadmin`: expect Ledger, Branches & staff, Notifications and Audit, but no
   Teller/Internal transfers. The organization route contains all three tabs and management
   actions.
5. A CUSTOMER login must remain on Sign in and show: “This is the employee operations
   console. Customer logins must use the customer banking application.”

The live checkpoint customer is `codex.customer.20260819@moneybags.test` / `Password@123`
with CIF `CIF607FAE22DE86`. It exists only to verify step 5 and has no account.

## J. Customer and organization administration — Phase 8B

### Customer onboarding

1. As `opsadmin`, Customers → New customer. Empty submit must say “First name is required”
   and make no request.
2. A deliberate write creates, in order, a CUSTOMER identity, customer/CIF and current
   residential address. Use a unique email, mobile and PAN. Success searches the new CIF and
   shows KYC PENDING. If the banner says a login user was created before failure, do not
   register that identity again; finish/reconcile the customer record instead.
3. The 2026-08-19 write test created `CIF607FAE22DE86` successfully. Do not repeat it with
   the same email/mobile/PAN.

### Branch, employee and role administration

Use `opsadmin` and choose Go back unless a disposable write was explicitly requested.

1. Branches: New branch validates required name/code/IFSC; Inspect exposes Edit, working
   hours, holiday and status controls. Weekly hours contain all seven days.
2. Employees: New employee combines username/email/password/full name/role with employee
   code, branch, designation, manager and joining date. CUSTOMER is not a staff-role option.
3. Inspect EMP-001. Edit must prefill DOB/designation/manager/status/role and must not offer
   Amit Patel as his own manager. Transfer offers another branch; Authority exposes account,
   transaction and reversal approval ceilings.
4. Roles & access: all five roles and their permission counts render. New/Edit role shows
   all 28 permission definitions and uses explicit Save/Create confirmation.
5. Repeat the route as `manager1`: it must be read-only with no roles tab or write controls.
6. Server boundary probe: a teller PATCH to a nonexistent branch/employee still returns
   `403 PERMISSION_DENIED` (`BRANCH_MANAGE` / `EMPLOYEE_MANAGE`) before a not-found response.

## Expected safety boundaries

- Confirmation dialogs mint one idempotency key and reuse it across retry; double-clicks
  must not post twice.
- Account and transaction confirmations state the amount/reference before the write.
- No client-side permission gate replaces server authorization; 403 responses must surface
  as a readable banner.
- Balance changes are asynchronous projections. The receipt/reference is authoritative;
  do not use an immediately refreshed balance as proof that a write succeeded.
