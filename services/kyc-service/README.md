# MoneyBags KYC Service

KYC Service owns KYC sessions, uploaded identity-document bytes, captured face
frames, and verification results. Customer Service remains the source of truth
for the customer's overall `kyc_status`.

## Integration flow

```text
Employee JWT
    -> API Gateway (:8090)
    -> KYC Service (:8093, KYC_VERIFY required)
    -> Customer Service internal API
    -> customers.kyc_status
    -> Account Service and other eligibility consumers
```

When a session is approved or rejected, KYC Service synchronously sends an
idempotent decision to Customer Service. Customer Service ignores duplicate and
older decisions, updates its canonical KYC status, and writes its normal
`KycVerified` or `KycRejected` outbox event.

## Running locally

Use the repository launcher so MySQL schemas, Eureka, the gateway, Customer
Service, and KYC Service start together:

```powershell
.\run-all.ps1
```

The shared root `.env` supplies the database credentials and ports. Relevant
variables are:

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=password
KYC_SERVICE_PORT=8093
KYC_MAX_FILE_SIZE=50MB
KYC_MAX_REQUEST_SIZE=100MB
# KYC_DB_URL=jdbc:mysql://localhost:3306/moneybags_kyc?createDatabaseIfNotExist=true&serverTimezone=UTC
```

The default database is MySQL schema `moneybags_kyc`. Flyway creates
`kyc_sessions`, `kyc_documents`, `kyc_frames`, and `kyc_verifications`.

## Authentication

Call staff-facing APIs through the gateway at `http://localhost:8090`. The JWT
must contain `KYC_VERIFY`. The gateway validates the JWT, removes spoofed actor
headers, and injects the trusted employee and permission headers used by KYC
Service. The reviewer ID is taken from the authenticated employee; it is not
accepted in the JSON request body.

The browser page bundled at the KYC service root is only a development UI. For
authenticated end-to-end testing, prefer the gateway Swagger UI or the
generated Postman collection.

## API

All public routes begin with `/api/v1/kyc`:

| Method | Path | Purpose |
|---|---|---|
| POST | `/sessions` | Validate the CIF with Customer Service and create a session. |
| GET | `/sessions/{sessionId}` | Read a session. |
| GET | `/customers/{cif}/sessions/pending` | List pending sessions for a customer. |
| POST | `/sessions/{sessionId}/documents` | Upload a document as multipart form data. |
| GET | `/sessions/{sessionId}/documents/{documentType}` | Download a document. |
| POST | `/sessions/{sessionId}/frames` | Upload one or more face frames. |
| GET | `/sessions/{sessionId}/frames` | List captured frame metadata. |
| GET | `/sessions/{sessionId}/frames/{frameNumber}` | Download one captured frame. |
| GET | `/sessions/{sessionId}/result` | Read the current verification result. |
| POST | `/sessions/{sessionId}/approve` | Approve and synchronize the KYC decision. |
| POST | `/sessions/{sessionId}/reject` | Reject and synchronize the KYC decision. |

Create-session body:

```json
{
  "cifNo": "CIF900101",
  "purpose": "ACCOUNT_OPENING",
  "documentType": "AADHAAR"
}
```

Approve/reject body:

```json
{
  "reason": "manual-review",
  "remarks": "Documents and face frames match."
}
```

Document types are `AADHAAR`, `PAN`, `PASSPORT`, `DRIVING_LICENSE`, and
`VOTER_ID`.

## Verification

```powershell
mvn -pl services/customer-service,services/kyc-service -am test
node postman/generate-collections.cjs
node postman/validate-collections.cjs
```

Tests use H2 and do not require MySQL or Eureka. The automated verification
engine classes remain available, while the current API decision flow is manual
review.
