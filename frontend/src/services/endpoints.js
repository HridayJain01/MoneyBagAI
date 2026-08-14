/**
 * Endpoint bindings, grouped by owning service.
 *
 * Only paths routed publicly by the gateway appear here. /internal/** is
 * rejected with 403 INTERNAL_ROUTE_BLOCKED by design, so it is never called —
 * which is also why there is no card data in this console: linked cards are
 * reachable only through an internal route.
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
      logout: function () {
        return api.post('/api/v1/auth/logout');
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
      beneficiaries: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo) + '/beneficiaries');
      },
      kycDocuments: function (cifNo) {
        return api.get('/api/v1/customers/' + enc(cifNo) + '/kyc-documents');
      }
    },

    /* -------------------------------------------------------- accounts --- */
    accounts: {
      search: function (query) {
        return api.get('/api/v1/accounts', { query: query });
      },
      get: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId));
      },
      balance: function (accountId) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/balance');
      },
      balanceHistory: function (accountId, query) {
        return api.get('/api/v1/accounts/' + enc(accountId) + '/balance-history', { query: query });
      },
      applications: function (query) {
        return api.get('/api/v1/accounts/applications', { query: query });
      },

      /* Servicing actions. All are writes and carry an intent-scoped key. */
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
      }
    },

    /* ---------------------------------------------------- transactions --- */
    transactions: {
      /** Supports account, reference, status, rail, transactionType, min/maxAmount, from, to, createdBy. */
      search: function (query) {
        return api.get('/api/v1/transactions', { query: query });
      },
      get: function (id) {
        return api.get('/api/v1/transactions/' + enc(id));
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
      list: function () {
        return api.get('/api/v1/products');
      }
    },

    branches: {
      list: function (query) {
        return api.get('/api/v1/branches', { query: query });
      },
      employees: function (query) {
        return api.get('/api/v1/employees', { query: query });
      }
    },

    /* --------------------------------------- audit / notifications ------- */
    audit: {
      events: function (query) {
        return api.get('/api/v1/audit-events', { query: query });
      },
      forAccount: function (accountId, query) {
        return api.get('/api/v1/audit-events/accounts/' + enc(accountId), { query: query });
      }
    },

    notifications: {
      list: function (query) {
        return api.get('/api/v1/notifications', { query: query });
      }
    }
  };
});
