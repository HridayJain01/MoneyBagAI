/** Purpose-built, read-only double-entry ledger explorer. */
define(['knockout', '../services/endpoints', '../services/format', '../services/http'], function (ko, endpoints, fmt, http) {
  'use strict';

  function LedgerViewModel() {
    var self = this;

    self.loading = ko.observable(true);
    self.error = ko.observable('');
    self.accounts = ko.observableArray([]);
    self.journals = ko.observableArray([]);
    self.selectedJournal = ko.observable(null);
    self.transactionId = ko.observable('');
    self.customerAccountId = ko.observable('');

    self.assetCount = ko.pureComputed(function () {
      return self.accounts().filter(function (row) { return row.type === 'ASSET'; }).length;
    });
    self.liabilityCount = ko.pureComputed(function () {
      return self.accounts().filter(function (row) { return row.type === 'LIABILITY'; }).length;
    });

    function decorateAccount(row) {
      row.balanceDisplay = fmt.money(row.balance, row.currencyCode);
      row.typeDisplay = fmt.humanize(row.type);
      row.sideDisplay = fmt.humanize(row.normalSide);
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.active ? 'ACTIVE' : 'CLOSED');
      return row;
    }

    function decorateJournal(row) {
      row.createdDisplay = fmt.dateTime(row.createdAt);
      row.amountDisplay = fmt.money(row.totalDebit, row.currencyCode);
      row.typeDisplay = fmt.humanize(row.journalType);
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.status);
      row.balanced = Number(row.totalDebit) === Number(row.totalCredit);
      row.lines = (row.lines || []).map(function (line) {
        line.amountDisplay = fmt.money(line.amount, row.currencyCode);
        line.sideClass = 'mb-pill mb-pill--' + (line.side === 'DEBIT' ? 'info' : 'success');
        return line;
      });
      row.inspect = function () { self.selectedJournal(row); };
      row.closeDetail = function () { self.selectedJournal(null); };
      return row;
    }

    self.load = function () {
      self.loading(true);
      self.error('');
      var query = {
        transactionId: (self.transactionId() || '').trim() || undefined,
        customerAccountId: (self.customerAccountId() || '').trim() || undefined
      };
      return Promise.all([endpoints.ledger.accounts(), endpoints.ledger.journals(query)])
        .then(function (parts) {
          self.accounts((parts[0] || []).map(decorateAccount));
          self.journals((parts[1] || []).map(decorateJournal));
          var selected = self.selectedJournal();
          if (selected) {
            self.selectedJournal(self.journals().find(function (row) { return row.id === selected.id; }) || null);
          }
        })
        .catch(function (err) {
          if (!err || !err.isSessionExpired) {
            self.error(http.messageFor(err));
          }
        })
        .then(function () { self.loading(false); });
    };

    self.applyFilters = self.load;
    self.clearFilters = function () {
      self.transactionId('');
      self.customerAccountId('');
      self.selectedJournal(null);
      self.load();
    };
    self.load();
  }

  return LedgerViewModel;
});
