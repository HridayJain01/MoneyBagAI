/** Account-application queue with maker/checker-safe actions. */
define([
  'knockout',
  '../services/endpoints',
  '../services/providers',
  '../services/navigation',
  '../services/session',
  '../services/format',
  './support/banner',
  './support/confirm',
  'ojs/ojtable',
  'ojs/ojbutton',
  'ojs/ojdialog'
], function (ko, endpoints, providers, navigation, session, fmt, Banner, Confirm) {
  'use strict';

  var STATUSES = ['', 'DRAFT', 'SUBMITTED', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'];

  function ApplicationsViewModel() {
    var self = this;
    var identity = session.getSession() || {};

    Banner.call(self);
    Confirm.call(self, { dialogId: 'applicationsConfirmDialog' });

    self.status = ko.observable('PENDING_APPROVAL');
    self.cifNo = ko.observable('');
    self.statusList = STATUSES;
    self.activeFilter = ko.observable('review');
    self.applicationFilters = [
      { key: 'all', label: 'All', status: '' },
      { key: 'new', label: 'New', status: 'SUBMITTED' },
      { key: 'review', label: 'In Review', status: 'PENDING_APPROVAL' },
      { key: 'documents', label: 'Needs Documents', status: 'DRAFT' }
    ];
    self.pendingReview = ko.observable(null);
    self.approvedToday = ko.observable(null);
    self.slaAttention = ko.observable(null);
    self.reason = ko.observable('');
    self.canOpen = session.hasPermission('ACCOUNT_OPEN') && !session.hasRole('CHECKER');
    self.canApprove = session.hasPermission('ACCOUNT_APPROVE');

    self.provider = providers.pagedProvider({
      url: '/api/v1/accounts/applications',
      idKey: 'applicationId',
      shape: 'envelope',
      pageSize: 25,
      query: function () {
        return { status: self.status() || undefined, cifNo: (self.cifNo() || '').trim() || undefined };
      }
    });

    self.columns = [
      { headerText: 'Reference', field: 'applicationReference', template: 'referenceTemplate' },
      { headerText: 'Customer', field: 'cifNo' },
      { headerText: 'Product', field: 'productCode' },
      { headerText: 'Status', field: 'status', template: 'statusTemplate' },
      { headerText: 'Maker', field: 'makerEmployeeId' },
      { headerText: 'Created', field: 'createdAt', template: 'createdTemplate' },
      { headerText: '', template: 'actionsTemplate', sortable: 'disabled' }
    ];

    self.applyFilters = function () {
      providers.refreshProvider(self.provider);
    };

    self.selectFilter = function (filter) {
      self.activeFilter(filter.key);
      self.status(filter.status);
      self.applyFilters();
    };

    function loadMetrics() {
      var today = new Date();
      today.setHours(0, 0, 0, 0);
      var attentionBefore = Date.now() - (24 * 60 * 60 * 1000);
      return Promise.all([
        endpoints.accounts.applications({ status: 'PENDING_APPROVAL', page: 0, size: 100 }),
        endpoints.accounts.applications({ status: 'APPROVED', page: 0, size: 100 })
      ]).then(function (results) {
        var pendingEnvelope = results[0] || {};
        var approvedEnvelope = results[1] || {};
        var pendingItems = pendingEnvelope.items || [];
        var approvedItems = approvedEnvelope.items || [];
        self.pendingReview(pendingEnvelope.totalItems || 0);
        self.slaAttention(pendingItems.filter(function (row) {
          return row.createdAt && new Date(row.createdAt).getTime() < attentionBefore;
        }).length);
        self.approvedToday(approvedItems.filter(function (row) {
          return row.updatedAt && new Date(row.updatedAt).getTime() >= today.getTime();
        }).length);
      }).catch(function () {
        self.pendingReview(0);
        self.approvedToday(0);
        self.slaAttention(0);
      });
    }

    self.startApplication = function () {
      navigation.startAccountOpening();
    };

    self.isOwn = function (row) {
      return !!identity.employeeId && row.makerEmployeeId === identity.employeeId;
    };

    self.canDecide = function (row) {
      return self.canApprove && row.status === 'PENDING_APPROVAL' && !self.isOwn(row);
    };

    self.canCancel = function (row) {
      return self.isOwn(row) && ['DRAFT', 'SUBMITTED', 'PENDING_APPROVAL'].indexOf(row.status) !== -1;
    };

    self.hasActions = function (row) {
      return self.canDecide(row) || self.canCancel(row);
    };

    self.statusClass = function (row) {
      return 'mb-pill mb-pill--' + fmt.toneFor(row.status);
    };
    self.fmtCreated = function (row) {
      return fmt.dateTime(row.createdAt);
    };

    self.dialogTitle = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return 'Application action';
      }
      return payload.action === 'approve' ? 'Approve application' : payload.action === 'reject' ? 'Reject application' : 'Cancel application';
    });

    self.needsReason = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return !!payload && payload.action !== 'cancel';
    });

    self.reasonLabel = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return payload && payload.action === 'reject' ? 'Rejection reason' : 'Remarks (optional)';
    });

    self.dialogSummary = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return '';
      }
      return fmt.humanize(payload.action) + ' ' + payload.row.applicationReference + ' for ' + payload.row.cifNo + '?';
    });

    self.confirmLabel = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (self.busy()) {
        return 'Working…';
      }
      return payload ? fmt.humanize(payload.action) + ' application' : 'Continue';
    });

    function begin(row, action) {
      self.reason('');
      self.dismissBanner();
      self.openConfirm({ row: row, action: action });
    }

    self.approveRow = function (row) { begin(row, 'approve'); };
    self.rejectRow = function (row) { begin(row, 'reject'); };
    self.cancelRow = function (row) { begin(row, 'cancel'); };

    self.confirm = function () {
      var payload = self.confirmPayload();
      var note = (self.reason() || '').trim();
      if (!payload) {
        return;
      }
      if (payload.action === 'reject' && !note) {
        self.notify('error', 'Reason required', 'Enter why the application is being rejected.');
        return;
      }

      self.runConfirm(function (committed, intent) {
        var id = committed.row.applicationId;
        if (committed.action === 'approve') {
          return endpoints.accounts.approveApplication(id, note ? { remarks: note } : {}, intent.idempotencyKey);
        }
        if (committed.action === 'reject') {
          return endpoints.accounts.rejectApplication(id, { reason: note }, intent.idempotencyKey);
        }
        return endpoints.accounts.cancelApplication(id, intent.idempotencyKey);
      }).then(function (result) {
        if (result === null) {
          return;
        }
        var action = payload.action === 'cancel' ? 'cancelled' : payload.action === 'approve' ? 'approved' : 'rejected';
        var accountNote = result && result.createdAccountId ? ' Account ' + result.createdAccountId + ' was created.' : '';
        self.notify('success', 'Application ' + action, payload.row.applicationReference + ' was ' + action + '.' + accountNote);
        providers.refreshProvider(self.provider);
        loadMetrics();
      }).catch(function (error) {
        self.failed('Application action failed', error);
      });
    };

    loadMetrics();
  }

  return ApplicationsViewModel;
});
