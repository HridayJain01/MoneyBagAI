/**
 * Account detail — the closest thing to a "personal dashboard" view, except the
 * viewer is an employee servicing the account rather than its owner.
 *
 * Three response shapes meet on this page, which is worth naming:
 *   /accounts/{id}, /balance         account-service, plain objects
 *   /accounts/{id}/balance-history   account-service PageResponse (envelope)
 *   /accounts/{id}/transactions      transaction-service Spring Page
 *   /accounts/{id}/mini-statement    transaction-service bare List
 *
 * The last two live under an /accounts path but are routed to
 * transaction-service by a gateway rule with order -10, which is exactly why
 * one resource family ends up with two different envelopes.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/providers',
  '../services/format',
  '../services/http',
  'ojs/ojtable',
  'ojs/ojchart',
  'ojs/ojbutton'
], function (ko, endpoints, providers, fmt, http) {
  'use strict';

  function AccountDetailViewModel(context) {
    var self = this;
    var params = (context && context.params) || {};
    // ModuleRouterAdapter does not pass router params into the module context,
    // so the navigation module is the reliable source.
    var accountId = params.id || navigation.param('id');

    self.accountId = accountId;
    self.loading = ko.observable(true);
    self.error = ko.observable(null);
    self.hasError = ko.pureComputed(function () {
      return !!self.error();
    });

    self.accountName = ko.observable('Account');
    self.subtitle = ko.observable('');
    self.status = ko.observable('');
    self.statusClass = ko.pureComputed(function () {
      return 'mb-pill mb-pill--' + fmt.toneFor(self.status());
    });

    self.currency = ko.observable('INR');
    self.available = ko.observable(null);
    self.ledger = ko.observable(null);
    self.held = ko.observable(null);
    self.asOf = ko.observable('');

    self.availableDisplay = ko.pureComputed(function () {
      return fmt.amount(self.available());
    });
    self.ledgerDisplay = ko.pureComputed(function () {
      return fmt.amount(self.ledger());
    });
    self.heldDisplay = ko.pureComputed(function () {
      return fmt.amount(self.held());
    });

    self.facts = ko.observableArray([]);
    self.mini = ko.observableArray([]);
    self.miniEmpty = ko.pureComputed(function () {
      return !self.loading() && self.mini().length === 0;
    });

    self.historySeries = ko.observableArray([]);
    self.historyGroups = ko.observableArray([]);
    self.historyEmpty = ko.pureComputed(function () {
      return !self.loading() && self.historyGroups().length === 0;
    });
    // Chart colours must be set in code: CSS variables do not reach chart marks.
    self.chartStyleDefaults = { colors: ['#6d4aff'] };

    /* ---------------------------------------------------- transactions -- */

    self.provider = accountId
      ? providers.pagedProvider({
          url: '/api/v1/accounts/' + encodeURIComponent(accountId) + '/transactions',
          idKey: 'transactionId',
          shape: 'spring',
          pageSize: 25,
          sort: 'createdAt,desc'
        })
      : null;

    self.columns = [
      { headerText: 'Reference', field: 'transactionReference', template: 'refTemplate' },
      { headerText: 'Type', field: 'type', template: 'typeTemplate' },
      { headerText: 'Status', field: 'status', template: 'statusTemplate' },
      { headerText: 'Amount', field: 'amount', template: 'amountTemplate' },
      { headerText: 'Posted', field: 'createdAt', template: 'whenTemplate' }
    ];

    self.fmtType = function (row) {
      return fmt.humanize(row.type);
    };
    self.fmtWhen = function (row) {
      return fmt.dateTime(row.createdAt);
    };
    self.fmtAmount = function (row) {
      var dir = fmt.direction(row, accountId);
      return (dir === 'credit' ? '+' : '−') + fmt.money(Math.abs(row.amount), row.currency);
    };
    self.amountClass = function (row) {
      return 'mb-money mb-money--' + fmt.direction(row, accountId);
    };
    self.statusClassFor = function (row) {
      return 'mb-pill mb-pill--' + fmt.toneFor(row.status);
    };

    /* --------------------------------------------------------- loading -- */

    function guard(promise) {
      return promise.catch(function (err) {
        if (!err || !err.isSessionExpired) {
          self.error(http.messageFor(err));
        }
      });
    }

    if (!accountId) {
      self.error('No account selected.');
      self.loading(false);
      return;
    }

    var loadDetail = endpoints.accounts.get(accountId).then(function (account) {
      self.accountName(account.accountName || 'Account');
      self.subtitle(
        [account.maskedAccountNumber, account.productCode, 'Branch ' + account.branchCode]
          .filter(Boolean)
          .join(' · ')
      );
      self.status(account.status);
      self.currency(account.currency || 'INR');
      self.facts([
        { key: 'CIF', value: account.cifNo, mono: true },
        { key: 'Number', value: account.accountNumber, mono: true },
        { key: 'Product', value: account.productCode, mono: false },
        { key: 'Currency', value: account.currency, mono: false },
        { key: 'Opened', value: fmt.dateOnly(account.openedOn), mono: false },
        {
          key: 'Overdraft',
          value: account.overdraftLimit ? fmt.money(account.overdraftLimit, account.currency) : 'None',
          mono: false
        }
      ]);
    });

    var loadBalance = endpoints.accounts.balance(accountId).then(function (balance) {
      self.currency(balance.currency || self.currency());
      self.available(balance.availableBalance);
      self.ledger(balance.ledgerBalance);
      self.held(balance.heldAmount);
      self.asOf(fmt.dateTime(balance.asOf));
    });

    var loadHistory = endpoints.accounts
      .balanceHistory(accountId, { page: 0, size: 30 })
      .then(function (envelope) {
        var items = (envelope && envelope.items) || [];
        // The endpoint returns newest-first; the chart reads left to right.
        var points = items.slice().reverse();
        self.historyGroups(
          points.map(function (entry) {
            return entry.businessDate;
          })
        );
        self.historySeries([
          {
            name: 'Ledger balance',
            items: points.map(function (entry) {
              return entry.ledgerBalanceAfter;
            })
          }
        ]);
      });

    var loadMini = endpoints.transactions.miniStatement(accountId, 8).then(function (rows) {
      self.mini(
        (rows || []).map(function (tx) {
          var dir = fmt.direction(tx, accountId);
          return {
            id: tx.transactionId,
            title: tx.narration || fmt.humanize(tx.type),
            when: fmt.relativeDateTime(tx.createdAt),
            amount: (dir === 'credit' ? '+' : '−') + fmt.money(Math.abs(tx.amount), tx.currency),
            moneyClass: 'mb-money mb-money--' + dir,
            glyphClass: 'mb-glyph mb-glyph--' + dir,
            arrow: dir === 'credit' ? '↓' : '↑'
          };
        })
      );
    });

    Promise.all([guard(loadDetail), guard(loadBalance), guard(loadHistory), guard(loadMini)]).then(function () {
      self.loading(false);
    });
  }

  return AccountDetailViewModel;
});
