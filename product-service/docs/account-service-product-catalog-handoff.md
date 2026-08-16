# Product Catalog Handoff for Account Service

## Summary

Product Service now exposes a versioned product catalog. Account Service should store both identifiers returned by Product Service:

```text
id                 -> accounts.product_id
productVersionId   -> accounts.product_version_id
```

Do not infer the version in Account Service. Product Service resolves the current product version and returns a clean `terms` object.

## Database Model

The product catalog is now split into four tables:

```text
products
product_versions
product_attribute_definitions
product_version_attributes
```

`products` stores the stable product family:

```text
id
product_code
name
product_category
product_type
created_at
updated_at
```

`product_versions` stores lifecycle/version details:

```text
id
product_id
version_number
status
effective_from
effective_to
created_at
updated_at
```

`product_attribute_definitions` defines supported terms such as `INTEREST_RATE`, `MIN_BALANCE`, and `OPENING_FEE`.

`product_version_attributes` stores the actual term values for a specific product version.

## APIs Account Service Should Use

Base URL:

```text
http://localhost:8083/api/v1/products
```

Useful endpoints:

```text
GET /api/v1/products?status=ACTIVE
GET /api/v1/products/{id}
GET /api/v1/products/code/{productCode}
GET /api/v1/products/versions/{productVersionId}
```

For account opening, the usual call should be:

```text
GET /api/v1/products?productCategory=DEPOSIT&status=ACTIVE
```

For CASA specifically:

```text
GET /api/v1/products?productCategory=DEPOSIT&productType=SAVINGS&status=ACTIVE
GET /api/v1/products?productCategory=DEPOSIT&productType=CURRENT&status=ACTIVE
```

## Response Contract

Every `ProductResponse` includes:

```json
{
  "id": 1,
  "productCode": "SAV-REG",
  "name": "Regular Savings Account",
  "productCategory": "DEPOSIT",
  "productType": "SAVINGS",
  "productVersionId": 101,
  "version": 1,
  "status": "ACTIVE",
  "effectiveFrom": "2026-07-01T00:00:00",
  "effectiveTo": null,
  "createdAt": "2026-07-01T09:00:00",
  "updatedAt": "2026-08-01T09:00:00",
  "terms": {
    "interestRate": 3,
    "minBalance": 1000,
    "openingFee": 0
  }
}
```

Important fields for Account Service:

```text
id
productVersionId
productCode
productCategory
productType
status
terms
```

The `terms` keys are camelCase. Product Service converts internal DB attribute codes like `INTEREST_RATE` into response keys like `interestRate`.

## CASA Examples

Savings product:

```json
{
  "id": 1,
  "productCode": "SAV-REG",
  "name": "Regular Savings Account",
  "productCategory": "DEPOSIT",
  "productType": "SAVINGS",
  "productVersionId": 101,
  "version": 1,
  "status": "ACTIVE",
  "effectiveFrom": "2026-07-01T00:00:00",
  "effectiveTo": null,
  "terms": {
    "interestRate": 3,
    "minBalance": 1000,
    "openingFee": 0
  }
}
```

Current account product:

```json
{
  "id": 3,
  "productCode": "CUR-BASIC",
  "name": "Basic Current Account",
  "productCategory": "DEPOSIT",
  "productType": "CURRENT",
  "productVersionId": 301,
  "version": 1,
  "status": "ACTIVE",
  "effectiveFrom": "2026-07-01T00:00:00",
  "effectiveTo": null,
  "terms": {
    "interestRate": 0,
    "minBalance": 10000,
    "openingFee": 500,
    "cashHandling": "BRANCH_ONLY"
  }
}
```

## Account Opening Usage

When Account Service opens an account, it should copy these values from Product Service:

```text
ProductResponse.id                -> accounts.product_id
ProductResponse.productVersionId  -> accounts.product_version_id
```

Example:

```json
{
  "product_id": 1,
  "product_version_id": 101,
  "account_type": "SAVINGS"
}
```

This preserves product history. If Product Service later creates version `102` for `SAV-REG`, existing accounts can still point to version `101`.

## Versioning Behavior

When Product Service updates a product:

```text
products.id stays the same
new product_versions.id is created
old product_version_attributes are not overwritten
new product_version_attributes are inserted for the new version
```

Example:

```text
Before update:
product_id = 1
product_version_id = 101
version = 1
status = ACTIVE

After update:
product_id = 1
product_version_id = 102
version = 2
status = ACTIVE
```

Existing accounts remain linked to:

```text
product_id = 1
product_version_id = 101
```

New accounts should use:

```text
product_id = 1
product_version_id = 102
```

## Status Notes

For available products, Account Service should request:

```text
GET /api/v1/products?status=ACTIVE
```

`DISCONTINUED` products may still be returned by direct product lookup or unfiltered catalog lookup for history/admin use, but Account Service should not offer them for new account opening.

## Expected CASA Terms

For `SAVINGS`, Account Service can expect terms like:

```text
interestRate
minBalance
openingFee
```

For `CURRENT`, Account Service can expect terms like:

```text
interestRate
minBalance
openingFee
cashHandling
```

Treat `terms` as product-specific. Do not hard-code Product Service database attribute tables in Account Service.
