# Moneybags Postman collections

This directory contains one Postman Collection v2.1 file per runnable service. Each Spring controller has its own folder, and every `GetMapping`, `PostMapping`, `PutMapping`, `PatchMapping`, and `DeleteMapping` in the current source is represented.

## Collections

| Service | Port | Controllers | Controller requests | Collection |
|---|---:|---:|---:|---|
| Eureka Server | 8080 | 0 | 0 | `collections/eureka-server.postman_collection.json` |
| Branch Employee Service | 8081 | 3 | 22 | `collections/branch-employee-service.postman_collection.json` |
| Customer Service | 8082 | 8 | 29 | `collections/customer-service.postman_collection.json` |
| Account Service | 8083 | 2 | 34 | `collections/account-service.postman_collection.json` |
| Transaction Service | 8084 | 5 | 33 | `collections/transaction-service.postman_collection.json` |
| Ledger Service | 8085 | 3 | 10 | `collections/ledger-service.postman_collection.json` |
| Statement Reporting Service | 8086 | 5 | 22 | `collections/statement-reporting-service.postman_collection.json` |
| Identity Service | 8087 | 3 | 19 | `collections/identity-service.postman_collection.json` |
| Product Service | 8088 | 1 | 15 | `collections/product-service.postman_collection.json` |
| Notification Service | 8089 | 1 | 6 | `collections/notification-service.postman_collection.json` |
| API Gateway | 8090 | 1 | 2 | `collections/api-gateway.postman_collection.json` |
| Audit Service | 8091 | 1 | 6 | `collections/audit-service.postman_collection.json` |
| Configuration Service | 8092 | 7 | 31 | `collections/configuration-service.postman_collection.json` |
| KYC Service | 8093 | 1 | 11 | `collections/kyc-service.postman_collection.json` |
| Legacy top-level Product Service | 8083 | 1 | 8 | `collections/legacy-product-service.postman_collection.json` |

The Eureka collection contains operational requests even though Eureka does not declare an application controller. Every collection also includes `Service Operations` requests for health and generated OpenAPI discovery; those operational requests are not included in the controller-request counts above.

The legacy top-level product service and account-service both default to port 8083, so they cannot be run on that port at the same time. Change the legacy collection's `baseUrl` when running both.

## Using the collections

1. Import one or more JSON files from `collections/` into Postman.
2. Start the relevant services with the repository's `run-all.ps1`, or start one service independently.
3. For direct service calls, leave each collection's `baseUrl` at its default local port.
4. For authenticated calls through the gateway, set `baseUrl` to `http://localhost:8090`. KYC routes should normally be tested this way so the gateway can validate the JWT and inject trusted actor headers.
5. Run `Identity Service > AuthController > POST Login`. A successful response automatically stores its session/token in the collection's `sessionId` variable.
6. In the KYC collection, `POST Create Session` stores the returned KYC session in `kycSessionId`; select local files in the multipart document/frame requests before sending them.
7. Review the collection variables before running mutation requests. Seed-dependent IDs are examples and may need to be replaced with IDs returned by earlier requests.

Direct staff-facing requests include `X-Employee-Id`, `X-Branch-Code`, `X-Permissions`, and `X-Correlation-Id`. Postman generates correlation IDs before each request and initializes an idempotency key for APIs that require one.

## Regeneration and audit

Run the generator after controller changes:

```powershell
node postman/generate-collections.cjs
node postman/validate-collections.cjs
```

`coverage-report.json` records every controller source file and route included in the generated collections. The generator discovers controller classes by annotation, which includes the five package-private controllers declared together in statement-reporting-service's `Controllers.java`.
