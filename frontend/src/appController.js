/**
 * Root controller: auth gate, shell chrome, and the oj-module host.
 *
 * The router itself lives in services/navigation.js so view models can navigate
 * without depending on this module or on Knockout's binding context.
 *
 * The auth gate lives in index.html: with no session, the login module replaces
 * the whole shell regardless of the URL.
 */
define([
  'knockout',
  'ojs/ojmodulerouter-adapter',
  './services/navigation',
  './services/session',
  './services/endpoints',
  './services/http'
], function (ko, ModuleRouterAdapter, navigation, session, endpoints, http) {
  'use strict';

  function Controller() {
    var self = this;
    var router = navigation.router;

    self.moduleAdapter = new ModuleRouterAdapter(router);
    self.moduleConfig = self.moduleAdapter.koObservableConfig;

    // Mirror the router's state into a plain observable — one subscription, and
    // it is obvious what it holds.
    self.currentState = ko.observable(null);
    router.currentState.subscribe(function (change) {
      self.currentState((change && change.state) || null);
    });

    /* ------------------------------------------------------------ session -- */

    self.sessionData = ko.observable(session.getSession());
    self.authenticated = ko.pureComputed(function () {
      var s = self.sessionData();
      return !!s && !session.isExpired(s);
    });

    // Path the user was heading for before being bounced to login.
    var intendedPath = null;

    function syncRouteAfterSignIn() {
      var target = intendedPath;
      intendedPath = null;
      if (target && target !== '/' && target.indexOf('/login') !== 0) {
        window.history.pushState(null, '', target);
      }
      var requestedPath = new URLSearchParams(window.location.search).get('ojr') || 'overview';
      if (!navigation.canAccessPath(requestedPath)) {
        // The URL adapter can reject a protected route before authentication,
        // which leaves CoreRouter with no state to transition away from. A
        // same-origin replace gives the freshly stored session a clean sync.
        window.location.replace('/?ojr=overview');
        return;
      }
      // A route module may still be mounted from the previous login in this
      // tab. Reload once so every permission flag and data request belongs to
      // the newly authenticated identity. Reading the stored session during
      // bootstrap does not notify subscribers, so this cannot loop.
      window.location.reload();
    }

    session.subscribe(function (updated) {
      self.sessionData(updated);
      if (updated && !session.isExpired(updated)) {
        syncRouteAfterSignIn();
      }
    });

    http.onAuthExpired(function () {
      intendedPath = window.location.pathname;
      self.sessionData(null);
    });

    self.onSignedIn = function () {
      self.sessionData(session.getSession());
    };

    self.signOut = function () {
      endpoints.auth
        .logout()
        .catch(function () {
          // A failed logout must not strand the user in the console. The
          // gateway caches session resolution for 30s anyway, so local state
          // is what counts.
        })
        .then(function () {
          session.clearSession();
          self.sessionData(null);
        });
    };

    /* -------------------------------------------------------------- shell -- */

    self.userName = ko.pureComputed(function () {
      var s = self.sessionData();
      return s ? s.fullName || s.username : '';
    });

    self.userMeta = ko.pureComputed(function () {
      var s = self.sessionData();
      if (!s) {
        return '';
      }
      var employee = s.employeeId ? 'Emp ' + s.employeeId : s.username;
      var branch = s.branchCode ? 'Branch ' + s.branchCode : 'No branch';
      return employee + ' · ' + branch;
    });

    self.userInitials = ko.pureComputed(function () {
      return session.initials(self.userName());
    });

    self.activePath = ko.pureComputed(function () {
      var state = self.currentState();
      return (state && state.path) || 'overview';
    });

    self.pageLabel = ko.pureComputed(function () {
      var state = self.currentState();
      return (state && state.detail && state.detail.label) || 'Overview';
    });

    // Keep the document title in step with the route.
    ko.computed(function () {
      document.title = self.pageLabel() + ' · Moneybags';
    });

    /* ---------------------------------------------------------------- nav -- */

    var SECTIONS = [
      {
        title: 'Workspace',
        items: [
          { path: 'overview', label: 'Overview', glyph: '◈', roles: ['TELLER', 'CHECKER', 'BRANCH_MANAGER', 'OPS_ADMIN'] },
          { path: 'tellerOps', label: 'Teller', glyph: '◧', permission: 'TRANSACTION_CREATE', roles: ['TELLER'] },
          { path: 'transfers', label: 'Internal transfers', glyph: '⇄', permission: 'TRANSACTION_CREATE', roles: ['TELLER'] },
          { path: 'approvals', label: 'Approvals', glyph: '✓', permission: 'TRANSACTION_APPROVE' },
          { path: 'applications', label: 'Applications', glyph: '▣', permission: 'ACCOUNT_VIEW' }
        ]
      },
      {
        title: 'Banking',
        items: [
          { path: 'customers', label: 'Customers', glyph: '◐', permission: 'CUSTOMER_READ' },
          { path: 'accounts', label: 'Accounts', glyph: '▤', permission: 'ACCOUNT_VIEW' },
          { path: 'transactions', label: 'Transactions', glyph: '⇄', permission: 'TRANSACTION_VIEW' }
        ]
      },
      {
        title: 'Back office',
        items: [
          { path: 'ledger', label: 'Ledger', glyph: '≡', roles: ['OPS_ADMIN'] },
          { path: 'products', label: 'Products', glyph: '◇', permission: 'PRODUCT_READ' },
          { path: 'branches', label: 'Branches & staff', glyph: '⌂', roles: ['BRANCH_MANAGER', 'OPS_ADMIN'] },
          { path: 'notifications', label: 'Notifications', glyph: '◔', permission: 'NOTIFICATION_MANAGE' },
          { path: 'audit', label: 'Audit trail', glyph: '◎', permission: 'AUDIT_VIEW' }
        ]
      }
    ];

    // Entries the user lacks permission for are hidden rather than disabled —
    // a teller has no use for a greyed-out ledger link. Sections that end up
    // empty are dropped so the nav has no orphan headings.
    self.navSections = ko.pureComputed(function () {
      self.sessionData(); // re-evaluate when permissions change
      return SECTIONS.map(function (section) {
        return {
          title: section.title,
          items: section.items.filter(function (item) {
            var permitted = !item.permission || session.hasPermission(item.permission);
            var roleAllowed = !item.roles || item.roles.some(function (role) { return session.hasRole(role); });
            return permitted && roleAllowed;
          })
        };
      }).filter(function (section) {
        return section.items.length > 0;
      });
    });

    self.navigate = function (path) {
      navigation.go(path);
    };

    router.sync();
  }

  return new Controller();
});
