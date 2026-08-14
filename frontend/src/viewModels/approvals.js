/**
 * Maker-checker approval queue.
 *
 * GET /api/v1/transactions/approvals returns a Spring Page, sorted createdAt
 * ASC (oldest first) by the server — the queue is worked front to back.
 *
 * Both actions require an Idempotency-Key. The key is minted when the user
 * commits to the action (dialog opens) and reused across retries: a fresh key
 * per attempt would defeat the protection and could double-post.
 *
 * Maker != checker is enforced server-side and surfaces as 422; that message is
 * shown verbatim because it explains exactly which rule refused.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/providers',
  '../services/format',
  '../services/http',
  'ojs/ojtable',
  'ojs/ojbutton',
  'ojs/ojdialog',
  'ojs/ojinputtext'
], function (ko, endpoints, providers, fmt, http) {
  'use strict';

  var RAILS = ['', 'INTERNAL', 'NEFT', 'RTGS', 'IMPS', 'UPI', 'CHEQUE', 'CARD', 'CASH'];

  function ApprovalsViewModel() {
    var self = this;

    self.rail = ko.observable('');
    self.minAmount = ko.observable('');
    self.railList = RAILS;

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

    /* ------------------------------------------------------------ banner -- */

    self.banner = ko.observable(null);
    self.hasBanner = ko.pureComputed(function () {
      return !!self.banner();
    });
    self.bannerClass = ko.pureComputed(function () {
      var b = self.banner();
      return b ? 'mb-banner mb-banner--' + b.tone : 'mb-banner';
    });
    self.dismissBanner = function () {
      self.banner(null);
    };

    /* ----------------------------------------------------------- filters -- */

    self.applyFilters = function () {
      providers.refreshProvider(self.provider);
    };

    /* ------------------------------------------------------------ dialog -- */

    self.pending = ko.observable(null); // { tx, action, key }
    self.reason = ko.observable('');
    self.busy = ko.observable(false);

    self.dialogTitle = ko.pureComputed(function () {
      var p = self.pending();
      return p && p.action === 'approve' ? 'Approve transaction' : 'Reject transaction';
    });

    self.isReject = ko.pureComputed(function () {
      var p = self.pending();
      return !!p && p.action === 'reject';
    });

    self.confirmLabel = ko.pureComputed(function () {
      if (self.busy()) {
        return 'Working…';
      }
      return self.isReject() ? 'Reject' : 'Approve';
    });

    self.dialogSummary = ko.pureComputed(function () {
      var p = self.pending();
      if (!p) {
        return '';
      }
      return (
        (p.action === 'approve' ? 'Approve ' : 'Reject ') +
        p.tx.transactionReference +
        ' for ' +
        fmt.money(p.tx.amount, p.tx.currency) +
        '?'
      );
    });

    function openDialog(tx, action) {
      self.reason('');
      self.banner(null);
      // One key per intent, held for the life of this dialog.
      self.pending({ tx: tx, action: action, key: http.beginCommand() });
      var dialog = document.getElementById('confirmDialog');
      if (dialog) {
        dialog.open();
      }
    }

    function closeDialog() {
      var dialog = document.getElementById('confirmDialog');
      if (dialog) {
        dialog.close();
      }
      self.pending(null);
    }

    self.approveRow = function (tx) {
      openDialog(tx, 'approve');
    };

    self.rejectRow = function (tx) {
      openDialog(tx, 'reject');
    };

    self.cancelDialog = function () {
      if (!self.busy()) {
        closeDialog();
      }
    };

    self.confirm = function () {
      var p = self.pending();
      if (!p || self.busy()) {
        return;
      }
      if (p.action === 'reject' && !(self.reason() || '').trim()) {
        self.banner({ tone: 'error', title: 'Reason required', detail: 'Enter why this is being rejected.' });
        return;
      }

      self.busy(true);

      var call =
        p.action === 'approve'
          ? endpoints.transactions.approve(p.tx.transactionId, p.key)
          : endpoints.transactions.reject(p.tx.transactionId, { reason: self.reason().trim() }, p.key);

      call
        .then(function () {
          closeDialog();
          var verb = p.action === 'approve' ? 'approved' : 'rejected';
          self.banner({
            tone: 'success',
            title: p.action === 'approve' ? 'Approved' : 'Rejected',
            detail: p.tx.transactionReference + ' was ' + verb + '.'
          });
          providers.refreshProvider(self.provider);
        })
        .catch(function (err) {
          // Keep the dialog and the key so the user can retry the same intent.
          self.banner({ tone: 'error', title: 'Could not complete', detail: http.messageFor(err) });
        })
        .then(function () {
          self.busy(false);
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
