/** Searchable audit explorer with cross-service correlation tracing. */
define(['knockout', '../services/endpoints', '../services/format', '../services/http'], function (ko, endpoints, fmt, http) {
  'use strict';

  function AuditViewModel() {
    var self = this;

    self.loading = ko.observable(true);
    self.error = ko.observable('');
    self.events = ko.observableArray([]);
    self.selectedEvent = ko.observable(null);
    self.totalItems = ko.observable(0);
    self.sourceService = ko.observable('');
    self.eventType = ko.observable('');
    self.aggregateType = ko.observable('');
    self.aggregateId = ko.observable('');
    self.branchCode = ko.observable('');
    self.correlationId = ko.observable('');
    self.traceMode = ko.observable(false);

    function prettyPayload(value) {
      if (!value) { return 'No event payload was recorded.'; }
      try { return JSON.stringify(JSON.parse(value), null, 2); }
      catch (ignore) { return String(value); }
    }

    function decorate(row) {
      row.occurredDisplay = fmt.dateTime(row.occurredAt);
      row.ingestedDisplay = fmt.dateTime(row.ingestedAt);
      row.eventDisplay = fmt.humanize(row.eventType);
      row.aggregateDisplay = [fmt.humanize(row.aggregateType), row.aggregateId].filter(Boolean).join(' · ');
      row.actorDisplay = row.actorEmployeeId || row.actorUserId || 'System';
      row.httpDisplay = [row.httpMethod, row.httpPath, row.httpStatus].filter(function (value) {
        return value !== null && value !== undefined && value !== '';
      }).join(' ');
      row.payloadPretty = prettyPayload(row.payload);
      row.inspect = function () { self.selectedEvent(row); };
      row.closeDetail = function () { self.selectedEvent(null); };
      return row;
    }

    self.load = function () {
      self.loading(true);
      self.error('');
      var trace = (self.correlationId() || '').trim();
      var request;
      if (trace) {
        self.traceMode(true);
        request = endpoints.audit.trace(trace).then(function (rows) {
          return { items: rows || [], totalItems: (rows || []).length };
        });
      } else {
        self.traceMode(false);
        request = endpoints.audit.events({
          sourceService: (self.sourceService() || '').trim() || undefined,
          eventType: (self.eventType() || '').trim() || undefined,
          aggregateType: (self.aggregateType() || '').trim() || undefined,
          aggregateId: (self.aggregateId() || '').trim() || undefined,
          branchCode: (self.branchCode() || '').trim() || undefined,
          page: 0,
          size: 50
        });
      }
      return request.then(function (page) {
        self.events((page.items || []).map(decorate));
        self.totalItems(page.totalItems || 0);
        var selected = self.selectedEvent();
        if (selected) {
          self.selectedEvent(self.events().find(function (row) { return row.eventId === selected.eventId; }) || null);
        }
      }).catch(function (err) {
        if (!err || !err.isSessionExpired) {
          self.error(http.messageFor(err));
        }
      }).then(function () { self.loading(false); });
    };

    self.applyFilters = self.load;
    self.clearFilters = function () {
      self.sourceService('');
      self.eventType('');
      self.aggregateType('');
      self.aggregateId('');
      self.branchCode('');
      self.correlationId('');
      self.selectedEvent(null);
      self.load();
    };
    self.load();
  }

  return AuditViewModel;
});
