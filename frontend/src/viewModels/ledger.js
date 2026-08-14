/**
 * Ledger — a thin configuration of the shared generic list screen.
 * See viewModels/genericList.js for how columns are derived from the response.
 */
define(['./genericList', '../services/endpoints'], function (GenericList, endpoints) {
  'use strict';

  function LedgerViewModel() {
    GenericList.call(this, {
      title: 'Ledger',
      lede: 'Journals posted by the double-entry ledger.',
      load: function () {
        return endpoints.ledger.journals({ page: 0, size: 50 });
      }
    });
  }

  LedgerViewModel.prototype = Object.create(GenericList.prototype);
  LedgerViewModel.prototype.constructor = LedgerViewModel;

  return LedgerViewModel;
});
