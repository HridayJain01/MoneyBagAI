# MoneyBags Auth and Identity Service

The active implementation is `services/identity-service`. It is the single auth and
identity service used by the MoneyBags platform. It registers in Eureka as
`security-service` so existing Feign clients continue to work. The old top-level
`auth-service` folder is retained only as source/reference and is not started by the
root Maven build or `run-all.ps1`.

## Request flow

1. A client registers or signs in through the API Gateway on port `8090`.
2. The auth service issues one signed JWT in the JSON `accessToken` field and an
   optional HttpOnly `access-token` cookie.
3. For protected requests, the gateway validates the JWT from either
   `Authorization: Bearer <token>` or the cookie.
4. The gateway removes untrusted actor headers and injects verified `X-User-Id`,
   `X-Employee-Id`, `X-Branch-Code`, and `X-Permissions` headers for downstream
   services.

In a deployed environment, expose only the API Gateway. The individual service
ports trust gateway-injected headers and must remain on the private service network.

## Start locally

Copy `.env.example` to `.env`, set the real MySQL password, then run:

```powershell
.\run-all.ps1
```

The Flyway migration creates the auth profile fields and the default `CUSTOMER`
role in the existing `moneybags_identity` database.

## Test login and a protected request

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8090/api/v1/auth/login' `
  -ContentType 'application/json' `
  -Body '{"username":"opsadmin","password":"Password@123"}'

$headers = @{ Authorization = "Bearer $($login.accessToken)" }
Invoke-RestMethod `
  -Uri 'http://localhost:8090/api/v1/users/me' `
  -Headers $headers
```

Login also accepts the standalone auth-service-compatible body:

```json
{"email":"user@example.com","password":"Password@123"}
```

## Test self-registration

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8090/api/v1/auth/register' `
  -ContentType 'application/json' `
  -Body '{"firstName":"Asha","lastName":"Shah","email":"asha@example.com","password":"Password@123","dob":"1995-06-15","gender":"FEMALE","mobile":"9876543210"}'
```

Self-registered users receive the `CUSTOMER` role. That role intentionally starts
without staff permissions; grant only the permissions required by customer-facing
features.

## Browser cookie test

```powershell
$loginBody = '{"username":"opsadmin","password":"Password@123"}'
Invoke-WebRequest `
  -Method Post `
  -Uri 'http://localhost:8090/api/v1/auth/login' `
  -ContentType 'application/json' `
  -Body $loginBody `
  -SessionVariable authSession | Out-Null

Invoke-RestMethod `
  -Uri 'http://localhost:8090/api/v1/users/me' `
  -WebSession $authSession
```

Set `AUTH_COOKIE_SECURE=true` when the deployed gateway uses HTTPS. Keep it `false`
for local HTTP testing.

## Administration

User administration requires `USER_MANAGE`. Role and permission administration
requires `ROLE_PERMISSION_MANAGE`. The seeded `opsadmin` user has both.

Supported compatibility paths include:

- `/api/v1/admin/users`
- `/api/v1/admin/roles`
- `/api/v1/admin/permissions`
- `/api/v1/admin/role-permissions`

The original MoneyBags paths under `/api/v1/users`, `/api/v1/roles`, and
`/api/v1/permissions` remain available.
