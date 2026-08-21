/**
 * Branch-manager End of Day review.
 *
 * There is deliberately no inferred till close, vault balance, physical count,
 * or persisted EOD status here: the current domain has no authoritative record
 * for those values. Missing controls render as an em dash. All populated money
 * and activity values come from existing branch, employee, transaction, and
 * reporting endpoints.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/format',
  '../services/http',
  '../services/session'
], function (ko, endpoints, fmt, http, session) {
  'use strict';

  var PAGE_SIZE = 100;

  function localDateValue(date) {
    var year = date.getFullYear();
    var month = String(date.getMonth() + 1).padStart(2, '0');
    var day = String(date.getDate()).padStart(2, '0');
    return year + '-' + month + '-' + day;
  }

  function nextDateValue(value) {
    var parts = value.split('-').map(Number);
    var date = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2]));
    date.setUTCDate(date.getUTCDate() + 1);
    return date.toISOString().slice(0, 10);
  }

  function numberValue(value) {
    var number = Number(value);
    return isFinite(number) ? number : 0;
  }

  function moneyValue(money) {
    return money ? numberValue(money.amount) : 0;
  }

  function csvCell(value) {
    var text = value === null || value === undefined ? '' : String(value);
    return '"' + text.replace(/"/g, '""') + '"';
  }

  function EodViewModel() {
    var self = this;
    var identity = session.getSession() || {};

    self.businessDate = ko.observable(localDateValue(new Date()));
    self.loading = ko.observable(false);
    self.error = ko.observable(null);
    self.lastRefreshed = ko.observable(null);
    self.branchCode = ko.observable(identity.branchCode || '');
    self.branchName = ko.observable('—');
    self.report = ko.observable(null);
    self.transactions = ko.observableArray([]);
    self.tellers = ko.observableArray([]);
    self.reportRows = ko.observableArray([]);

    self.hasError = ko.pureComputed(function () { return !!self.error(); });
    self.dateDisplay = ko.pureComputed(function () {
      return fmt.dateOnly(self.businessDate() + 'T00:00:00');
    });
    self.lastRefreshedDisplay = ko.pureComputed(function () {
      return self.lastRefreshed() ? fmt.dateTime(self.lastRefreshed()) : '—';
    });
    self.postingCount = ko.pureComputed(function () {
      var report = self.report();
      return report ? report.transactionCount : '—';
    });
    self.debitTotal = ko.pureComputed(function () {
      var report = self.report();
      return report ? fmt.money(moneyValue(report.debitTotal), report.debitTotal.currency) : '—';
    });
    self.creditTotal = ko.pureComputed(function () {
      var report = self.report();
      return report ? fmt.money(moneyValue(report.creditTotal), report.creditTotal.currency) : '—';
    });

    function isCompleted(tx) {
      return tx.status === 'COMPLETED' || tx.status === 'SETTLED';
    }

    function sumCash(type, rows) {
      return rows.filter(function (tx) {
        return tx.rail === 'CASH' && tx.type === type && isCompleted(tx);
      }).reduce(function (sum, tx) { return sum + numberValue(tx.amount); }, 0);
    }

    self.cashDeposits = ko.pureComputed(function () {
      return fmt.money(sumCash('DEPOSIT', self.transactions()), 'INR');
    });
    self.cashWithdrawals = ko.pureComputed(function () {
      return fmt.money(sumCash('WITHDRAWAL', self.transactions()), 'INR');
    });
    self.netCashMovement = ko.pureComputed(function () {
      var rows = self.transactions();
      return fmt.money(sumCash('DEPOSIT', rows) - sumCash('WITHDRAWAL', rows), 'INR');
    });

    function loadAllTransactions(query, page, accumulated) {
      return endpoints.transactions.search(Object.assign({}, query, {
        page: page,
        size: PAGE_SIZE,
        sort: 'createdAt,asc'
      })).then(function (response) {
        var rows = accumulated.concat((response && response.content) || []);
        var totalPages = (response && response.totalPages) || 0;
        return page + 1 < totalPages
          ? loadAllTransactions(query, page + 1, rows)
          : rows;
      });
    }

    function decorateReport(report) {
      var groups = {};
      ((report && report.transactions) || []).forEach(function (entry) {
        var key = entry.type || 'UNKNOWN';
        if (!groups[key]) {
          groups[key] = { type: key, count: 0, debit: 0, credit: 0, currency: 'INR' };
        }
        var group = groups[key];
        var amount = moneyValue(entry.amount);
        group.count += 1;
        group.currency = (entry.amount && entry.amount.currency) || group.currency;
        if (entry.direction === 'DEBIT') {
          group.debit += amount;
        } else if (entry.direction === 'CREDIT') {
          group.credit += amount;
        }
      });
      self.reportRows(Object.keys(groups).sort().map(function (key) {
        var group = groups[key];
        return {
          type: fmt.humanize(group.type),
          count: group.count,
          debit: fmt.money(group.debit, group.currency),
          credit: fmt.money(group.credit, group.currency)
        };
      }));
    }

    function tellerRows(branch, employees, transactions) {
      var tellers = employees.filter(function (employee) {
        return Number(employee.branchId) === Number(branch.id) &&
          String(employee.designation || '').toLowerCase().indexOf('teller') !== -1 &&
          employee.status === 'ACTIVE';
      });

      return Promise.all(tellers.map(function (employee) {
        return endpoints.identity.userSummary(employee.userId).catch(function () { return null; }).then(function (user) {
          var rows = transactions.filter(function (tx) {
            return String(tx.makerEmployeeId) === String(employee.id);
          });
          var cashIn = sumCash('DEPOSIT', rows);
          var cashOut = sumCash('WITHDRAWAL', rows);
          return {
            id: employee.id,
            employee: (user && user.username) || employee.employeeCode,
            employeeCode: employee.employeeCode,
            completed: rows.filter(isCompleted).length,
            cashIn: fmt.money(cashIn, 'INR'),
            cashOut: fmt.money(cashOut, 'INR'),
            expectedMovement: fmt.money(cashIn - cashOut, 'INR'),
            actualCash: '—',
            variance: '—',
            closureStatus: '—'
          };
        });
      }));
    }

    self.refresh = function () {
      if (!self.branchCode()) {
        self.error('The signed-in manager has no branch assignment.');
        return;
      }
      self.loading(true);
      self.error(null);
      var date = self.businessDate();
      var from = date + 'T00:00:00.000Z';
      var to = nextDateValue(date) + 'T00:00:00.000Z';

      Promise.all([
        endpoints.branches.list(),
        endpoints.branches.employees(),
        endpoints.statements.branchDailyTransactions(self.branchCode(), date),
        loadAllTransactions({ from: from, to: to }, 0, [])
      ]).then(function (parts) {
        var branches = parts[0] || [];
        var branch = branches.find(function (item) { return item.branchCode === self.branchCode(); });
        if (!branch) {
          throw new Error('Branch ' + self.branchCode() + ' was not found.');
        }
        var branchTransactions = parts[3].filter(function (tx) {
          return tx.branchCode === self.branchCode();
        });
        self.branchName(branch.name || branch.branchCode);
        self.report(parts[2]);
        self.transactions(branchTransactions);
        decorateReport(parts[2]);
        return tellerRows(branch, parts[1] || [], branchTransactions);
      }).then(function (rows) {
        self.tellers(rows);
        self.lastRefreshed(new Date().toISOString());
      }).catch(function (err) {
        self.error(http.messageFor(err));
        self.report(null);
        self.transactions([]);
        self.tellers([]);
        self.reportRows([]);
      }).then(function () {
        self.loading(false);
      });
    };

    self.downloadCsv = function () {
      var report = self.report();
      if (!report) { return; }
      var lines = [[
        'Transaction ID', 'Ledger entry', 'Reference', 'Posted at', 'Type',
        'Direction', 'Narration', 'Amount', 'Currency'
      ]];
      (report.transactions || []).forEach(function (entry) {
        lines.push([
          entry.transactionId, entry.ledgerEntryId, entry.reference, entry.postedAt,
          entry.type, entry.direction, entry.narration,
          entry.amount && entry.amount.amount, entry.amount && entry.amount.currency
        ]);
      });
      var csv = lines.map(function (row) { return row.map(csvCell).join(','); }).join('\r\n');
      var url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
      var link = document.createElement('a');
      link.href = url;
      link.download = 'eod-' + self.branchCode() + '-' + self.businessDate() + '.csv';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    };

    self.refresh();
  }

  return EodViewModel;
});
