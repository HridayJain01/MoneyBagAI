/**
 * Branches — a thin configuration of the shared generic list screen.
 * See viewModels/genericList.js for how columns are derived from the response.
 */
define(['./genericList', '../services/endpoints'], function (GenericList, endpoints) {
  'use strict';

  function BranchesViewModel() {
    GenericList.call(this, {
      title: 'Branches',
      lede: 'The physical branch network and its addresses.',
      load: function () {
        return endpoints.branches.list({ page: 0, size: 50 });
      }
    });
  }

  BranchesViewModel.prototype = Object.create(GenericList.prototype);
  BranchesViewModel.prototype.constructor = BranchesViewModel;

  return BranchesViewModel;
});
