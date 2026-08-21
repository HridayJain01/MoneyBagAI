/**
 * Branch-manager holdings dashboard.
 *
 * The account query is explicitly limited to the branch in the authenticated
 * session. Customer responses are used only for their risk classification;
 * names and other personal fields are deliberately discarded. Physical/vault
 * cash and lending recommendations stay blank because neither is authoritative
 * in the current domain.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/format',
  '../services/http',
  '../services/session'
], function (ko, endpoints, fmt, http, session) {
  'use strict';

  var PAGE_SIZE = 100;
  var PRODUCT_GROUPS = [
    { key: 'SAVINGS', label: 'Savings', shortLabel: 'SAV' },
    { key: 'CURRENT', label: 'Current accounts', shortLabel: 'CURR' },
    { key: 'TERM_DEPOSIT', label: 'Fixed deposits', shortLabel: 'FD' },
    { key: 'RECURRING_DEPOSIT', label: 'Recurring deposits', shortLabel: 'RD' }
  ];
  var RISK_GROUPS = [
    {
      key: 'LOW', label: 'Low-risk model', tone: 'success', intent: 'Capital preservation',
      loanRate: 0.06, expectedReturn: 1.00
    },
    {
      key: 'MEDIUM', label: 'Medium-risk model', tone: 'warning', intent: 'Balanced growth',
      loanRate: 0.08, expectedReturn: 0.80
    },
    {
      key: 'HIGH', label: 'High-risk model', tone: 'danger', intent: 'Selective higher-risk growth',
      loanRate: 0.10, expectedReturn: 0.60
    }
  ];

  function numberValue(value) {
    var number = Number(value);
    return isFinite(number) ? number : 0;
  }

  function activeHolding(account) {
    return account.status !== 'CLOSED';
  }

  function productGroup(product) {
    var type = product && product.productType;
    return PRODUCT_GROUPS.some(function (group) { return group.key === type; }) ? type : 'OTHER';
  }

  function customerType(product) {
    var code = String((product && product.productCode) || '').toUpperCase();
    var type = product && product.productType;
    if (type === 'CURRENT') { return 'Business customer'; }
    if (code.indexOf('SENIOR') !== -1) { return 'Senior retail customer'; }
    if (type === 'TERM_DEPOSIT') { return 'Retail term depositor'; }
    if (type === 'RECURRING_DEPOSIT') { return 'Retail recurring depositor'; }
    if (type === 'SAVINGS') { return 'Retail savings customer'; }
    return '—';
  }

  function riskTone(risk) {
    if (risk === 'LOW') { return 'success'; }
    if (risk === 'MEDIUM') { return 'warning'; }
    if (risk === 'HIGH') { return 'danger'; }
    return 'neutral';
  }

  function HoldingsViewModel() {
    var self = this;
    var identity = session.getSession() || {};

    self.branchCode = ko.observable(identity.branchCode || '');
    self.branchName = ko.observable('—');
    self.loading = ko.observable(false);
    self.error = ko.observable('');
    self.lastRefreshed = ko.observable(null);
    self.accounts = ko.observableArray([]);
    self.productRows = ko.observableArray([]);
    self.accountRows = ko.observableArray([]);
    self.riskModels = ko.observableArray([]);
    self.netCashMovement = ko.observable(null);

    self.hasError = ko.pureComputed(function () { return !!self.error(); });
    self.totalAccounts = ko.pureComputed(function () { return self.accounts().length; });
    self.activeAccounts = ko.pureComputed(function () {
      return self.accounts().filter(function (account) { return account.status === 'ACTIVE'; }).length;
    });
    self.totalDepositsValue = ko.pureComputed(function () {
      return self.accounts().filter(activeHolding).reduce(function (sum, account) {
        return sum + Math.max(0, numberValue(account.ledgerBalance));
      }, 0);
    });
    self.totalDeposits = ko.pureComputed(function () {
      return fmt.money(self.totalDepositsValue(), 'INR');
    });
    self.weightedDepositRate = ko.pureComputed(function () {
      var balance = self.totalDepositsValue();
      if (!balance) { return '—'; }
      var weighted = self.accounts().filter(activeHolding).reduce(function (sum, account) {
        var amount = Math.max(0, numberValue(account.ledgerBalance));
        return sum + (amount * numberValue(account.interestRate));
      }, 0);
      return (weighted / balance).toFixed(2) + '%';
    });
    self.netCashMovementDisplay = ko.pureComputed(function () {
      return self.netCashMovement() === null ? '—' : fmt.money(self.netCashMovement(), 'INR');
    });
    self.lastRefreshedDisplay = ko.pureComputed(function () {
      return self.lastRefreshed() ? fmt.dateTime(self.lastRefreshed()) : '—';
    });

    function loadAllAccounts(page, accumulated) {
      return endpoints.accounts.search({
        branchCode: self.branchCode(), page: page, size: PAGE_SIZE
      }).then(function (response) {
        var rows = accumulated.concat((response && response.items) || []);
        var totalPages = (response && response.totalPages) || 0;
        return page + 1 < totalPages ? loadAllAccounts(page + 1, rows) : rows;
      });
    }

    function loadAllTransactions(page, accumulated) {
      return endpoints.transactions.search({
        from: fmt.startOfTodayIso(), to: fmt.endOfTodayIso(),
        page: page, size: PAGE_SIZE, sort: 'createdAt,asc'
      }).then(function (response) {
        var rows = accumulated.concat((response && response.content) || []);
        var totalPages = (response && response.totalPages) || 0;
        return page + 1 < totalPages ? loadAllTransactions(page + 1, rows) : rows;
      });
    }

    function riskMapFor(accounts) {
      var uniqueCifs = accounts.reduce(function (result, account) {
        if (account.cifNo && result.indexOf(account.cifNo) === -1) { result.push(account.cifNo); }
        return result;
      }, []);
      return Promise.all(uniqueCifs.map(function (cifNo) {
        return endpoints.customers.get(cifNo).then(function (customer) {
          return { cifNo: cifNo, risk: customer && customer.riskClassification };
        }).catch(function () {
          return { cifNo: cifNo, risk: null };
        });
      })).then(function (entries) {
        return entries.reduce(function (map, entry) {
          map[entry.cifNo] = entry.risk;
          return map;
        }, {});
      });
    }

    function buildRows(accounts, products, risks) {
      var productMap = products.reduce(function (map, product) {
        map[product.productCode] = product;
        return map;
      }, {});
      var holdings = accounts.filter(activeHolding);

      var baseGroups = PRODUCT_GROUPS.concat([{ key: 'OTHER', label: 'Other deposits', shortLabel: 'OTHER' }]);
      self.productRows(baseGroups.map(function (group) {
        var rows = holdings.filter(function (account) {
          return productGroup(productMap[account.productCode]) === group.key;
        });
        return {
          key: group.key,
          label: group.label,
          shortLabel: group.shortLabel,
          accountCount: rows.length,
          balance: fmt.money(rows.reduce(function (sum, account) {
            return sum + Math.max(0, numberValue(account.ledgerBalance));
          }, 0), 'INR')
        };
      }));

      self.accountRows(accounts.map(function (account) {
        var product = productMap[account.productCode] || {};
        return {
          accountId: account.accountId,
          maskedNumber: account.maskedAccountNumber || '—',
          productName: product.productName || account.productCode,
          productType: fmt.humanize(product.productType),
          customerType: customerType(product),
          risk: fmt.humanize(risks[account.cifNo]),
          riskClass: 'mb-pill mb-pill--' + riskTone(risks[account.cifNo]),
          status: fmt.humanize(account.status),
          statusClass: 'mb-pill mb-pill--' + fmt.toneFor(account.status),
          balance: fmt.money(account.ledgerBalance, account.currency)
        };
      }));

      self.riskModels(RISK_GROUPS.map(function (model) {
        var cifs = [];
        var depositRelationship = holdings.reduce(function (sum, account) {
          if (risks[account.cifNo] !== model.key) { return sum; }
          if (cifs.indexOf(account.cifNo) === -1) { cifs.push(account.cifNo); }
          return sum + Math.max(0, numberValue(account.ledgerBalance));
        }, 0);
        var recommendedLending = self.totalDepositsValue() * model.expectedReturn;
        return {
          key: model.key,
          label: model.label,
          intent: model.intent,
          toneClass: 'mb-pill mb-pill--' + model.tone,
          customerCount: cifs.length,
          depositRelationship: fmt.money(depositRelationship, 'INR'),
          loanRate: (model.loanRate * 100).toFixed(0) + '%',
          expectedReturn: (model.expectedReturn * 100).toFixed(0) + '%',
          lendingEnvelope: fmt.money(recommendedLending, 'INR'),
          projectedProfit: fmt.money(recommendedLending * model.loanRate, 'INR')
        };
      }));
    }

    function calculateCashMovement(transactions) {
      return transactions.filter(function (tx) {
        return tx.branchCode === self.branchCode() && tx.rail === 'CASH' &&
          (tx.status === 'COMPLETED' || tx.status === 'SETTLED');
      }).reduce(function (sum, tx) {
        if (tx.type === 'DEPOSIT') { return sum + numberValue(tx.amount); }
        if (tx.type === 'WITHDRAWAL') { return sum - numberValue(tx.amount); }
        return sum;
      }, 0);
    }

    self.refresh = function () {
      if (!self.branchCode()) {
        self.error('The signed-in manager has no branch assignment.');
        return;
      }
      self.loading(true);
      self.error('');
      Promise.all([
        endpoints.branches.list(),
        endpoints.products.list(),
        loadAllAccounts(0, []),
        loadAllTransactions(0, [])
      ]).then(function (parts) {
        var branch = (parts[0] || []).find(function (item) {
          return item.branchCode === self.branchCode();
        });
        if (!branch) { throw new Error('Branch ' + self.branchCode() + ' was not found.'); }
        self.branchName(branch.name || branch.branchCode);
        self.accounts(parts[2]);
        self.netCashMovement(calculateCashMovement(parts[3]));
        return riskMapFor(parts[2]).then(function (risks) {
          buildRows(parts[2], parts[1] || [], risks);
        });
      }).then(function () {
        self.lastRefreshed(new Date().toISOString());
      }).catch(function (err) {
        self.error(http.messageFor(err));
        self.accounts([]);
        self.productRows([]);
        self.accountRows([]);
        self.riskModels([]);
        self.netCashMovement(null);
      }).then(function () {
        self.loading(false);
      });
    };

    self.refresh();
  }

  return HoldingsViewModel;
});
