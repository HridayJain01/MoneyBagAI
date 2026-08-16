# Product Master Service

Minimal Spring Boot Product Master Service for a banking microservices project.

## Run

Set Oracle environment variables before starting the app:

```bash
DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1
DB_USERNAME=product_user
DB_PASSWORD=product_password
DB_DRIVER=oracle.jdbc.OracleDriver
HIBERNATE_DIALECT=org.hibernate.dialect.OracleDialect
JPA_DDL_AUTO=none
SQL_INIT_MODE=always
SERVER_PORT=8083
```

Start the service:

```bash
mvn spring-boot:run
```

Base URL:

```text
http://localhost:8083/api/v1/products
```

## Postman Examples

### Create Product

`POST /api/v1/products`

```json
{
  "productCode": "SAV-REG",
  "name": "Regular Savings Account",
  "productType": "DEPOSIT",
  "interestRate": 3.000,
  "minBalance": 1000,
  "tenureMonths": null,
  "openingFee": 0,
  "status": "ACTIVE",
  "effectiveFrom": "2026-07-01",
  "effectiveTo": null
}
```

### Create Product Without Status

`POST /api/v1/products`

```json
{
  "productCode": "INS-HEALTH-BASIC",
  "name": "Basic Health Insurance",
  "productType": "INSURANCE",
  "interestRate": 0,
  "minBalance": null,
  "tenureMonths": 12,
  "openingFee": 250,
  "effectiveFrom": "2026-07-01",
  "effectiveTo": null
}
```

### List Products

```text
GET /api/v1/products
GET /api/v1/products?productType=DEPOSIT
GET /api/v1/products?status=ACTIVE
GET /api/v1/products?productType=DEPOSIT&status=ACTIVE
```

### Get Product By ID

```text
GET /api/v1/products/1
```

### Get Product By Code

```text
GET /api/v1/products/code/SAV-REG
```

### Update Product

`PUT /api/v1/products/1`

`productCode` is accepted for validation but ignored during update. The original product code is not changed.

```json
{
  "productCode": "IGNORED-CODE",
  "name": "Regular Savings Account Plus",
  "productType": "DEPOSIT",
  "interestRate": 3.250,
  "minBalance": 1500,
  "tenureMonths": null,
  "openingFee": 0,
  "status": "ACTIVE",
  "effectiveFrom": "2026-07-01",
  "effectiveTo": null
}
```

### Update Product Status

`PATCH /api/v1/products/1/status`

```json
{
  "status": "DISCONTINUED"
}
```

### Validation Error Example

`POST /api/v1/products`

```json
{
  "productCode": "BAD-DATE",
  "name": "Bad Date Product",
  "productType": "DEPOSIT",
  "interestRate": 1.000,
  "minBalance": 1000,
  "tenureMonths": null,
  "openingFee": 0,
  "status": "ACTIVE",
  "effectiveFrom": "2026-08-01",
  "effectiveTo": "2026-07-01"
}
```
