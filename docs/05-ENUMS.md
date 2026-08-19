# 05 — Enums & Constants

Every enum the API accepts or returns. This is your source for dropdown options, status
badges and client-side validation.

Values are sent and received as **exact uppercase strings**. An unrecognised value on a
request is a `400`.

---

## Identity

**`UserStatus`** — `users.status`

| Value | Meaning |
|---|---|
| `ACTIVE` | Normal, can log in |
| `LOCKED` | Too many failed attempts; auto-clears at `locked_until` |
| `DISABLED` | Administratively disabled; only an admin can re-enable |

**`Gender`** (identity) — `MALE` `FEMALE` `OTHERS`

> Identity and customer services use **different** gender enums. Identity has `OTHERS`;
> customer has `NON_BINARY` and `UNDISCLOSED`. Do not share one dropdown between the
> registration form and the customer form.

**Roles** — `TELLER` `CHECKER` `BRANCH_MANAGER` `OPS_ADMIN` `CUSTOMER`

**Permissions** — 28 values, see [02 — Authentication](02-AUTHENTICATION.md#full-permission-catalogue)

---

## Customer

**`CustomerStatus`** — `ACTIVE` `INACTIVE` `DECEASED` `BLOCKED`

**`KycStatus`** — the canonical customer KYC state

| Value | Meaning |
|---|---|
| `PENDING` | Not yet verified — blocks account opening |
| `VERIFIED` | Cleared |
| `REJECTED` | Failed review; `kyc_failure_count` incremented |
| `EXPIRED` | A document passed its expiry date |

**`RiskClassification`** — `LOW` `MEDIUM` `HIGH` (default `LOW`)

**`Gender`** (customer) — `MALE` `FEMALE` `NON_BINARY` `UNDISCLOSED`

**`AddressType`** — `RESIDENTIAL` `PERMANENT` `OFFICE`

**`CommunicationChannel`** — `EMAIL` `SMS` `PUSH` `NONE`

**`DocumentVerifyStatus`** — `PENDING` `VERIFIED` `REJECTED`

**`EventPublicationStatus`** — `PENDING` `PUBLISHED` `FAILED`

---

## KYC

**`KycSessionStatus`** — the workflow state machine

| Value | Stage |
|---|---|
| `CREATED` | Session opened |
| `DOCUMENT_PENDING` | Awaiting document upload |
| `DOCUMENT_UPLOADED` | Document stored |
| `FRAME_CAPTURE_PENDING` | Awaiting face frames |
| `FRAME_CAPTURED` | Frames stored |
| `VERIFICATION_IN_PROGRESS` | Under review |
| `VERIFIED` | Approved — pushed to customer-service |
| `REJECTED` | Rejected — pushed to customer-service |
| `FAILED` | Processing error |

Happy path: `CREATED → DOCUMENT_PENDING → DOCUMENT_UPLOADED → FRAME_CAPTURE_PENDING →
FRAME_CAPTURED → VERIFICATION_IN_PROGRESS → VERIFIED`

**`DocumentType`** — `AADHAAR` `PAN` `PASSPORT` `DRIVING_LICENSE` `VOTER_ID`

**`VerificationStatus`** (engine-internal) — `CONTINUE` `STOP`

---

## Product

**`ProductType`**

| Value | Tenure |
|---|---|
| `SAVINGS` | must be null |
| `CURRENT` | must be null |
| `TERM_DEPOSIT` | **required** |
| `RECURRING_DEPOSIT` | **required** |

The tenure rule is a database CHECK constraint, not just validation.

**`ProductStatus`** — `ACTIVE` `INACTIVE`

**Charge types** (seeded, not a Java enum) — `ATM_WITHDRAWAL` `SMS_ALERT` `MAINTENANCE`
`PREMATURE_CLOSURE` `MISSED_INSTALMENT`

**Charge frequencies** (seeded) — `PER_TRANSACTION` `MONTHLY` `ONE_TIME`

**Rule keys** (seeded) — `DAY_COUNT_BASIS` `INTEREST_PAYOUT` `MIN_AGE_YEARS`
`OVERDRAFT_LIMIT` `PREMATURE_PENALTY_PCT` `INSTALMENT_FREQUENCY`

**Product codes** (seeded) — `SAV-REG` `SAV-SENIOR` `CUR-BASIC` `FD-12M` `FD-24M` `RD-12M`

---

## Account

**`AccountStatus`**

| Value | Debit? | Credit? | Meaning |
|---|---|---|---|
| `PENDING_ACTIVATION` | no | no | Opened but unfunded (products with `requiresFunding`) |
| `ACTIVE` | **yes** | **yes** | Normal |
| `DORMANT` | no | **yes** | Inactive past the dormancy window; can still receive |
| `FROZEN` | no | no | Temporarily frozen |
| `BLOCKED` | no | no | Blocked with reason |
| `CLOSURE_REQUESTED` | no | no | Closure in progress |
| `MATURED` | no | no | Term product reached maturity |
| `CLOSED` | no | no | **Terminal** |

Only `ACTIVE` allows a debit. `ACTIVE` and `DORMANT` allow a credit — a dormant account
can still receive money, which is what reactivates it.

**`ApplicationStatus`** — `DRAFT` `SUBMITTED` `PENDING_APPROVAL` `APPROVED` `REJECTED`
`CANCELLED`. The last three are decided/terminal.

**`HoldStatus`** — `HELD` `CONSUMED` `RELEASED` `EXPIRED`

**`HoldType`** — `TRANSACTION` (created by transaction-service) `LIEN` `MANUAL` (both
staff-created)

**`Direction`** — `DEBIT` `CREDIT`

**`ProductAcquisitionType`** — `ACCOUNT_OPENING` `TRANSACTION_PURCHASE`

**`ProductOwnershipStatus`** — `PENDING` `ACTIVE` `REVERSED` `MATURED` `CLOSED`

**`OutboxStatus`** — `PENDING` `PUBLISHED` `FAILED`

**Holder roles** — `PRIMARY` `JOINT`

---

## Transaction

**`TransactionType`**

| Value | Debits an account? | Externally cleared? |
|---|---|---|
| `DEPOSIT` | no | no |
| `WITHDRAWAL` | yes | no |
| `INTERNAL_TRANSFER` | yes | no |
| `NEFT` | yes | **yes** |
| `RTGS` | yes | **yes** |
| `IMPS` | yes | **yes** |
| `UPI` | yes | **yes** |
| `CHEQUE` | no | **yes** |
| `CARD_PAYMENT` | yes | **yes** |
| `PRODUCT_PURCHASE` | yes | no |
| `REVERSAL` | yes | no |

Externally cleared types get a `clearing_instructions` row and complete asynchronously via
a rail callback.

**`TransactionStatus`** — 14 values

| Value | Terminal | Meaning |
|---|---|---|
| `RECEIVED` | | Accepted, not yet validated |
| `VALIDATED` | | Passed business validation |
| `PENDING_APPROVAL` | | Over the maker-checker threshold |
| `APPROVED` | | Approved, awaiting execution |
| `FUNDS_RESERVED` | | Hold placed on the source account |
| `PROCESSING` | | Executing |
| `PROJECTION_PENDING` | | Posted; balance projection outstanding |
| `SETTLED` | | Rail confirmed settlement |
| `COMPLETED` | **yes** | Fully done |
| `FAILED` | **yes** | Definitively failed |
| `REJECTED` | **yes** | Rejected by a checker |
| `CANCELLED` | **yes** | Cancelled by the maker |
| `REVERSAL_PENDING` | | Reversal in flight |
| `REVERSED` | **yes** | Compensated by a linked reversal |

Terminal statuses accept no further action. Show a spinner and poll
`GET /api/v1/transactions/{id}/status` while non-terminal.

**`PaymentRail`** — `CASH` `INTERNAL` `NEFT` `RTGS` `IMPS` `UPI` `CHEQUE` `CARD`

**`PaymentChannel`** — `BRANCH` `MOBILE` `WEB` `ATM` `API` `INTERNAL`

**`PaymentMethod`** — `CASH` `ACCOUNT` `NEFT` `RTGS` `IMPS` `UPI` `CHEQUE` `CARD`

> `PaymentRail` and `PaymentMethod` have near-identical values but are separate fields with
> separate meanings. Rail is *how the money moves between banks*; method is *what the payer
> used*. For a branch cash deposit: rail `CASH`, method `CASH`. For an internal transfer:
> rail `INTERNAL`, method `ACCOUNT`.

**`LegRole`** — `SOURCE` `DESTINATION` `FEE` `COUNTERPARTY`

**`HoldStatus`** (transaction copy) — `FUNDS_HELD` `CONSUMED` `RELEASED`

> Note this differs from account-service's `HoldStatus`, which uses `HELD` (not
> `FUNDS_HELD`) and adds `EXPIRED`.

**`JournalStatus`** (transaction copy) — `PENDING` `POSTED`

**`ClearingStatus`** — `CREATED` `SUBMITTED` `SETTLED` `FAILED` `CANCELLED`

**`IdempotencyState`** — `PROCESSING` `COMPLETED` `FAILED`

**`ReconciliationStatus`** — `OPEN` `ASSIGNED` `RESOLVED`

**`ProductPurchaseStatus`** — `PENDING` `ACTIVE` `CANCELLED` `REVERSED`

**`OutboxStatus`** — `PENDING` `PUBLISHED` `FAILED`

---

## Ledger

**`LedgerAccountType`** — `ASSET` `LIABILITY` `INCOME` `EXPENSE` `CLEARING`

**`EntrySide`** — `DEBIT` `CREDIT`

**`JournalStatus`** (ledger) — `DRAFT` `POSTED` `REVERSED`

> Ledger's `JournalStatus` has three values including `REVERSED`; transaction-service's has
> two (`PENDING` `POSTED`). They are different enums in different schemas.

**Seeded GL account codes** — `110100` `210000` `210100` `220100` `220200` `410100`,
all in INR. `210100` is the Term Deposit Control liability.

---

## Statement & Reporting

**`OutputFormat`** — `PDF` `CSV` `XLSX`

**`StatementKind`** — `DATE_RANGE` `MONTHLY` `YEARLY`

**`RequestStatus`** — `PENDING` `GENERATING` `READY` `FAILED` `CANCELLED`

Poll until `READY`, then request a download link. `FAILED` carries `errorCode` and
`errorMessage`.

**`Frequency`** — `DAILY` `WEEKLY` `MONTHLY` `YEARLY`

**`Direction`** — `DEBIT` `CREDIT`

**Download outcomes** — `SUCCESS` `DENIED` `EXPIRED` `FAILED`

---

## Notification

**Channels** — `EMAIL` `SMS` `PUSH`

**Statuses**

| Value | Meaning |
|---|---|
| `PENDING` | Queued, awaiting delivery |
| `SENT` | Delivered |
| `FAILED` | Delivery failed after retries |
| `SUPPRESSED` | Recipient opted out of this channel in customer-service |

`SUPPRESSED` is not an error — do not show it as one.

**Default locale** — `en-IN`

---

## Configuration

**Feature flag `enabled`** — `'Y'` or `'N'` as a **single-character string**, not a boolean.
Several configuration columns use this convention (`require_upper`, `require_digit`,
`require_special`, `is_closed`).

---

## Shared constants

**Currency** — `INR` throughout. The wire format is a 3-uppercase-letter string, validated
by `[A-Z]{3}`.

**Seeded branches** — `BR001` (Mumbai Fort Main, IFSC `MBAG0000001`) ·
`BR002` (Pune Hinjewadi, IFSC `MBAG0000002`)

**Seeded customers** — `CIF900101` (Vikram Rao, `VERIFIED`) ·
`CIF900102` (Ananya Deshmukh, `PENDING`)

**Seeded accounts**

| Account id | Number | CIF | Product | Branch | Balance |
|---|---|---|---|---|---|
| `a0000000-…-101` | 510000000101 | CIF900101 | SAV-REG | BR001 | 50 000 |
| `a0000000-…-102` | 510000000102 | CIF900101 | CUR-BASIC | BR001 | 120 000 |
| `a0000000-…-103` | 520000000103 | CIF900102 | SAV-SENIOR | BR002 | 8 000 |

`CIF900102` is deliberately `PENDING` KYC — use it to exercise the eligibility-denied path.
