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
  '../services/navigation',
  '../services/session',
  './support/banner',
  './support/confirm',
  'ojs/ojtable',
  'ojs/ojchart',
  'ojs/ojbutton',
  'ojs/ojdialog'
], function (ko, endpoints, providers, fmt, http, navigation, session, Banner, Confirm) {
  'use strict';

  function AccountDetailViewModel(context) {
    var self = this;
    var params = (context && context.params) || {};
    // ModuleRouterAdapter does not pass router params into the module context,
    // so the navigation module is the reliable source.
    var accountId = params.id || navigation.param('id');

    Banner.call(self);
    Confirm.call(self, { dialogId: 'accountServicingDialog' });

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
    self.canManage = session.hasPermission('ACCOUNT_STATUS_MANAGE');
    self.holders = ko.observableArray([]);
    self.holds = ko.observableArray([]);
    self.statusHistory = ko.observableArray([]);
    self.ownedProducts = ko.observableArray([]);
    self.limits = ko.observable(null);
    self.reason = ko.observable('');
    self.holderCif = ko.observable('');
    self.holderRole = ko.observable('JOINT');
    self.holdAmount = ko.observable('');
    self.holdReason = ko.observable('');
    self.holdType = ko.observable('MANUAL');
    self.perTransactionLimit = ko.observable('');
    self.dailyWithdrawalLimit = ko.observable('');

    self.availableDisplay = ko.pureComputed(function () {
      return fmt.amount(self.available());
    });
    self.ledgerDisplay = ko.pureComputed(function () {
      return fmt.amount(self.ledger());
    });
    self.heldDisplay = ko.pureComputed(function () {
      return fmt.amount(self.held());
    });

    self.hasHeldFunds = ko.pureComputed(function () {
      return Number(self.held()) > 0;
    });

    self.lifecycleActions = ko.pureComputed(function () {
      if (!self.canManage || self.status() === 'CLOSED') {
        return [];
      }
      var current = self.status();
      var actions = [];
      if (current === 'ACTIVE') {
        actions.push({ id: 'freeze', label: 'Freeze', danger: false }, { id: 'block', label: 'Block', danger: false }, { id: 'markDormant', label: 'Mark dormant', danger: false });
      } else if (current === 'FROZEN') {
        actions.push({ id: 'unfreeze', label: 'Unfreeze', danger: false }, { id: 'block', label: 'Block', danger: false });
      } else if (current === 'BLOCKED') {
        actions.push({ id: 'unblock', label: 'Unblock', danger: false }, { id: 'freeze', label: 'Freeze', danger: false });
      } else if (current === 'DORMANT') {
        actions.push({ id: 'reactivate', label: 'Reactivate', danger: false }, { id: 'freeze', label: 'Freeze', danger: false }, { id: 'block', label: 'Block', danger: false });
      }
      actions.push({ id: 'close', label: 'Close account', danger: true });
      return actions;
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
      { headerText: 'Posted', field: 'createdAt', template: 'whenTemplate' },
      { headerText: '', template: 'actionsTemplate', sortable: 'disabled' }
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
    self.openTransaction = function (row) {
      navigation.openTransaction(row.transactionId);
    };

    self.fmtDateTime = function (value) {
      return fmt.dateTime(value);
    };

    self.fmtMoney = function (value, currency) {
      return fmt.money(value, currency || self.currency());
    };

    /* ------------------------------------------------------- servicing -- */

    function loadServicing() {
      return Promise.all([
        endpoints.accounts.holders(accountId).then(self.holders),
        endpoints.accounts.holds(accountId).then(self.holds),
        endpoints.accounts.statusHistory(accountId).then(self.statusHistory),
        endpoints.accounts.ownedProducts(accountId).then(self.ownedProducts),
        endpoints.accounts.limits(accountId).then(function (value) {
          self.limits(value);
          self.perTransactionLimit(value.perTransactionLimit);
          self.dailyWithdrawalLimit(value.dailyWithdrawalLimit);
        })
      ]);
    }

    function loadCurrentAccount() {
      return Promise.all([
        endpoints.accounts.get(accountId).then(function (account) {
          self.status(account.status);
        }),
        endpoints.accounts.balance(accountId).then(function (balance) {
          self.available(balance.availableBalance);
          self.ledger(balance.ledgerBalance);
          self.held(balance.heldAmount);
          self.asOf(fmt.dateTime(balance.asOf));
        })
      ]);
    }

    function beginServicing(action) {
      self.reason('');
      self.dismissBanner();
      self.openConfirm({ kind: 'lifecycle', action: action });
    }

    self.lifecycle = function (action) {
      beginServicing(action);
    };

    self.addHolder = function () {
      self.holderCif('');
      self.holderRole('JOINT');
      self.dismissBanner();
      self.openConfirm({ kind: 'holder' });
    };

    self.placeHold = function () {
      self.holdAmount('');
      self.holdReason('');
      self.holdType('MANUAL');
      self.dismissBanner();
      self.openConfirm({ kind: 'hold' });
    };

    self.editLimits = function () {
      var value = self.limits() || {};
      self.perTransactionLimit(value.perTransactionLimit || 0);
      self.dailyWithdrawalLimit(value.dailyWithdrawalLimit || 0);
      self.dismissBanner();
      self.openConfirm({ kind: 'limits' });
    };

    self.dialogTitle = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) { return 'Service account'; }
      if (payload.kind === 'holder') { return 'Add account holder'; }
      if (payload.kind === 'hold') { return 'Place funds hold'; }
      if (payload.kind === 'limits') { return 'Set account limits'; }
      return payload.action.label;
    });

    self.isLifecycle = ko.pureComputed(function () { return self.confirmPayload() && self.confirmPayload().kind === 'lifecycle'; });
    self.isHolderAction = ko.pureComputed(function () { return self.confirmPayload() && self.confirmPayload().kind === 'holder'; });
    self.isHoldAction = ko.pureComputed(function () { return self.confirmPayload() && self.confirmPayload().kind === 'hold'; });
    self.isLimitsAction = ko.pureComputed(function () { return self.confirmPayload() && self.confirmPayload().kind === 'limits'; });
    self.confirmLabel = ko.pureComputed(function () { return self.busy() ? 'Working…' : 'Confirm change'; });

    var lifecycleEndpoints = {
      freeze: endpoints.accounts.freeze,
      unfreeze: endpoints.accounts.unfreeze,
      block: endpoints.accounts.block,
      unblock: endpoints.accounts.unblock,
      markDormant: endpoints.accounts.markDormant,
      reactivate: endpoints.accounts.reactivate,
      close: endpoints.accounts.close
    };

    self.confirmServicing = function () {
      var payload = self.confirmPayload();
      if (!payload) { return; }
      var holderCif = (self.holderCif() || '').trim().toUpperCase();
      var holdAmount = Number(self.holdAmount());
      var holdReason = (self.holdReason() || '').trim();
      var perTxn = Number(self.perTransactionLimit());
      var daily = Number(self.dailyWithdrawalLimit());
      if (payload.kind === 'holder' && !holderCif) {
        self.notify('error', 'CIF required', 'Enter the customer to add as a joint holder.');
        return;
      }
      if (payload.kind === 'hold' && (!isFinite(holdAmount) || holdAmount < 0.01 || !holdReason)) {
        self.notify('error', 'Hold details required', 'Enter an amount of at least 0.01 and a reason.');
        return;
      }
      if (payload.kind === 'limits' && (!isFinite(perTxn) || perTxn < 0 || !isFinite(daily) || daily < 0)) {
        self.notify('error', 'Valid limits required', 'Both limits must be zero or greater.');
        return;
      }

      self.runConfirm(function (committed, intent) {
        if (committed.kind === 'lifecycle') {
          return lifecycleEndpoints[committed.action.id](accountId, { reason: (self.reason() || '').trim() || committed.action.label + ' by staff' }, intent.idempotencyKey);
        }
        if (committed.kind === 'holder') {
          return endpoints.accounts.addHolder(accountId, { cifNo: holderCif, holderRole: self.holderRole() }, intent.idempotencyKey);
        }
        if (committed.kind === 'hold') {
          return endpoints.accounts.placeHold(accountId, { amount: holdAmount, reason: holdReason, holdType: self.holdType() }, intent.idempotencyKey);
        }
        return endpoints.accounts.setLimits(accountId, { perTransactionLimit: perTxn, dailyWithdrawalLimit: daily }, intent.idempotencyKey);
      }).then(function (result) {
        if (result === null) { return; }
        self.notify('success', 'Account updated', 'The servicing change was applied successfully.');
        return Promise.all([loadCurrentAccount(), loadServicing()]);
      }).catch(function (error) {
        self.failed('Account could not be updated', error);
      });
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

    var loadServiceData = loadServicing();

    Promise.all([guard(loadDetail), guard(loadBalance), guard(loadHistory), guard(loadMini), guard(loadServiceData)]).then(function () {
      self.loading(false);
    });
  }

  return AccountDetailViewModel;
});
