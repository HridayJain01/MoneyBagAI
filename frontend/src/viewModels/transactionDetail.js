/** Deep transaction evidence and permitted corrective actions. */
define([
  'knockout',
  '../services/endpoints',
  '../services/navigation',
  '../services/session',
  '../services/format',
  './support/banner',
  './support/confirm',
  'ojs/ojbutton',
  'ojs/ojdialog'
], function (ko, endpoints, navigation, session, fmt, Banner, Confirm) {
  'use strict';

  function TransactionDetailViewModel(context) {
    var self = this;
    var params = (context && context.params) || {};
    var transactionId = params.id || navigation.param('id');
    var identity = session.getSession() || {};

    Banner.call(self);
    Confirm.call(self, { dialogId: 'transactionActionDialog' });

    self.transactionId = transactionId;
    self.loading = ko.observable(true);
    self.transaction = ko.observable(null);
    self.reason = ko.observable('');

    self.reference = ko.pureComputed(function () {
      var tx = self.transaction();
      return tx ? tx.transactionReference : 'Transaction';
    });
    self.subtitle = ko.pureComputed(function () {
      var tx = self.transaction();
      return tx ? [fmt.humanize(tx.type), tx.rail, tx.channel].filter(Boolean).join(' · ') : '';
    });
    self.statusClass = ko.pureComputed(function () {
      return 'mb-pill mb-pill--' + fmt.toneFor(self.transaction() && self.transaction().status);
    });
    self.amountDisplay = ko.pureComputed(function () {
      var tx = self.transaction();
      return tx ? fmt.money(tx.amount, tx.currency) : '—';
    });
    self.feeDisplay = ko.pureComputed(function () {
      var tx = self.transaction();
      return tx ? fmt.money(tx.feeAmount || 0, tx.currency) : '—';
    });
    self.canCancel = ko.pureComputed(function () {
      var tx = self.transaction();
      if (!tx || !session.hasPermission('TRANSACTION_CANCEL')) { return false; }
      var cancellable = ['RECEIVED', 'VALIDATED', 'PENDING_APPROVAL', 'APPROVED', 'FUNDS_RESERVED'].indexOf(tx.status) !== -1;
      var owns = tx.makerEmployeeId === identity.employeeId || session.hasPermission('TRANSACTION_CANCEL_ANY');
      return cancellable && owns;
    });
    self.canReverse = ko.pureComputed(function () {
      var tx = self.transaction();
      return !!tx && session.hasPermission('TRANSACTION_REVERSE') && tx.status === 'COMPLETED' && tx.type !== 'REVERSAL';
    });
    self.hasActions = ko.pureComputed(function () {
      return self.canCancel() || self.canReverse();
    });

    self.fmtMoney = function (value, currency) { return fmt.money(value, currency); };
    self.fmtDateTime = function (value) { return fmt.dateTime(value); };
    self.fmtType = function (value) { return fmt.humanize(value); };
    self.toneClass = function (value) { return 'mb-pill mb-pill--' + fmt.toneFor(value); };

    function load() {
      if (!transactionId) {
        self.notify('error', 'No transaction selected', 'Open a transaction from the search screen.');
        self.loading(false);
        return Promise.resolve();
      }
      self.loading(true);
      self.dismissBanner();
      return endpoints.transactions.get(transactionId).then(function (tx) {
        self.transaction(tx);
      }).catch(function (error) {
        self.failed('Transaction could not be loaded', error);
      }).then(function () {
        self.loading(false);
      });
    }

    function begin(action) {
      self.reason('');
      self.dismissBanner();
      self.openConfirm({ action: action, transaction: self.transaction() });
    }
    self.cancelTransaction = function () { begin('cancel'); };
    self.reverseTransaction = function () { begin('reverse'); };

    self.dialogTitle = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return payload && payload.action === 'reverse' ? 'Reverse transaction' : 'Cancel transaction';
    });
    self.dialogSummary = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) { return ''; }
      return fmt.humanize(payload.action) + ' ' + payload.transaction.transactionReference + ' for ' + fmt.money(payload.transaction.amount, payload.transaction.currency) + '?';
    });
    self.confirmLabel = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (self.busy()) { return 'Working…'; }
      return payload && payload.action === 'reverse' ? 'Create reversal' : 'Cancel transaction';
    });

    self.confirmAction = function () {
      var payload = self.confirmPayload();
      var reason = (self.reason() || '').trim();
      if (!payload) { return; }
      if (!reason) {
        self.notify('error', 'Reason required', 'Enter the operational reason for this action.');
        return;
      }
      self.runConfirm(function (committed, intent) {
        return committed.action === 'reverse'
          ? endpoints.transactions.reverse(transactionId, { reason: reason }, intent.idempotencyKey)
          : endpoints.transactions.cancel(transactionId, { reason: reason }, intent.idempotencyKey);
      }).then(function (result) {
        if (result === null) { return; }
        if (payload.action === 'reverse') {
          var newId = result.transactionId || result.id;
          self.notify('success', 'Reversal created', (result.transactionReference || result.reference || 'The compensating transaction') + ' was created.');
          if (newId) {
            navigation.openTransaction(newId);
          }
          return;
        }
        self.notify('success', 'Transaction cancelled', payload.transaction.transactionReference + ' was cancelled.');
        return load();
      }).catch(function (error) {
        self.failed('Transaction action failed', error);
      });
    };

    self.openLinkedTransaction = function (id) {
      if (id) { navigation.openTransaction(id); }
    };

    load();
  }

  return TransactionDetailViewModel;
});
