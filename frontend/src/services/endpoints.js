/**
 * Endpoint bindings, grouped by owning service.
 *
 * Only paths routed publicly by the gateway appear here. /internal/** is
 * rejected with 403 INTERNAL_ROUTE_BLOCKED by design, so it is never called —
 * which is also why there is no card data in this console: linked cards are
 * reachable only through an internal route.
 *
 * This file is the map of the API surface. It is deliberately complete rather
 * than minimal: a few bindings below have no caller yet, and are kept because
 * the next person reading this should see the whole contract in one place.
 * Where the backend disagrees with docs/04-API-REFERENCE.md, the comment records
 * what the server actually does.
 */
define(['./http'], function (api) {
  'use strict';

  function enc(value) {
    return encodeURIComponent(value);
  }

  return {
    /* ------------------------------------------------------------ auth --- */
    auth: {
      login: function (body) {
        return api.post('/api/v1/auth/login', { body: body });
      },
      register: function (body, key) {
        return api.post('/api/v1/auth/register', { body: body, idempotencyKey: key });
      },
      logout: function () {
        return api.post('/api/v1/auth/logout');
      }
    },

    /* ------------------------------------------ identity administration --- */
    identity: {
      users: function (query) {
        return api.get('/api/v1/users', { query: query });
      },
      createUser: function (body, key) {
        return api.post('/api/v1/users', { body: body, idempotencyKey: key });
      },
      roles: function () {
        return api.get('/api/v1/roles');
      },
      permissions: function () {
        return api.get('/api/v1/permissions');
      },
      createRole: function (body, key) {
        return api.post('/api/v1/roles', { body: body, idempotencyKey: key });
      },
      updateRole: function (roleId, body, key) {
        return api.put('/api/v1/roles/' + enc(roleId), { body: body, idempotencyKey: key });
      },
      replaceRolePermissions: function (roleId, permissionIds, key) {
        return api.put('/api/v1/admin/role-permissions/' + enc(roleId), {
          body: { permissionIds: permissionIds }, idempotencyKey: key
        });
      },
      replaceUserRole: function (userId, role, key) {
        return api.put('/api/v1/admin/users/' + enc(userId) + '/role', {
          body: { role: role }, idempotencyKey: key
        });
      },
      enableUser: function (userId, key) {
        return api.post('/api/v1/users/' + enc(userId) + '/enable', { idempotencyKey: key });
      },
      disableUser: function (userId, key) {
        return api.post('/api/v1/users/' + enc(userId) + '/disable', { idempotencyKey: key });
      }
    },

    /* ------------------------------------------------------- customers --- */
    customers: {
      /**
       * `query` is required and the response is a bare List — customer-service
       * offers no paging or sorting on search, so the UI must not imply either.
       */
      search: function (query) {
        return api.get('/api/v1/customers/search', { query: { query: query } });
      },
      list: function () {
        return api.get('/api/v1/customers');
      },
      create: function (body, key) {
        return api.post('/api/v1/customers', { body: body, idempotencyKey: key });
      },
      get: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo));
      },
      summary: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo) + '/summary');
      },
      completeness: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo) + '/completeness');
      },
      addresses: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo) + '/addresses');
      },
      addAddress: function (cifNo, body, key) {
        return api.put('/api/v1/customers/' + enc(cifNo) + '/addresses', { body: body, idempotencyKey: key });
      },
      kycDocuments: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo) + '/kyc-documents');
      },

      /**
       * The gate for account opening. Returns
       *   { cifNo, eligible, customerStatus, kycStatus, riskClassification,
       *     adult, residentAddressAvailable, reasons[] }
       *
       * Eligibility needs ACTIVE status AND verified KYC AND age >= 18 AND an
       * Indian resident address. Render `reasons[]` — inferring the refusal from
       * kycStatus alone is wrong three times out of four. Note the field is
       * `customerStatus`, not `status`.
       */
      eligibility: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo) + '/eligibility');
      }

      /* No `beneficiaries` binding. The tables are migrated and seeded and the
         JPA entity exists, but no controller does, and transaction-service
         dropped beneficiary_id in V2. External transfers carry an account number
         directly. */
    },

    /* -------------------------------------------------------- accounts --- */
    accounts: {
      search: function (query) {
        return api.get('/api/v1/accounts', { query: query });
      },
      get: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId));
      },
      /** Branch-scoped: 403 for an account outside the caller's own branch. */
      byNumber: function (accountNumber) {
        return api.get('/api/v1/accounts/by-number/' + enc(accountNumber));
      },
      balance: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/balance');
      },
      balanceHistory: function (accountId, query) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/balance-history', { query: query });
      },

      /* --- Applications ----------------------------------------------------
         account-service declares no Idempotency-Key on any write. http.js sends
         one regardless, which is harmless — but do not design around it. */
      applications: function (query) {
        return api.get('/api/v1/accounts/applications', { query: query });
      },
      application: function (applicationId) {
        return api.get('/api/v1/accounts/applications/' + enc(applicationId));
      },
      createApplication: function (body, key) {
        return api.post('/api/v1/accounts/applications', { body: body, idempotencyKey: key });
      },
      /** `remarks` optional. Refused when the caller is the maker. */
      approveApplication: function (applicationId, body, key) {
        return api.post('/api/v1/accounts/applications/' + enc(applicationId) + '/approve',
                        { body: body, idempotencyKey: key });
      },
      /** `reason` is @NotBlank on the server. */
      rejectApplication: function (applicationId, body, key) {
        return api.post('/api/v1/accounts/applications/' + enc(applicationId) + '/reject',
                        { body: body, idempotencyKey: key });
      },
      /** Maker only — gated on identity, not on a permission. No body. */
      cancelApplication: function (applicationId, key) {
        return api.post('/api/v1/accounts/applications/' + enc(applicationId) + '/cancel',
                        { idempotencyKey: key });
      },

      /* --- Servicing. All writes, all carry an intent-scoped key. --------- */
      freeze: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/freeze', { body: body, idempotencyKey: key });
      },
      unfreeze: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/unfreeze', { body: body, idempotencyKey: key });
      },
      block: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/block', { body: body, idempotencyKey: key });
      },
      unblock: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/unblock', { body: body, idempotencyKey: key });
      },
      markDormant: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/mark-dormant', { body: body, idempotencyKey: key });
      },
      reactivate: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/reactivate', { body: body, idempotencyKey: key });
      },
      /** 422 BALANCE_NOT_SETTLED / HOLDS_OUTSTANDING unless both are exactly zero. */
      close: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/close', { body: body, idempotencyKey: key });
      },

      /* --- Bare Lists, not page envelopes. -------------------------------- */
      statusHistory: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/status-history');
      },
      holders: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/holders');
      },
      /** { cifNo, holderRole } where holderRole is PRIMARY or JOINT. */
      addHolder: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/holders', { body: body, idempotencyKey: key });
      },
      holds: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/holds');
      },
      /**
       * { amount, reason, holdType } where holdType is LIEN or MANUAL.
       * TRANSACTION holds are placed by transaction-service, never from here.
       */
      placeHold: function (accountId, body, key) {
        return api.post('/api/v1/accounts/' + enc(accountId) + '/holds', { body: body, idempotencyKey: key });
      },
      limits: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/limits');
      },
      setLimits: function (accountId, body, key) {
        return api.put('/api/v1/accounts/' + enc(accountId) + '/limits', { body: body, idempotencyKey: key });
      },
      ownedProducts: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/products');
      }
    },

    /* ---------------------------------------------------- transactions --- */
    transactions: {
      /** Supports account, reference, status, rail, transactionType, min/maxAmount, from, to, createdBy. */
      search: function (query) {
        return api.get('/api/v1/transactions', { query: query });
      },
      /** Deep view: legs, hold, journals with lines, clearing, rail details, history. */
      get: function (id) {
        return api.get('/api/v1/transactions/' + enc(id));
      },
      /**
       * Lightweight { transactionId, transactionReference, status, updatedAt }.
       * Poll THIS, never get() — the deep view is a great deal of payload to
       * refetch on a timer just to read one field.
       */
      status: function (id) {
        return api.get('/api/v1/transactions/' + enc(id) + '/status');
      },
      byReference: function (reference) {
        return api.get('/api/v1/transactions/by-reference/' + enc(reference));
      },
      forAccount: function (accountId, query) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/transactions', { query: query });
      },
      /** Bare List, not a page. */
      miniStatement: function (accountId, size) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/mini-statement', {
          query: { size: size || 10 }
        });
      },
      approvals: function (query) {
        return api.get('/api/v1/transactions/approvals', { query: query });
      },

      /**
       * Pre-flight limit check.
       * { accountId, transactionType, rail, channel, currency, amount }
       *
       * Two things must match the server or the quote quietly lies:
       *   - accountId is the DESTINATION for DEPOSIT and CHEQUE, the source for
       *     everything else. That is the account the orchestrator itself quotes.
       *   - amount must include the fee; the server validates amount + fee.
       *
       * Only ONE limit rule is seeded (RTGS/INR, min 200000, approval >= 1000000).
       * Every other rail answers allowed:true with all bounds null, so a bounds
       * panel is only worth rendering when something is non-null.
       */
      limitQuote: function (query) {
        return api.get('/api/v1/transactions/limits/quote', { query: query });
      },

      /* --- Creation --------------------------------------------------------
         All require an intent-scoped Idempotency-Key: the header is declared
         without required=false, so a missing key is a 400 before any business
         logic runs. All return 201.

         paymentMethod is NOT a free choice. validateShape() requires
         method.name() == rail.name(), with exactly two exceptions:
         rail INTERNAL takes method ACCOUNT, rail CASH takes method CASH. Each
         endpoint hard-codes its own rail, so the method follows from the
         endpoint. services/txn.js owns that mapping — build bodies there. */
      deposit: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/deposits',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },
      withdrawal: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/withdrawals',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },
      internalTransfer: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/transfers/internal',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },
      neft: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/transfers/neft',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },
      rtgs: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/transfers/rtgs',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },
      imps: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/transfers/imps',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },
      upi: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/transfers/upi',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },
      cheque: function (body, key, correlationId) {
        return api.post('/api/v1/transactions/cheques',
                        { body: body, idempotencyKey: key, correlationId: correlationId });
      },

      /* No cardPayment binding. CARD_PAYMENT needs a cardId, and card context is
         served only from /internal/v1/cards/{cardId}/payment-context, which the
         gateway blocks. A teller has no way to obtain one. */

      /** No request body — the endpoint takes only the idempotency key. */
      approve: function (id, key) {
        return api.post('/api/v1/transactions/' + enc(id) + '/approve', { idempotencyKey: key });
      },
      /** `reason` is @NotBlank on the server. */
      reject: function (id, body, key) {
        return api.post('/api/v1/transactions/' + enc(id) + '/reject', { body: body, idempotencyKey: key });
      },
      cancel: function (id, body, key) {
        return api.post('/api/v1/transactions/' + enc(id) + '/cancel', { body: body, idempotencyKey: key });
      },
      /**
       * 201 with a NEW linked transaction. The original is never mutated — it
       * moves to REVERSED and keeps its own record. Navigate to the response
       * rather than patching the view you are looking at.
       */
      reverse: function (id, body, key) {
        return api.post('/api/v1/transactions/' + enc(id) + '/reversals', { body: body, idempotencyKey: key });
      }
    },

    /* ---------------------------------------------------------- ledger --- */
    ledger: {
      journals: function (query) {
        return api.get('/api/v1/ledger/journals', { query: query });
      },
      accounts: function () {
        return api.get('/api/v1/ledger/accounts');
      },
      entriesFor: function (accountId, query) {
        return api.get('/api/v1/ledger/customer-accounts/' + enc(accountId) + '/entries', { query: query });
      }
    },

    /* ------------------------------------------------------ statements --- */
    statements: {
      requests: function (query) {
        return api.get('/api/v1/statements/requests', { query: query });
      },
      dailyTransactions: function (date) {
        return api.get('/api/v1/reports/daily-transactions', { query: { date: date } });
      },
      dormantAccounts: function () {
        return api.get('/api/v1/reports/dormant-accounts');
      }
    },

    /* --------------------------------------------- products / branches --- */
    products: {
      /**
       * Bare List. Filters are `status` and `productType` — NOT `type`, which
       * docs/04 gets wrong. ProductDetail carries requiresFunding,
       * minOpeningDeposit, minBalance and tenureMonths, so the opening form can
       * validate client-side without the blocked /internal effective-terms route.
       */
      list: function (query) {
        return api.get('/api/v1/products', { query: query });
      },
      get: function (productCode) {
        return api.get('/api/v1/products/' + enc(productCode));
      },
      versions: function (productCode) {
        return api.get('/api/v1/products/' + enc(productCode) + '/versions');
      },
      create: function (body, key) {
        return api.post('/api/v1/products', { body: body, idempotencyKey: key });
      },
      update: function (productCode, body, key) {
        return api.patch('/api/v1/products/' + enc(productCode), { body: body, idempotencyKey: key });
      },
      activate: function (productCode, key) {
        return api.post('/api/v1/products/' + enc(productCode) + '/activate', { idempotencyKey: key });
      },
      deactivate: function (productCode, key) {
        return api.post('/api/v1/products/' + enc(productCode) + '/deactivate', { idempotencyKey: key });
      }
    },

    branches: {
      list: function (query) {
        return api.get('/api/v1/branches', { query: query });
      },
      create: function (body, key) {
        return api.post('/api/v1/branches', { body: body, idempotencyKey: key });
      },
      update: function (branchId, body, key) {
        return api.patch('/api/v1/branches/' + enc(branchId), { body: body, idempotencyKey: key });
      },
      activate: function (branchId, key) {
        return api.post('/api/v1/branches/' + enc(branchId) + '/activate', { idempotencyKey: key });
      },
      deactivate: function (branchId, key) {
        return api.post('/api/v1/branches/' + enc(branchId) + '/deactivate', { idempotencyKey: key });
      },
      workingHours: function (branchId) {
        return api.get('/api/v1/branches/' + enc(branchId) + '/working-hours');
      },
      replaceWorkingHours: function (branchId, body, key) {
        return api.put('/api/v1/branches/' + enc(branchId) + '/working-hours', { body: body, idempotencyKey: key });
      },
      holidays: function (branchId) {
        return api.get('/api/v1/branches/' + enc(branchId) + '/holidays');
      },
      addHoliday: function (branchId, body, key) {
        return api.post('/api/v1/branches/' + enc(branchId) + '/holidays', { body: body, idempotencyKey: key });
      },
      deleteHoliday: function (branchId, holidayId, key) {
        return api.del('/api/v1/branches/' + enc(branchId) + '/holidays/' + enc(holidayId), { idempotencyKey: key });
      },
      employees: function (query) {
        return api.get('/api/v1/employees', { query: query });
      },
      employee: function (employeeId) {
        return api.get('/api/v1/employees/' + enc(employeeId));
      },
      createEmployee: function (body, key) {
        return api.post('/api/v1/employees', { body: body, idempotencyKey: key });
      },
      updateEmployee: function (employeeId, body, key) {
        return api.patch('/api/v1/employees/' + enc(employeeId), { body: body, idempotencyKey: key });
      },
      updateManager: function (employeeId, managerId, key) {
        return api.put('/api/v1/employees/' + enc(employeeId) + '/manager', {
          body: { reportingManagerId: managerId }, idempotencyKey: key
        });
      },
      transferEmployee: function (employeeId, body, key) {
        return api.post('/api/v1/employees/' + enc(employeeId) + '/transfer', { body: body, idempotencyKey: key });
      },
      approvalAuthority: function (employeeId) {
        return api.get('/api/v1/employees/' + enc(employeeId) + '/approval-authority');
      },
      replaceApprovalAuthority: function (employeeId, body, key) {
        return api.put('/api/v1/employees/' + enc(employeeId) + '/approval-authority', { body: body, idempotencyKey: key });
      }
    },

    /* --------------------------------------- audit / notifications ------- */
    audit: {
      events: function (query) {
        return api.get('/api/v1/audit-events', { query: query });
      },
      forAccount: function (accountId, query) {
        return api.get('/api/v1/audit-events/accounts/' + enc(accountId), { query: query });
      },
      forTransaction: function (transactionId, query) {
        return api.get('/api/v1/audit-events/transactions/' + enc(transactionId), { query: query });
      },
      /** Reconstructs one user intent across every service it touched. */
      trace: function (correlationId) {
        return api.get('/api/v1/audit-events/trace/' + enc(correlationId));
      }
    },

    notifications: {
      list: function (query) {
        return api.get('/api/v1/notifications', { query: query });
      },
      templates: function () {
        return api.get('/api/v1/notification-templates');
      }
    }
  };
});
