/**
 * The router, and the only place that knows how to reach a screen.
 *
 * This lives apart from appController so view models can navigate without
 * reaching through Knockout's binding context. Inside an oj-table cell
 * template `$root` is not the app controller, so `$root.openAccount(...)`
 * silently does nothing — requiring this module instead is unambiguous.
 *
 * Routing state lives in the query string (?ojr=accountDetail&id=ACC001) via
 * UrlParamAdapter. That is deliberate over path-style URLs: parameters survive
 * a cold load without the route having to encode them positionally, and the
 * server needs no index.html fallback because every URL is still "/". The
 * dev-server fallback in before_serve.js remains as a safety net.
 */
define(['ojs/ojcorerouter', 'ojs/ojurlparamadapter', './session'], function (
  CoreRouter,
  UrlParamAdapter,
  session
) {
  'use strict';

  // Permission strings gate both the route and its nav entry, and must match
  // the seeded values in the identity schema.
  var routes = [
    { path: '', redirect: 'overview' },
    { path: 'overview', detail: { label: 'Overview', roles: ['TELLER', 'CHECKER', 'BRANCH_MANAGER', 'OPS_ADMIN'] } },
    { path: 'tellerOps', detail: { label: 'Teller operations', permission: 'TRANSACTION_CREATE', roles: ['TELLER'] } },
    { path: 'transfers', detail: { label: 'Internal transfers', permission: 'TRANSACTION_CREATE', roles: ['TELLER'] } },
    { path: 'approvals', detail: { label: 'Approvals', permission: 'TRANSACTION_APPROVE', excludedRoles: ['OPS_ADMIN'] } },
    { path: 'openAccount', detail: { label: 'Open account', permission: 'ACCOUNT_OPEN' } },
    { path: 'applications', detail: { label: 'Account applications', permission: 'ACCOUNT_VIEW', excludedRoles: ['OPS_ADMIN'] } },
    { path: 'customers', detail: { label: 'Customers', permission: 'CUSTOMER_READ', excludedRoles: ['OPS_ADMIN'] } },
    { path: 'customerDetail', detail: { label: 'Customer', permission: 'CUSTOMER_READ', excludedRoles: ['OPS_ADMIN'] } },
    { path: 'accounts', detail: { label: 'Accounts', permission: 'ACCOUNT_VIEW', excludedRoles: ['OPS_ADMIN'] } },
    { path: 'accountDetail', detail: { label: 'Account', permission: 'ACCOUNT_VIEW', excludedRoles: ['OPS_ADMIN'] } },
    { path: 'transactions', detail: { label: 'Transactions', permission: 'TRANSACTION_VIEW', excludedRoles: ['TELLER', 'CHECKER', 'OPS_ADMIN'] } },
    { path: 'transactionDetail', detail: { label: 'Transaction', permission: 'TRANSACTION_VIEW', excludedRoles: ['OPS_ADMIN'] } },
    { path: 'eod', detail: { label: 'End of day', permission: 'REPORT_VIEW', roles: ['BRANCH_MANAGER'] } },
    { path: 'ledger', detail: { label: 'Ledger', roles: ['OPS_ADMIN'] } },
    { path: 'products', detail: { label: 'Products', permission: 'PRODUCT_READ' } },
    { path: 'branches', detail: { label: 'Branches & staff', roles: ['BRANCH_MANAGER', 'OPS_ADMIN'] } },
    { path: 'notifications', detail: { label: 'Notifications', permission: 'NOTIFICATION_MANAGE' } },
    { path: 'audit', detail: { label: 'Audit trail', permission: 'AUDIT_VIEW' } }
  ];

  var router = new CoreRouter(routes, { urlAdapter: new UrlParamAdapter() });

  function isAllowed(detail) {
    var required = detail && detail.permission;
    var roles = (detail && detail.roles) || [];
    var excludedRoles = (detail && detail.excludedRoles) || [];
    return (!required || session.hasPermission(required)) &&
      (!roles.length || roles.some(function (role) { return session.hasRole(role); })) &&
      !excludedRoles.some(function (role) { return session.hasRole(role); });
  }

  // Client-side gate. Cosmetic only — the gateway is the real authority, so
  // every screen still handles a 403 from any call.
  router.beforeStateChange.subscribe(function (args) {
    var detail = args.state && args.state.detail;
    if (!isAllowed(detail)) {
      args.accept(Promise.reject(new Error('This route is not available for the current role.')));
    }
  });

  // Track the live route state. ModuleRouterAdapter does not forward router
  // params into the module's config, so detail screens read them from here
  // rather than from their own view-model context.
  var state = null;
  router.currentState.subscribe(function (change) {
    state = (change && change.state) || null;
  });

  /** Value of a route parameter, e.g. param('id') on accountDetail. */
  function param(name) {
    return (state && state.params && state.params[name]) || null;
  }

  function go(path, params) {
    return router.go({ path: path, params: params || {} }).catch(function () {
      // beforeStateChange rejects transitions the user cannot make.
    });
  }

  return {
    router: router,
    go: go,
    param: param,
    canAccessPath: function (path) {
      var route = routes.find(function (candidate) { return candidate.path === path; });
      return !!route && isAllowed(route.detail || {});
    },
    openAccount: function (accountId) {
      return go('accountDetail', { id: accountId });
    },
    openCustomer: function (cifNo) {
      return go('customerDetail', { cif: cifNo });
    },
    startAccountOpening: function (cifNo) {
      return go('openAccount', cifNo ? { cif: cifNo } : {});
    },
    openApplications: function () {
      return go('applications');
    },
    openTransaction: function (transactionId) {
      return go('transactionDetail', { id: transactionId });
    }
  };
});
