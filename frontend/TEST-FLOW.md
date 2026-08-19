# Moneybags console — end-to-end test flow

Last verified: 2026-08-19. App: `http://localhost:8000`.

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
10. **Teller** — Cash deposit, resolve `510000000101`, amount `1`, Review, **Go back**.
    Switch to withdrawal and cheque to confirm the account side and cheque-number field.
11. **Transfers**:
    - Internal: resolve `101` → `102`, amount `1`, Review, **Go back**;
    - RTGS `₹1,00,000`: expect the `₹2,00,000` minimum refusal;
    - RTGS `₹10,00,000`: expect “Approval required”, Review, **Go back**;
    - NEFT/IMPS/UPI: destination remains free text rather than being resolved locally.
12. Open Ledger, Products, Branches, Notifications and Audit trail. Each must render live
    rows or an honest empty state, with no red load banner or console error.

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
5. To seed Approvals, submit RTGS `₹10,00,000` from `101` to any external test account.
   It must stop at PENDING_APPROVAL.
6. As `checker1` in the second context, Approvals shows the row. Approve or reject it.
   The maker must not be able to check their own transaction.

## D. Cancellation and reversal — creates permanent audit records

Cancellation:

1. As `teller1`, create another `₹10,00,000` RTGS so it stays PENDING_APPROVAL.
2. Open it from Transactions and Cancel with a reason. The maker may cancel; another
   ordinary teller may not. A supervisor with `TRANSACTION_CANCEL_ANY` may cancel it.

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

## Expected safety boundaries

- Confirmation dialogs mint one idempotency key and reuse it across retry; double-clicks
  must not post twice.
- Account and transaction confirmations state the amount/reference before the write.
- No client-side permission gate replaces server authorization; 403 responses must surface
  as a readable banner.
- Balance changes are asynchronous projections. The receipt/reference is authoritative;
  do not use an immediately refreshed balance as proof that a write succeeded.
