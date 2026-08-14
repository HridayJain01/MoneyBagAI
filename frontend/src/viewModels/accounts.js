/**
 * Account browser.
 *
 * account-service returns its own PageResponse envelope ({ items, totalItems })
 * rather than Spring's Page — hence shape 'envelope'. Getting this wrong
 * renders an empty grid against a 200.
 */
define([
  'knockout',
  '../services/providers',
  '../services/format',
  '../services/navigation',
  'ojs/ojtable',
  'ojs/ojbutton'
], function (ko, providers, fmt, navigation) {
  'use strict';

  function AccountsViewModel() {
    var self = this;

    self.cifNo = ko.observable('');
    self.status = ko.observable('');
    self.statusList = ['', 'ACTIVE', 'FROZEN', 'BLOCKED', 'DORMANT', 'CLOSED'];

    self.provider = providers.pagedProvider({
      url: '/api/v1/accounts',
      idKey: 'accountId',
      shape: 'envelope',
      pageSize: 25,
      query: function () {
        return {
          cifNo: self.cifNo() || undefined,
          status: self.status() || undefined
        };
      }
    });

    self.columns = [
      { headerText: 'Account', field: 'accountName', template: 'nameTemplate' },
      { headerText: 'CIF', field: 'cifNo' },
      { headerText: 'Product', field: 'productCode' },
      { headerText: 'Status', field: 'status', template: 'statusTemplate' },
      { headerText: 'Available', field: 'availableBalance', template: 'balanceTemplate' },
      { headerText: '', template: 'actionsTemplate', sortable: 'disabled' }
    ];

    self.applyFilters = function () {
      providers.refreshProvider(self.provider);
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
  }

  return AccountsViewModel;
});
