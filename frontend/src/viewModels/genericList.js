/**
 * Shared read-only list screen for the remaining services.
 *
 * These deliberately stop at "list and inspect". They exist so every service is
 * reachable from the console; write flows for them are a later pass.
 *
 * Columns are derived from whatever the service actually returns, which avoids
 * inventing a schema for endpoints whose DTOs are not yet pinned down on the
 * client. Nested objects are skipped — they do not read well in a flat table.
 */
define(['knockout', '../services/format', '../services/http'], function (ko, fmt, http) {
  'use strict';

  /** Columns worth showing first; everything else follows in encounter order. */
  var PREFERRED = [
    'id',
    'eventId',
    'notificationId',
    'productCode',
    'branchCode',
    'employeeId',
    'journalId',
    'reference',
    'name',
    'action',
    'channel',
    'status',
    'createdAt',
    'occurredAt'
  ];

  var MAX_COLUMNS = 7;

  function columnsOf(rows) {
    var seen = [];
    rows.slice(0, 20).forEach(function (row) {
      Object.keys(row).forEach(function (key) {
        var value = row[key];
        if ((value === null || typeof value !== 'object') && seen.indexOf(key) === -1) {
          seen.push(key);
        }
      });
    });
    var preferred = PREFERRED.filter(function (c) {
      return seen.indexOf(c) !== -1;
    });
    var rest = seen.filter(function (c) {
      return preferred.indexOf(c) === -1;
    });
    return preferred.concat(rest).slice(0, MAX_COLUMNS);
  }

  /** Pulls the row array out of whichever envelope the service used. */
  function rowsOf(body) {
    if (Array.isArray(body)) {
      return body;
    }
    var candidate = body && (body.items || body.content);
    return Array.isArray(candidate) ? candidate : [];
  }

  /**
   * Shared constructor, not a routed module: the thin screens call it with
   * `GenericList.call(this, {...})` so they *become* a generic list. Nesting an
   * ojModule and passing params was the obvious alternative, but that binding
   * does not forward params to the view-model constructor.
   *
   * options: { title, lede, load() }
   */
  function GenericListViewModel(options) {
    var self = this;
    var params = options || {};

    self.title = params.title || 'Records';
    self.lede = params.lede || '';

    self.loading = ko.observable(true);
    self.error = ko.observable(null);
    self.columns = ko.observableArray([]);
    self.rows = ko.observableArray([]);
    self.count = ko.observable(0);

    self.hasError = ko.pureComputed(function () {
      return !!self.error();
    });

    self.isEmpty = ko.pureComputed(function () {
      return !self.loading() && !self.hasError() && self.rows().length === 0;
    });

    self.subtitle = ko.pureComputed(function () {
      if (self.loading()) {
        return '';
      }
      var n = self.count();
      return n + (n === 1 ? ' row' : ' rows');
    });

    function cellFor(value) {
      if (value === null || value === undefined || value === '') {
        return { text: '—', isPill: false, cls: 'mb-muted' };
      }
      var text = String(value);
      // Enum-looking values read better as status pills.
      if (/^[A-Z_]{3,}$/.test(text)) {
        return { text: text.replace(/_/g, ' '), isPill: true, cls: 'mb-pill mb-pill--' + fmt.toneFor(text) };
      }
      return { text: text, isPill: false, cls: '' };
    }

    if (typeof params.load !== 'function') {
      self.error('This screen is misconfigured: no loader was supplied.');
      self.loading(false);
      return;
    }

    params
      .load()
      .then(function (body) {
        var rows = rowsOf(body);
        var columns = columnsOf(rows);

        self.count(rows.length);
        self.columns(
          columns.map(function (key) {
            return { key: key, label: fmt.labelize(key) };
          })
        );
        self.rows(
          rows.map(function (row) {
            return columns.map(function (key) {
              return cellFor(row[key]);
            });
          })
        );
      })
      .catch(function (err) {
        if (!err || !err.isSessionExpired) {
          self.error(http.messageFor(err));
        }
      })
      .then(function () {
        self.loading(false);
      });
  }

  return GenericListViewModel;
});
