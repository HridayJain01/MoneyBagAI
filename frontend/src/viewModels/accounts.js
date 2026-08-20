/**
 * Account browser.
 *
 * account-service returns its own PageResponse envelope ({ items, totalItems })
 * rather than Spring's Page — hence shape 'envelope'. Getting this wrong
 * renders an empty grid against a 200.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/format',
  '../services/http',
  '../services/navigation',
  'ojs/ojarraydataprovider',
  'ojs/ojtable',
  'ojs/ojbutton'
], function (ko, endpoints, fmt, http, navigation, ArrayDataProvider) {
  'use strict';

  function AccountsViewModel() {
    var self = this;

    self.accountNumber = ko.observable('');
    self.status = ko.observable('');
    self.loading = ko.observable(true);
    self.error = ko.observable('');
    self.rows = ko.observableArray([]);
    self.statusList = ['', 'ACTIVE', 'FROZEN', 'BLOCKED', 'DORMANT', 'CLOSED'];

    self.provider = new ArrayDataProvider(self.rows, { keyAttributes: 'accountId' });

    self.columns = [
      { headerText: 'Account', field: 'accountName', template: 'nameTemplate' },
      { headerText: 'CIF', field: 'cifNo' },
      { headerText: 'Product', field: 'productCode' },
      { headerText: 'Status', field: 'status', template: 'statusTemplate' },
      { headerText: 'Available', field: 'availableBalance', template: 'balanceTemplate' },
      { headerText: '', template: 'actionsTemplate', sortable: 'disabled' }
    ];

    self.applyFilters = function () {
      self.loading(true);
      self.error('');
      var number = String(self.accountNumber() || '').trim();
      var request = number
        ? endpoints.accounts.byNumber(number).then(function (account) { return account ? [account] : []; })
        : endpoints.accounts.search({ status: self.status() || undefined, page: 0, size: 100 })
            .then(function (envelope) { return (envelope && envelope.items) || []; });

      request.then(function (rows) {
        self.rows(rows.filter(function (row) {
          return !self.status() || row.status === self.status();
        }));
      }).catch(function (err) {
        if (!err || !err.isSessionExpired) { self.error(http.messageFor(err)); }
        self.rows([]);
      }).then(function () { self.loading(false); });
    };

    // Navigation goes through the navigation module rather than $root: inside
    // an oj-table cell template the binding root is not the app controller.
    self.openAccount = function (accountId) {
      navigation.openAccount(accountId);
    };

    self.fmtBalance = function (row) {
      return fmt.money(row.availableBalance, row.currency);
    };
    self.statusClassFor = function (row) {
      return 'mb-pill mb-pill--' + fmt.toneFor(row.status);
    };

    self.applyFilters();
  }

  return AccountsViewModel;
});
