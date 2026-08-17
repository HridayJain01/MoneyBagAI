# Auth Service

A stateless **authentication and authorization (RBAC)** REST API built with **Spring Boot**.

This service provides:

- **User registration** with email/password
- **Login / Logout** using **JWT (JSON Web Tokens)** delivered through a secure **httpOnly cookie** (not an
  `Authorization` header)
- **Role-based access control (RBAC)** with pre-defined roles
- **Permission management** — permissions can be created and attached to roles
- **Admin endpoints** for user management and role/permission administration
- **"Who am I?"** endpoint returning the current authenticated user's profile and permissions

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)
- [Architecture & Package Structure](#architecture--package-structure)
- [Database Schema](#database-schema)
- [Authentication & Security Flow](#authentication--security-flow)
- [API Reference](#api-reference)
- [DTO Reference](#dto-reference)
- [Service Layer](#service-layer)
- [Utility Classes](#utility-classes)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Known Notes & Limitations](#known-notes--limitations)

---

## Tech Stack

| Layer           | Technology                                                    |
|-----------------|---------------------------------------------------------------|
| Language        | Java 17                                                       |
| Framework       | Spring Boot 4.1.0                                             |
| Web layer       | Spring MVC (`spring-boot-starter-webmvc`)                     |
| Security        | Spring Security (`spring-boot-starter-security`)              |
| Persistence     | Spring Data JPA (`spring-boot-starter-data-jpa`)              |
| Database driver | Oracle JDBC (`ojdbc11`)                                       |
| Validation      | Bean Validation (`spring-boot-starter-validation`)            |
| JWT             | JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) version 0.13.0 |
| Utilities       | Lombok, DevTools (runtime, optional)                          |
| Build tool      | Maven (wrapped: `mvnw`)                                       |

> The service is built as a self-contained Spring Boot application exposing REST endpoints. It is **stateless** — no
`HttpSession` is used; every request is authenticated by inspecting the JWT cookie.
---

## Getting Started

### Prerequisites

- **JDK 17** (project targets Java 17, see `pom.xml` `<java.version>17</java.version>`)
- **Maven** (or use the included Maven wrapper `mvnw` / `mvnw.cmd`)
- An **Oracle Database** instance reachable on `localhost:1521` with a `FREE` service/SID

### 1. Create the Oracle database schema/user

The service connects as user `c##auth` (password `admin`). Log into Oracle and run something like:

```sql
CREATE USER c##auth IDENTIFIED BY admin;
GRANT CONNECT, RESOURCE TO c##auth;
```

Because `spring.jpa.hibernate.ddl-auto=update` is enabled, Hibernate will **automatically create/update the tables** on
startup (see [Database Schema](#database-schema) for the generated tables).

### 2. Configure the connection (optional)

Edit `src/main/resources/application.properties` if your DB host, port, SID, username, or password differ (
see [Configuration Reference](#configuration-reference)).

### 3. Run the application

```bash
# Using the Maven wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Using the Maven wrapper (Linux/macOS)
./mvnw spring-boot:run

# Or with a local Maven installation
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

## Configuration Reference

All configuration lives in `src/main/resources/application.properties`.

```properties
spring.application.name=auth-service
server.port=8080
#   Database Configuration
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/FREE
spring.datasource.username=c##auth
spring.datasource.password=admin
spring.datasource.driver-class-name=oracle.jdbc.driver.OracleDriver
#   JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
spring.jpa.hibernate.ddl-auto=update
# JWT Configuration
application.security.jwt.secret-key=b69f9e70da478cccbf1f479f85e7d9372cde981520899b94fa9c4fec21d78c58
application.security.jwt.expiration=900000
```

| Property                                     | Value                                     | Purpose                                                                 |
|----------------------------------------------|-------------------------------------------|-------------------------------------------------------------------------|
| `spring.application.name`                    | `auth-service`                            | Application name                                                        |
| `server.port`                                | `8080`                                    | HTTP port the service listens on                                        |
| `spring.datasource.url`                      | `jdbc:oracle:thin:@//localhost:1521/FREE` | Oracle JDBC connection URL                                              |
| `spring.datasource.username`                 | `c##auth`                                 | DB user                                                                 |
| `spring.datasource.password`                 | `admin`                                   | DB password                                                             |
| `spring.datasource.driver-class-name`        | `oracle.jdbc.driver.OracleDriver`         | JDBC driver class                                                       |
| `spring.jpa.database-platform`               | `org.hibernate.dialect.OracleDialect`     | Hibernate dialect                                                       |
| `spring.jpa.show-sql`                        | `true`                                    | Logs generated SQL to the console                                       |
| `spring.jpa.properties.hibernate.format_sql` | `true`                                    | Pretty-prints logged SQL                                                |
| `spring.jpa.hibernate.ddl-auto`              | `update`                                  | Hibernate auto-creates/updates the schema                               |
| `application.security.jwt.secret-key`        | (64-char hex string)                      | HMAC signing secret for JWT                                             |
| `application.security.jwt.expiration`        | `900000`                                  | Access token lifetime in **milliseconds** (900,000 ms = **15 minutes**) |

### JWT properties loaded in code

The `JwtUtil` component reads these two values via `@Value`:

- `application.security.jwt.secret-key` → the HMAC secret used to sign/verify tokens (
  `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`).
- `application.security.jwt.expriration` → mapped to the `accessTokenExpirationMs` field and used as the token's `exp`
  claim and the login cookie's `Max-Age`.
---

## Architecture & Package Structure

```
auth-service
└── src/main/java/org/jeffrypatrick/authservice
    ├── AuthServiceApplication.java        # Spring Boot entry point
    ├── config/
    │   ├── SecurityConfig.java            # Security filter chain, JWT filter, CORS, beans
    │   └── DataInitializer.java           # EMPTY placeholder (no seeding implemented)
    ├── controller/                        # REST endpoints (thin layer)
    │   ├── AuthController.java
    │   ├── UserController.java
    │   ├── AdminUserController.java
    │   ├── RoleController.java
    │   ├── PermissionController.java
    │   └── RolePermissionController.java
    ├── service/                           # Business logic
    │   ├── AuthService.java
    │   ├── AdminUserService.java
    │   ├── UserService.java
    │   ├── RoleService.java
    │   ├── PermissionService.java
    │   ├── CustomUserDetailsService.java
    │   └── CustomUserDetails.java         # Spring Security principal implementation
    ├── repository/                        # Spring Data JPA repositories
    │   ├── UserRepository.java
    │   ├── RoleRepository.java
    │   └── PermissionRepository.java
    ├── model/                             # JPA entities + enums
    │   ├── User.java, Role.java, Permission.java
    │   └── RoleName.java, Gender.java, Status.java
    ├── dto/                               # Request/Response records + validation
    ├── exception/
    │   └── GlobalExceptionHandler.java    # Central REST error handling
    └── utility/
        ├── JwtUtil.java                   # JWT generation/parsing/validation
        ├── CookieUtils.java               # Cookie helpers
        └── UserUtil.java                  # Shared user-building helper
```

The flow of a typical request is:

```
HTTP Request
   → Controller (@RestController)        # maps URL → service call
   → Service (@Service)                  # business logic, @Transactional
   → Repository (JpaRepository)          # persistence
   → Oracle Database
```

All classes use **constructor injection** (no field injection).

---

## Database Schema

Entities are mapped with Hibernate (`ddl-auto=update`). Note: the JPA entity annotations define the exact table/column
names, several of which carry the typo **`PERMISSONS`**.

### `USERS` table (entity `User`)

| Column          | Type                   | Notes                                      |
|-----------------|------------------------|--------------------------------------------|
| `ID`            | sequence (`USERS_SEQ`) | Primary key                                |
| `FIRST_NAME`    | varchar(100)           | not null                                   |
| `LAST_NAME`     | varchar(100)           | not null                                   |
| `EMAIL`         | varchar(150)           | not null, **unique**                       |
| `PASSWORD_HASH` | varchar(255)           | not null (BCrypt)                          |
| `DOB`           | date                   | optional                                   |
| `GENDER`        | varchar(20)            | enum `Gender` (`MALE`, `FEMALE`, `OTHERS`) |
| `MOBILE`        | varchar(20)            | optional                                   |
| `STATUS`        | varchar(20)            | enum `Status`, default `ACTIVE`            |
| `ROLE_ID`       | FK → `ROLES.ID`        | lazy `@ManyToOne`                          |
| `CREATED_AT`    | timestamp              | set by `@PrePersist`                       |

### `ROLES` table (entity `Role`)

| Column        | Type                   | Notes                                 |
|---------------|------------------------|---------------------------------------|
| `ID`          | sequence (`ROLES_SEQ`) | Primary key                           |
| `NAME`        | varchar(50)            | enum `RoleName`, not null, **unique** |
| `DESCRIPTION` | varchar(500)           | optional                              |

### `PERMISSONS` table (entity `Permission`) — note spelling

| Column        | Type                        | Notes                |
|---------------|-----------------------------|----------------------|
| `ID`          | identity / `PERMISSONS_SEQ` | Primary key          |
| `NAME`        | varchar(100)                | not null, **unique** |
| `DESCRIPTION` | varchar(500)                | optional             |

### `ROLE_PERMISSIONS` join table (from `Role.permissions`)

Many-to-many join table: `ROLE_ID` → `ROLES.ID`, `PERMISSON_ID` → `PERMISSONS.ID`.

### Enums

- **`RoleName`**: `ADMIN`, `TELLER`, `BRANCH_MANAGER`, `LOAN_OFFICER`, `AUDITOR`, `CUSTOMER`
- **`Gender`**: `MALE`, `FEMALE`, `OTHERS`
- **`Status`**: `ACTIVE`, `LOCKED`, `DISABLED`

> `Status` drives account state: `LOCKED` → `isAccountNonLocked()` returns false; `DISABLED` → `isEnabled()` returns
> false in `CustomUserDetails`.
---

## Authentication & Security Flow

### Security configuration (`SecurityConfig`)

The `SecurityFilterChain` is configured as follows:

```java
http.csrf(AbstractHttpConfigurer::disable)                        // CSRF off (stateless API)
    .cors(Customizer.withDefaults())                              // CORS enabled
    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // CORS preflight
        .requestMatchers("/api/v1/auth/**").permitAll()           // public auth endpoints
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")     // admin-only
        .requestMatchers("/error").permitAll()
        .anyRequest().authenticated()                             // everything else needs authentication
    )
    .exceptionHandling(ex ->ex
        .authenticationEntryPoint(unauthorizedEntryPoint())       // 401 JSON
        .accessDeniedHandler(accessDeniedHandler())               // 403 JSON
    )               
    .addFilterBefore(jwtAuthenticationFilter(),UsernamePasswordAuthenticationFilter.class);
```

**Endpoint access rules:**

| URL pattern                        | Access                                   |
|------------------------------------|------------------------------------------|
| `OPTIONS /**`                      | Public (CORS preflight)                  |
| `/api/v1/auth/**`                  | Public (register, login, logout)         |
| `/api/v1/admin/**`                 | **ADMIN** role only (`hasRole("ADMIN")`) |
| `/error`                           | Public                                   |
| anything else (`/api/v1/users/**`) | Any **authenticated** user               |

### Authentication mechanism (JWT in an httpOnly cookie)

1. **Register** (`POST /api/v1/auth/register`) — creates a user (default role `CUSTOMER`), does **not** log them in.
2. **Login** (`POST /api/v1/auth/login`) — Spring Security's `AuthenticationManager` validates the credentials; on
   success the service:
    - loads the user as a `CustomUserDetails`,
    - generates a JWT (`JwtUtil.generateAccessToken`),
    - places the JWT into an **`access-token`** cookie with `httpOnly`, `secure`, `sameSite=Strict`, `path=/`, and
      `Max-Age` = token lifetime.
    - The response also returns an `AuthResponse` body.
3. **Subsequent requests** — the `jwtAuthenticationFilter` (`OncePerRequestFilter`):
    - reads the `access-token` cookie via `CookieUtils.getCookieValue`,
    - if absent/blank, the request proceeds unauthenticated,
    - otherwise extracts the email (`sub` claim), loads `CustomUserDetails`, validates the token (`isTokenValid`), and
      sets the `SecurityContextHolder` authentication if valid.
    - On any exception, it returns `401` `{"message":"Invalid or expired token"}` and clears the context.
4. **Logout** (`POST /api/v1/auth/logout`) — sets the `access-token` cookie to an empty value with `Max-Age=0`, forcing
   the browser to drop it.

### JWT structure (`JwtUtil`)

Generated with JJWT, **signed with HS256** (HMAC-SHA256) using the configured secret:

- `sub` → user email (username)
- custom claims: `userId`, `role`
- `iat` → issued-at timestamp
- `exp` → expiration (`now + accessTokenExpirationMs`, default 15 min)

Validation (`isTokenValid`) checks that the token's subject matches the loaded user **and** the token is not expired.

### CORS

Allowed origins: `http://localhost:3000`, `https://localhost:3000`, `http://localhost:8080`, `https://localhost:8080`.
Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`. Allowed headers: `*`. `allowCredentials=true`.

### Support beans

- `PasswordEncoder` → `BCryptPasswordEncoder` (used to hash all passwords).
- `AuthenticationManager` → from `AuthenticationConfiguration`.
- `unauthorizedEntryPoint()` → returns `401` `{"message":"Unauthorized"}`.
- `accessDeniedHandler()` → returns `403` `{"message":"Forbidden: insufficient privileges"}`.

---

## API Reference

Base URL: `http://localhost:8080`

All bodies are `application/json`. Admin endpoints require an **ADMIN** role (valid `access-token` cookie). All other
non-auth endpoints require any authenticated user.

### 1. Auth endpoints — `/api/v1/auth` *(public)*

#### POST `/api/v1/auth/register` — Create a new account

Creates a user with the default `CUSTOMER` role.

**Request body (`RegisterRequest`):**

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "password123",
  "dob": "1990-01-15",
  "gender": "MALE",
  "mobile": "1234567890"
}
```

| Field       | Type                            | Validation                              |
|-------------|---------------------------------|-----------------------------------------|
| `firstName` | string                          | required, max 100                       |
| `lastName`  | string                          | required, max 100                       |
| `email`     | string                          | required, valid email, max 150          |
| `password`  | string                          | required, min 8, max 100                |
| `dob`       | date (yyyy-MM-dd)               | optional, must be in the past (`@Past`) |
| `gender`    | enum (`MALE`/`FEMALE`/`OTHERS`) | optional                                |
| `mobile`    | string                          | optional, max 20                        |

**Response `200 OK` (`AuthResponse`):**

```json
{
  "message": "Registered successfully",
  "user": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "dob": "1990-01-15",
    "gender": "MALE",
    "mobile": "1234567890",
    "role": "CUSTOMER",
    "permissions": [],
    "status": "ACTIVE",
    "createdAt": "2026-08-11T12:00:00"
  }
}
```

**Errors:** `400` if the email already exists; `400` if the `CUSTOMER` role does not exist in the DB (
see [Known Notes & Limitations](#known-notes--limitations)).

---

#### POST `/api/v1/auth/login` — Authenticate and receive a JWT cookie

Validates credentials and sets the `access-token` httpOnly cookie on the response.

**Request body (`LoginRequest`):**

```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response `200 OK`** — sets the `Set-Cookie: access-token=...` header and returns:

```json
{
  "message": "Login successful",
  "user": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "dob": "1990-01-15",
    "gender": "MALE",
    "mobile": "1234567890",
    "role": "CUSTOMER",
    "permissions": [],
    "status": "ACTIVE",
    "createdAt": "2026-08-11T12:00:00"
  }
}
```

**Errors:** `401` for bad credentials / unknown user.

---

#### POST `/api/v1/auth/logout` — Invalidate the session cookie

Clears the `access-token` cookie (sets `Max-Age=0`).

**Request:** no-body.

**Response `200 OK`:**

```json
{
  "message": "Logged out successfully",
  "user": null
}
```

---

### 2. Current user — `/api/v1/users` *(any authenticated user)*

#### GET `/api/v1/users/me` — Get the current user's profile

Returns the profile and permission set of the authenticated user (identified from the JWT cookie).

**Request:** no-body (cookie required).

**Response `200 OK` (`UserInfoResponse`):**

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "dob": "1990-01-15",
  "gender": "MALE",
  "mobile": "1234567890",
  "role": "CUSTOMER",
  "permissions": [],
  "status": "ACTIVE",
  "createdAt": "2026-08-11T12:00:00"
}
```

---

### 3. Admin user management — `/api/v1/admin/users` *(ADMIN only)*

#### POST `/api/v1/admin/users` — Admin creates a user with a chosen role

Creates a user with an explicit (non-ADMIN) role.

**Request body (`AdminCreateUserRequest`):**

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "password": "password123",
  "dob": "1985-07-22",
  "gender": "FEMALE",
  "mobile": "0987654321",
  "role": "TELLER"
}
```

| Field  | Type              | Validation                      |
|--------|-------------------|---------------------------------|
| `role` | enum (`RoleName`) | **required**; cannot be `ADMIN` |

**Response `200 OK`** (`UserInfoResponse`, same shape as `/users/me`).

**Errors:** `400` if `role` is `ADMIN`, email already exists, or role not found.

---

#### PUT `/api/v1/admin/users/{id}/role` — Change a user's role

**Path variable:** `id` — the user ID (Long).

**Request body (`UpdateUserRoleRequest`):**

```json
{
  "role": "LOAN_OFFICER"
}
```

| Field  | Type              | Validation                      |
|--------|-------------------|---------------------------------|
| `role` | enum (`RoleName`) | **required**; cannot be `ADMIN` |

**Response `200 OK`** (`UserInfoResponse`, updated with the new role).

**Errors:** `400` if user not found, role is `ADMIN`, or role not found.

---

### 4. Role management — `/api/v1/admin/roles` *(ADMIN only)*

#### POST `/api/v1/admin/roles` — Create a role

**Request body (`RoleRequest`):**

```json
{
  "name": "TELLER",
  "description": "Handles counter transactions"
}
```

| Field         | Type              | Validation        |
|---------------|-------------------|-------------------|
| `name`        | enum (`RoleName`) | required          |
| `description` | string            | optional, max 500 |

**Response `200 OK` (`RoleResponse`):**

```json
{
  "id": 2,
  "name": "TELLER",
  "description": "Handles counter transactions",
  "permissions": []
}
```

**Errors:** `400` if the role name already exists.

---

#### GET `/api/v1/admin/roles` — List all roles

**Response `200 OK`:** array of `RoleResponse`.

```json
[
  {
    "id": 1,
    "name": "ADMIN",
    "description": "Full access",
    "permissions": []
  }
]
```

---

#### PUT `/api/v1/admin/roles/{id}` — Update a role

**Path variable:** `id` — the role ID.

**Request body (`RoleRequest`):** same as create (name + description).

**Response `200 OK`** (`RoleResponse`, updated).

**Errors:** `400` if role not found.

---

#### DELETE `/api/v1/admin/roles/{id}` — Delete a role

**Path variable:** `id` — the role ID.

**Response `204 No Content`.**

**Errors:** `400` if role not found.
---

### 5. Permission management — `/api/v1/admin/permissions` *(ADMIN only)*

#### POST `/api/v1/admin/permissions` — Create a permission

**Request body (`PermissionRequest`):**

```json
{
  "name": "CREATE_LOAN",
  "description": "Can create loan records"
}
```

| Field         | Type   | Validation        |
|---------------|--------|-------------------|
| `name`        | string | required, max 100 |
| `description` | string | optional, max 500 |

**Response `200 OK` (`PermissionResponse`):**

```json
{
  "id": 5,
  "name": "CREATE_LOAN",
  "description": "Can create loan records"
}
```

**Errors:** `400` if a permission with that name already exists.

---

#### GET `/api/v1/admin/permissions` — List all permissions

**Response `200 OK`:** array of `PermissionResponse`.

---

#### GET `/api/v1/admin/permissions/{id}` — Get a permission by ID

**Path variable:** `id` — the permission ID.

**Response `200 OK`** (`PermissionResponse`).

**Errors:** `400` if permission not found.

---

#### PUT `/api/v1/admin/permissions/{id}` — Update a permission

**Path variable:** `id` — the permission ID.

**Request body (`PermissionRequest`):** same as create.

**Response `200 OK`** (`PermissionResponse`, updated).

**Errors:** `400` if permission not found.

---

#### DELETE `/api/v1/admin/permissions/{id}` — Delete a permission

**Path variable:** `id` — the permission ID.

**Response `204 No Content`.**

**Errors:** `400` if permission not found.

---

### 6. Role ↔ permission linking — `/api/v1/admin/role-permissions` *(ADMIN only)*

#### POST `/api/v1/admin/role-permissions` — Link one permission to a role

**Request body (`RolePermissionLinkRequest`):**

```json
{
  "roleId": 2,
  "permissionId": 5
}
```

| Field          | Type | Validation |
|----------------|------|------------|
| `roleId`       | Long | required   |
| `permissionId` | Long | required   |

**Response `200 OK`** (`RoleResponse` with the updated permission set).

**Errors:** `400` if role or permission not found.

---

#### GET `/api/v1/admin/role-permissions/{roleId}` — Get a role with its permissions

**Path variable:** `roleId` — the role ID.

**Response `200 OK`** (`RoleResponse` including the `permissions` set).

---

#### PUT `/api/v1/admin/role-permissions/{roleId}` — Replace all permissions of a role

**Path variable:** `roleId` — the role ID.

**Request body (`RolePermissionRequest`):**

```json
{
  "roleId": 2,
  "permissionIds": [
    5,
    6,
    7
  ]
}
```

> Note: the `roleId` inside the body is ignored by the handler (the path variable wins); only `permissionIds` is used.

| Field           | Type        | Validation         |
|-----------------|-------------|--------------------|
| `roleId`        | Long        | required (ignored) |
| `permissionIds` | set of Long | required           |

**Response `200 OK`** (`RoleResponse`).

**Errors:** `400` if role or any permission not found.

---

#### DELETE `/api/v1/admin/role-permissions/{roleId}/{permissionId}` — Unlink one permission from a role

**Path variables:** `roleId` and `permissionId`.

**Response `200 OK`** (`RoleResponse` with the permission removed).

**Errors:** `400` if role or permission not found.
---

## DTO Reference

All DTOs are Java **records** in the `dto` package. Request records carry Bean Validation annotations.

| DTO                         | Kind     | Fields / Notes                                                                                            |
|-----------------------------|----------|-----------------------------------------------------------------------------------------------------------|
| `RegisterRequest`           | Request  | firstName, lastName, email, password, dob, gender, mobile                                                 |
| `LoginRequest`              | Request  | email (`@NotBlank @Email`), password (`@NotBlank`)                                                        |
| `AdminCreateUserRequest`    | Request  | like Register + `role` (`@NotNull`)                                                                       |
| `UpdateUserRoleRequest`     | Request  | `role` (`@NotNull`)                                                                                       |
| `RoleRequest`               | Request  | `name` (`@NotNull` RoleName), `description`                                                               |
| `RolePermissionLinkRequest` | Request  | `roleId`, `permissionId` (both `@NotNull`)                                                                |
| `RolePermissionRequest`     | Request  | `roleId`, `permissionIds` (both `@NotNull`)                                                               |
| `PermissionRequest`         | Request  | `name` (`@NotBlank`, max 100), `description` (max 500)                                                    |
| `AuthResponse`              | Response | `message`, `user` (`UserInfoResponse` or null)                                                            |
| `UserInfoResponse`          | Response | id, firstName, lastName, email, dob, gender, mobile, role, permissions (Set\<String\>), status, createdAt |
| `RoleResponse`              | Response | id, name, description, permissions (Set\<PermissionResponse\>)                                            |
| `PermissionResponse`        | Response | id, name, description                                                                                     |

---

## Service Layer

### `AuthService`

Handles public authentication operations.

| Method                      | Description                                                                                                                                                                                                     |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `register(RegisterRequest)` | Validates email uniqueness, assigns the default `CUSTOMER` role, BCrypt-hashes the password, saves the user, returns an `AuthResponse`. `@Transactional`.                                                       |
| `login(LoginRequest)`       | Authenticates via `AuthenticationManager`, loads `CustomUserDetails`, generates a JWT, builds the `access-token` httpOnly cookie, returns a `LoginResult` (token, `Set-Cookie` header, body). `@Transactional`. |
| `logoutCookie()`            | Returns an expired empty `access-token` cookie string.                                                                                                                                                          |
| `record LoginResult`        | Holds `token`, `setCookieHeader`, `body`.                                                                                                                                                                       |

### `AdminUserService`

Admin-only user management.

| Method                                    | Description                                                                  |
|-------------------------------------------|------------------------------------------------------------------------------|
| `createUser(AdminCreateUserRequest)`      | Creates a user with an explicit role; **rejects `ADMIN`**. `@Transactional`. |
| `changeRole(Long, UpdateUserRoleRequest)` | Assigns a new role to a user; **rejects `ADMIN`**. `@Transactional`.         |

### `UserService`

| Method                               | Description                                                                                               |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `getCurrentUserInfo(Authentication)` | Looks up the authenticated user by email and returns `UserInfoResponse`. `@Transactional(readOnly=true)`. |

### `RoleService`

| Method                                | Description                                                |
|---------------------------------------|------------------------------------------------------------|
| `create(RoleRequest)`                 | Creates a role; rejects duplicate names. `@Transactional`. |
| `getAll()`                            | Lists all roles with their permissions.                    |
| `getById(Long)`                       | Returns one role with permissions.                         |
| `update(Long, RoleRequest)`           | Updates name/description.                                  |
| `delete(Long)`                        | Deletes a role.                                            |
| `replacePermissions(Long, Set<Long>)` | Replaces the role's entire permission set.                 |
| `linkPermission(Long, Long)`          | Adds a permission to a role.                               |
| `unlinkPermission(Long, Long)`        | Removes a permission from a role.                          |

### `PermissionService`

| Method                            | Description                                    |
|-----------------------------------|------------------------------------------------|
| `create(PermissionRequest)`       | Creates a permission; rejects duplicate names. |
| `getAll()`                        | Lists all permissions.                         |
| `getById(Long)`                   | Returns one permission.                        |
| `update(Long, PermissionRequest)` | Updates name/description.                      |
| `delete(Long)`                    | Deletes a permission.                          |

### `CustomUserDetailsService`

Implements Spring Security's `UserDetailsService`. `loadUserByUsername(email)` loads the user by email and wraps it in a
`CustomUserDetails`. Throws `UsernameNotFoundException` if not found. `@Transactional(readOnly=true)`.

### `CustomUserDetails`

Spring Security `UserDetails` implementation built from a `User` entity:

- `getUsername()` → email
- `getPassword()` → password hash
- `getAuthorities()` → single authority `ROLE_<ROLENAME>`
- `isAccountNonLocked()` → `false` when `Status == LOCKED`
- `isEnabled()` → `false` when `Status == DISABLED`

> Note: permissions are collected into `CustomUserDetails` but `getAuthorities()` currently exposes only the role
> authority — permissions are not enforced as authorities in the current security config.
---

## Utility Classes

### `JwtUtil` (`@Component`)

| Method                                    | Description                                                                         |
|-------------------------------------------|-------------------------------------------------------------------------------------|
| `generateAccessToken(CustomUserDetails)`  | Creates an HS256-signed JWT with `sub`=email, claims `userId`/`role`, `iat`, `exp`. |
| `extractUsername(String)`                 | Returns the `sub` claim (email).                                                    |
| `isTokenValid(String, CustomUserDetails)` | True if the subject matches the user and the token is not expired.                  |
| `isTokenExpired(String)`                  | True if `exp` is before now.                                                        |
| `getAllClaims(String)`                    | Parses and verifies the token, returning its claims.                                |
| `getAccessTokenExpirationMs()`            | Getter for the configured expiration (Lombok `@Getter`).                            |

### `CookieUtils` (static helper)

| Method                                       | Description                                                                                           |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `getCookieValue(HttpServletRequest, String)` | Returns a cookie's value by name, or `null` if absent. Used by the JWT filter to read `access-token`. |

### `UserUtil` (static helper)

| Method                  | Description                                                                                                                                      |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `commonCreateUser(...)` | Builds and returns a populated `User` from the provided fields. Shared by `AuthService` and `AdminUserService` to avoid duplicated builder code. |

---

## Error Handling

Centralized by `GlobalExceptionHandler` (`@RestControllerAdvice`). Errors are returned as JSON with a `message` key (and
an `errors` map for validation failures).

| Exception                         | HTTP status | Response shape                                                 |
|-----------------------------------|-------------|----------------------------------------------------------------|
| `IllegalArgumentException`        | `400`       | `{"message": "<msg>"}`                                         |
| `BadCredentialsException`         | `401`       | `{"message": "<msg>"}`                                         |
| `UsernameNotFoundException`       | `401`       | `{"message": "<msg>"}`                                         |
| `AuthenticationException`         | `401`       | `{"message": "<msg>"}`                                         |
| `AccessDeniedException`           | `403`       | `{"message": "<msg>"}`                                         |
| `MethodArgumentNotValidException` | `400`       | `{"message":"Validation failed","errors":{"<field>":"<msg>"}}` |
| `HttpMessageNotReadableException` | `400`       | `{"message": "<msg>"}` (bad JSON)                              |
| `Exception` (fallback)            | `500`       | `{"message": "<msg>"}`                                         |

Additionally, the Spring Security filter chain produces its own JSON for unauthenticated/forbidden requests:

- `401` `{"message":"Unauthorized"}` — from `unauthorizedEntryPoint`
- `403` `{"message":"Forbidden: insufficient privileges"}` — from `accessDeniedHandler`
- `401` `{"message":"Invalid or expired token"}` — from the JWT filter on token failures

---

## Testing

Run the test suite with:

```bash
# Windows
.\mvnw.cmd test

# Linux/macOS
./mvnw test
```

The only test is `AuthServiceApplicationTests.contextLoads()`, a `@SpringBootTest` smoke test verifying the application
context loads. It requires the Oracle database to be reachable (it loads the full Spring context on
`localhost:1521/FREE`).

---

## Known Notes & Limitations

1. **`DataInitializer` is empty.** No roles/permissions are seeded automatically. Registration requires a `CUSTOMER`role
   to exist, and admin endpoints require a user with an `ADMIN` role — both must be created manually (via the admin
   endpoints or directly in the DB) before the service can be used end-to-end.
2. **`ADMIN` role cannot be assigned via admin endpoints.** Both `AdminUserService.createUser` and
   `AdminUserService.changeRole` throw if `role == RoleName.ADMIN`. An `ADMIN` user therefore cannot be created through
   the API in the current code.
3. **Table name typo:** the permission table is mapped as **`PERMISSONS`** (three S's) in the `Permission` entity and
   the join table references `PERMISSON_ID`.
4. **Property typo:** the JWT property is `application.security.jwt.expriration` (misspelled) both in
   `application.properties` and the `@Value` in `JwtUtil`.
5. **Cookie security flag:** the `access-token` cookie is marked `secure`, which means it will **only** be sent over
   HTTPS. Over plain HTTP `localhost` (with `http://localhost:8080` CORS origin) some browsers may not store/send the
   cookie; consider serving over HTTPS or relaxing the flag in a non-production setup.
6. **Token lifetime:** the JWT expires after `expriration` ms (default 15 minutes). There is no refresh-token mechanism
   in the code.
7. **Permissions are not enforced as Spring authorities.** Although `Role.permissions` are loaded and returned in
   responses, `CustomUserDetails.getAuthorities()` exposes only the role; method-level or URL-level permission checks
   are not implemented yet.

