/**
 * Bank-wide transaction search.
 *
 * transaction-service returns Spring's Page — shape 'spring'. Contrast with the
 * account browser, which uses the 'envelope' shape against account-service.
 */
define([
  'knockout',
  '../services/providers',
  '../services/format',
  'ojs/ojtable',
  'ojs/ojbutton'
], function (ko, providers, fmt) {
  'use strict';

  function TransactionsViewModel() {
    var self = this;

    self.reference = ko.observable('');
    self.status = ko.observable('');
    self.statusList = ['', 'PENDING', 'PENDING_APPROVAL', 'SETTLED', 'FAILED', 'REJECTED', 'REVERSED'];

    self.provider = providers.pagedProvider({
      url: '/api/v1/transactions',
      idKey: 'transactionId',
      shape: 'spring',
      pageSize: 25,
      sort: 'createdAt,desc',
      query: function () {
        return {
          reference: self.reference() || undefined,
          status: self.status() || undefined
        };
      }
    });

    self.columns = [
      { headerText: 'Reference', field: 'transactionReference', template: 'refTemplate' },
      { headerText: 'Type', field: 'type', template: 'typeTemplate' },
      { headerText: 'Rail', field: 'rail' },
      { headerText: 'Status', field: 'status', template: 'statusTemplate' },
      { headerText: 'Amount', field: 'amount', template: 'amountTemplate' },
      { headerText: 'Created', field: 'createdAt', template: 'whenTemplate' }
    ];

    self.applyFilters = function () {
      providers.refreshProvider(self.provider);
    };

    self.fmtType = function (row) {
      return fmt.humanize(row.type);
    };
    self.fmtWhen = function (row) {
      return fmt.dateTime(row.createdAt);
    };
    self.fmtAmount = function (row) {
      var dir = fmt.direction(row);
      return (dir === 'credit' ? '+' : '−') + fmt.money(Math.abs(row.amount), row.currency);
    };
    self.amountClass = function (row) {
      return 'mb-money mb-money--' + fmt.direction(row);
    };
    self.statusClassFor = function (row) {
      return 'mb-pill mb-pill--' + fmt.toneFor(row.status);
    };
  }

  return TransactionsViewModel;
});
