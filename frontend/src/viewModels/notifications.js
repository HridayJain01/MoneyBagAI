/** Notification delivery monitor and template catalogue. */
define(['knockout', '../services/endpoints', '../services/format', '../services/http'], function (ko, endpoints, fmt, http) {
  'use strict';

  function NotificationsViewModel() {
    var self = this;

    self.loading = ko.observable(true);
    self.error = ko.observable('');
    self.notifications = ko.observableArray([]);
    self.templates = ko.observableArray([]);
    self.selectedNotification = ko.observable(null);
    self.status = ko.observable('');
    self.cifNo = ko.observable('');
    self.recipient = ko.observable('');
    self.statuses = ['', 'PENDING', 'SENT', 'FAILED', 'SUPPRESSED'];
    self.totalItems = ko.observable(0);

    function decorateNotification(row) {
      row.createdDisplay = fmt.dateTime(row.createdAt);
      row.sentDisplay = fmt.dateTime(row.sentAt);
      row.channelDisplay = fmt.humanize(row.channel);
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.status);
      row.inspect = function () { self.selectedNotification(row); };
      row.closeDetail = function () { self.selectedNotification(null); };
      return row;
    }

    function decorateTemplate(row) {
      row.updatedDisplay = fmt.dateTime(row.updatedAt);
      row.channelDisplay = fmt.humanize(row.channel);
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.active ? 'ACTIVE' : 'CLOSED');
      return row;
    }

    self.load = function () {
      self.loading(true);
      self.error('');
      return Promise.all([
        endpoints.notifications.list({
          status: self.status() || undefined,
          cifNo: (self.cifNo() || '').trim() || undefined,
          recipient: (self.recipient() || '').trim() || undefined,
          page: 0,
          size: 50
        }),
        endpoints.notifications.templates()
      ]).then(function (parts) {
        var page = parts[0] || {};
        self.notifications((page.items || []).map(decorateNotification));
        self.totalItems(page.totalItems || 0);
        self.templates((parts[1] || []).map(decorateTemplate));
        var selected = self.selectedNotification();
        if (selected) {
          self.selectedNotification(self.notifications().find(function (row) { return row.notificationId === selected.notificationId; }) || null);
        }
      }).catch(function (err) {
        if (!err || !err.isSessionExpired) {
          self.error(http.messageFor(err));
        }
      }).then(function () { self.loading(false); });
    };

    self.applyFilters = self.load;
    self.clearFilters = function () {
      self.status('');
      self.cifNo('');
      self.recipient('');
      self.selectedNotification(null);
      self.load();
    };
    self.load();
  }

  return NotificationsViewModel;
});
