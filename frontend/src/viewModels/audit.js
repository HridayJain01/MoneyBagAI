/**
 * Audit trail — a thin configuration of the shared generic list screen.
 * See viewModels/genericList.js for how columns are derived from the response.
 */
define(['./genericList', '../services/endpoints'], function (GenericList, endpoints) {
  'use strict';

  function AuditViewModel() {
    GenericList.call(this, {
      title: 'Audit trail',
      lede: 'Every actor-attributed change recorded across the services.',
      load: function () {
        return endpoints.audit.events({ page: 0, size: 50 });
      }
    });
  }

  AuditViewModel.prototype = Object.create(GenericList.prototype);
  AuditViewModel.prototype.constructor = AuditViewModel;

  return AuditViewModel;
});
