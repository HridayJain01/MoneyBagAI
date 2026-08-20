/**
 * Overview — the branch-day dashboard.
 *
 * Every tile here is backed by a real endpoint. Tiles from consumer-fintech
 * mockups that have nothing behind them in this backend (cards, FX,
 * investments, rewards, revenue) are deliberately absent: linked cards exist
 * only behind /internal/**, which the gateway blocks by design.
 *
 * The rail breakdown is aggregated client-side over one page of the approval
 * queue, and is labelled as such — there is no server-side aggregation endpoint.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/format',
  '../services/http',
  '../services/session',
  '../services/navigation',
  'ojs/ojarraydataprovider',
  'ojs/ojchart',
  'ojs/ojbutton'
], function (ko, endpoints, fmt, http, session, navigation, ArrayDataProvider) {
  'use strict';

  var QUEUE_SAMPLE = 100;

  function OverviewViewModel() {
    var self = this;
    var identity = session.getSession() || {};
    var isTeller = session.hasRole('TELLER');
    var isChecker = session.hasRole('CHECKER');
    var isManager = session.hasRole('BRANCH_MANAGER');
    var isAdmin = session.hasRole('OPS_ADMIN');

    self.showQueue = (isChecker || isManager) && session.hasPermission('TRANSACTION_APPROVE');
    self.showGlobalTransactions = isManager && session.hasPermission('TRANSACTION_VIEW');
    self.showApplications = (isChecker || isManager) && session.hasPermission('ACCOUNT_VIEW');
    self.showRoleHome = !self.showQueue && !self.showGlobalTransactions && !self.showApplications;
    self.isTeller = isTeller;
    self.isChecker = isChecker;
    self.isAdmin = isAdmin;
    self.roleTitle = isTeller ? 'Teller workspace' : isAdmin ? 'Administration workspace' : 'Branch workspace';
    self.roleMessage = isTeller
      ? 'Start a counter transaction or an internal account transfer.'
      : isAdmin
        ? 'Manage products, branches, staff and operational controls.'
        : 'Your permitted branch operations are ready.';

    self.loading = ko.observable(true);
    self.error = ko.observable(null);
    self.hasError = ko.pureComputed(function () {
      return !!self.error();
    });

    self.queueCount = ko.observable(0);
    self.queueValue = ko.observable(0);
    self.queueSampled = ko.observable(false);
    self.sampleSize = ko.observable(0);
    self.todayCount = ko.observable(null);
    self.applicationCount = ko.observable(null);
    self.recent = ko.observableArray([]);
    self.railSeries = ko.observableArray([]);
    self.railGroups = ko.observableArray(['Pending']);

    self.cashPositionAmount = ko.observable(null);
    self.cashPositionDisplay = ko.pureComputed(function () {
      return fmt.money(self.cashPositionAmount(), 'INR');
    });
    self.branchDetail = ko.observable(identity.branchCode ? 'Branch ' + identity.branchCode : '—');
    self.cashVariance = ko.observable('₹0.00');
    self.cashVarianceTone = ko.observable('Always zero');
    self.lastUpdated = ko.observable('—');
    self.kycPendingCount = ko.observable(null);

    self.transactionsTodayDisplay = ko.pureComputed(function () {
      return self.todayCount() === null ? '—' : self.todayCount();
    });
    self.pendingReviewDisplay = ko.pureComputed(function () {
      return self.kycPendingCount() === null ? '—' : self.kycPendingCount();
    });
    self.pendingReviewHint = ko.pureComputed(function () {
      if (isTeller) {
        return 'KYC waiting for teller submission';
      }
      if (isChecker) {
        return 'KYC submitted for checker review';
      }
      return 'No role-specific KYC queue';
    });
    self.dashboardRecent = ko.pureComputed(function () {
      return self.recent();
    });

    self.heroValue = ko.pureComputed(function () {
      return fmt.amount(self.queueValue());
    });

    self.queueHint = ko.pureComputed(function () {
      return self.queueSampled()
        ? 'Value shown covers the oldest ' + self.sampleSize()
        : 'Full queue valued';
    });

    self.todayDisplay = ko.pureComputed(function () {
      var value = self.todayCount();
      return value === null ? '—' : value;
    });

    self.applicationDisplay = ko.pureComputed(function () {
      var value = self.applicationCount();
      return value === null ? '—' : value;
    });

    self.recentEmpty = ko.pureComputed(function () {
      return !self.loading() && self.recent().length === 0;
    });

    self.railEmpty = ko.pureComputed(function () {
      return !self.loading() && self.railSeries().length === 0;
    });

    // Chart colours must be set in code: CSS variables do not reach chart marks.
    self.chartColors = ['#6d4aff', '#3d7dff', '#ff5fa2', '#0f9d6b', '#b7791f', '#7b8496'];
    self.chartStyleDefaults = { colors: self.chartColors, barGapRatio: 0.4 };

    self.goToApprovals = function () {
      navigation.go('approvals');
    };
    self.goTo = function (path) { navigation.go(path); };
    self.newSelfTransfer = function () { navigation.go('tellerOps'); };
    self.newInternalTransfer = function () { navigation.go('transfers'); };
    self.findCustomer = function () { navigation.go('customers'); };

    function loadQueue() {
      return endpoints.transactions.approvals({ page: 0, size: QUEUE_SAMPLE }).then(function (page) {
        var items = (page && page.content) || [];
        var total = (page && page.totalElements) || 0;

        self.queueCount(total);
        self.sampleSize(items.length);
        self.queueSampled(total > items.length);
        self.queueValue(
          items.reduce(function (sum, tx) {
            return sum + (tx.amount || 0);
          }, 0)
        );

        var counts = {};
        items.forEach(function (tx) {
          var key = tx.rail || 'UNKNOWN';
          counts[key] = (counts[key] || 0) + 1;
        });

        // oj-chart's series/groups form: one series per rail, one shared group.
        self.railSeries(
          Object.keys(counts)
            .sort(function (a, b) {
              return counts[b] - counts[a];
            })
            .map(function (rail) {
              return { name: rail, items: [counts[rail]] };
            })
        );
      });
    }

    function tellerScopedQuery(query) {
      if (isTeller && identity.employeeId) {
        query.createdBy = identity.employeeId;
      }
      return query;
    }

    function loadToday() {
      return endpoints.transactions
        .search(tellerScopedQuery({ from: fmt.startOfTodayIso(), to: fmt.endOfTodayIso(), page: 0, size: 1 }))
        .then(function (page) {
          self.todayCount(page && typeof page.totalElements === 'number' ? page.totalElements : null);
        });
    }

    function loadRecent() {
      return endpoints.transactions.search(tellerScopedQuery({ page: 0, size: 8 })).then(function (page) {
        var rows = (page && page.content) || [];
        self.recent(
          rows.map(function (tx) {
            var dir = fmt.direction(tx);
            return {
              id: tx.transactionId,
              title: tx.narration || fmt.humanize(tx.type),
              reference: tx.transactionReference,
              when: fmt.relativeDateTime(tx.createdAt),
              amount: (dir === 'credit' ? '+' : '−') + fmt.money(Math.abs(tx.amount), tx.currency),
              status: tx.status,
              statusTone: 'mb-pill mb-pill--' + fmt.toneFor(tx.status),
              glyphClass: 'mb-glyph mb-glyph--' + dir,
              moneyClass: 'mb-money mb-money--' + dir,
              arrow: dir === 'credit' ? '↓' : '↑',
              customer: tx.customerName || tx.cifNo || tx.accountName || 'Branch customer',
              rail: fmt.humanize(tx.rail || tx.type || 'transaction')
            };
          })
        );
      });
    }

    function collectTransactions(query) {
      var rows = [];
      function fetchPage(pageNumber) {
        var pageQuery = Object.assign({}, query, { page: pageNumber, size: 100 });
        return endpoints.transactions.search(pageQuery).then(function (page) {
          var content = (page && page.content) || [];
          rows = rows.concat(content);
          var totalPages = page && typeof page.totalPages === 'number' ? page.totalPages : 0;
          return pageNumber + 1 < totalPages ? fetchPage(pageNumber + 1) : rows;
        });
      }
      return fetchPage(0);
    }

    function loadCashPosition() {
      if (!isTeller || !identity.employeeId) {
        return Promise.resolve();
      }
      return collectTransactions({
        from: fmt.startOfTodayIso(),
        to: fmt.endOfTodayIso(),
        createdBy: identity.employeeId,
        rail: 'CASH',
        transactionType: 'DEPOSIT',
        status: 'COMPLETED'
      }).then(function (rows) {
        if (!rows.length) {
          self.cashPositionAmount(null);
          self.lastUpdated('—');
          return;
        }
        self.cashPositionAmount(rows.reduce(function (sum, tx) {
          return sum + Number(tx.amount || 0);
        }, 0));
        var latest = rows.reduce(function (current, tx) {
          var timestamp = tx.completedAt || tx.updatedAt || tx.createdAt;
          return !current || new Date(timestamp) > new Date(current) ? timestamp : current;
        }, null);
        self.lastUpdated(latest ? fmt.dateTime(latest) : '—');
      });
    }

    function loadKycPending() {
      if (!isTeller && !isChecker) {
        return Promise.resolve();
      }
      return endpoints.customers.list().then(function (customers) {
        var pendingCustomers = (customers || []).filter(function (customer) {
          return customer.kycStatus === 'PENDING';
        });
        return Promise.all(pendingCustomers.map(function (customer) {
          return endpoints.kyc.pendingSessions(customer.cifNo).then(function (sessions) {
            var submitted = (sessions || []).some(function (item) {
              return item.status === 'VERIFICATION_IN_PROGRESS';
            });
            return isChecker ? submitted : !submitted;
          });
        }));
      }).then(function (matches) {
        self.kycPendingCount(matches.filter(Boolean).length);
      });
    }

    function loadApplications() {
      // ApplicationStatus is DRAFT | SUBMITTED | PENDING_APPROVAL | APPROVED |
      // REJECTED | CANCELLED — there is no plain "PENDING". Applications
      // awaiting a decision are PENDING_APPROVAL.
      return endpoints.accounts
        .applications({ status: 'PENDING_APPROVAL', page: 0, size: 1 })
        .then(function (envelope) {
          self.applicationCount((envelope && envelope.totalItems) || 0);
        });
    }

    // Each tile fails independently: one dead endpoint should not blank the page.
    function guard(promise) {
      return promise.catch(function (err) {
        if (!err || !err.isSessionExpired) {
          self.error(http.messageFor(err));
        }
      });
    }

    // ModuleRouterAdapter can instantiate the current route before the auth-gated
    // <oj-module> enters the DOM. Firing these protected requests immediately
    // therefore produces four 401s on the login screen and leaves Overview stale
    // after sign-in. Start once a real session exists, whether it was restored at
    // startup or published by login.js a moment later.
    var started = false;
    var unsubscribe = null;

    function start() {
      if (started || !session.isAuthenticated()) {
        return;
      }
      started = true;
      if (unsubscribe) {
        unsubscribe();
        unsubscribe = null;
      }
      var calls = [];
      if (session.hasPermission('TRANSACTION_VIEW')) {
        calls.push(guard(loadToday()));
        calls.push(guard(loadRecent()));
      }
      if (isTeller) { calls.push(guard(loadCashPosition())); }
      if (isTeller || isChecker) { calls.push(guard(loadKycPending())); }
      if (self.showApplications) { calls.push(guard(loadApplications())); }
      Promise.all(calls).then(
        function () {
          self.loading(false);
        }
      );
    }

    if (session.isAuthenticated()) {
      start();
    } else {
      unsubscribe = session.subscribe(start);
    }

    self.disconnected = function () {
      if (unsubscribe) {
        unsubscribe();
        unsubscribe = null;
      }
    };

    self.recentProvider = new ArrayDataProvider(self.recent, { keyAttributes: 'id' });
  }

  return OverviewViewModel;
});
