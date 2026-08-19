/**
 * Maker-checker approval queue.
 *
 * GET /api/v1/transactions/approvals returns a Spring Page, sorted createdAt
 * ASC (oldest first) by the server — the queue is worked front to back. Do not
 * flip that sort.
 *
 * Both actions require an Idempotency-Key. The key is minted when the user
 * commits to the action (the dialog opens) and reused across retries; that
 * discipline now lives in support/confirm.js rather than being restated here.
 *
 * Maker != checker is enforced server-side and arrives as a 403 whose message
 * names the rule. http.messageFor passes non-PERMISSION_DENIED 403 messages
 * through verbatim, so it is shown as-is.
 *
 * Worth knowing when testing: only ONE limit rule is seeded (RTGS/INR, approval
 * at 1,000,000), so nothing but a large RTGS ever lands in this queue. See
 * PROGRESS.md for the recipe that puts a row here.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/providers',
  '../services/format',
  './support/banner',
  './support/confirm',
  'ojs/ojtable',
  'ojs/ojbutton',
  'ojs/ojdialog',
  'ojs/ojinputtext'
], function (ko, endpoints, providers, fmt, Banner, Confirm) {
  'use strict';

  var RAILS = ['', 'INTERNAL', 'NEFT', 'RTGS', 'IMPS', 'UPI', 'CHEQUE', 'CARD', 'CASH'];

  function ApprovalsViewModel() {
    var self = this;

    Banner.call(self);
    Confirm.call(self, { dialogId: 'approvalsConfirmDialog' });

    self.rail = ko.observable('');
    self.minAmount = ko.observable('');
    self.railList = RAILS;
    self.reason = ko.observable('');

    // Read at fetch time so the provider always picks up the latest values.
    self.provider = providers.pagedProvider({
      url: '/api/v1/transactions/approvals',
      idKey: 'transactionId',
      shape: 'spring',
      pageSize: 25,
      query: function () {
        return {
          rail: self.rail() || undefined,
          minAmount: self.minAmount() || undefined
        };
      }
    });

    self.columns = [
      { headerText: 'Reference', field: 'transactionReference', template: 'refTemplate' },
      { headerText: 'Type', field: 'type', template: 'typeTemplate' },
      { headerText: 'Amount', field: 'amount', template: 'amountTemplate' },
      { headerText: 'Branch', field: 'branchCode' },
      { headerText: 'Maker', field: 'makerEmployeeId' },
      { headerText: 'Raised', field: 'createdAt', template: 'createdTemplate' },
      { headerText: '', template: 'actionsTemplate', sortable: 'disabled' }
    ];

    /* ----------------------------------------------------------- filters -- */

    self.applyFilters = function () {
      providers.refreshProvider(self.provider);
    };

    /* ------------------------------------------------------------ dialog -- */

    self.dialogTitle = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return payload && payload.action === 'approve' ? 'Approve transaction' : 'Reject transaction';
    });

    self.isReject = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return !!payload && payload.action === 'reject';
    });

    self.confirmLabel = ko.pureComputed(function () {
      if (self.busy()) {
        return 'Working…';
      }
      return self.isReject() ? 'Reject' : 'Approve';
    });

    self.dialogSummary = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return '';
      }
      return (
        (payload.action === 'approve' ? 'Approve ' : 'Reject ') +
        payload.tx.transactionReference +
        ' for ' +
        fmt.money(payload.tx.amount, payload.tx.currency) +
        '?'
      );
    });

    function commit(tx, action) {
      self.reason('');
      self.dismissBanner();
      self.openConfirm({ tx: tx, action: action });
    }

    self.approveRow = function (tx) {
      commit(tx, 'approve');
    };

    self.rejectRow = function (tx) {
      commit(tx, 'reject');
    };

    self.confirm = function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return;
      }
      if (payload.action === 'reject' && !(self.reason() || '').trim()) {
        self.notify('error', 'Reason required', 'Enter why this is being rejected.');
        return;
      }

      self
        .runConfirm(function (committed, intent) {
          return committed.action === 'approve'
            ? endpoints.transactions.approve(committed.tx.transactionId, intent.idempotencyKey)
            : endpoints.transactions.reject(
                committed.tx.transactionId,
                { reason: self.reason().trim() },
                intent.idempotencyKey
              );
        })
        .then(function (result) {
          // runConfirm resolves null without calling the server when a request
          // is already in flight, so a double-click must not report success.
          if (result === null) {
            return;
          }
          var approved = payload.action === 'approve';
          self.notify(
            'success',
            approved ? 'Approved' : 'Rejected',
            payload.tx.transactionReference + ' was ' + (approved ? 'approved' : 'rejected') + '.'
          );
          providers.refreshProvider(self.provider);
        })
        .catch(function (error) {
          // The dialog and its idempotency key stay alive so the same intent
          // can be retried — that is handled inside runConfirm.
          self.failed('Could not complete', error);
        });
    };

    /* -------------------------------------------------- cell formatting -- */

    self.fmtMoney = function (row) {
      return fmt.money(row.amount, row.currency);
    };
    self.fmtType = function (row) {
      return fmt.humanize(row.type);
    };
    self.fmtCreated = function (row) {
      return fmt.dateTime(row.createdAt);
    };
  }

  return ApprovalsViewModel;
});
