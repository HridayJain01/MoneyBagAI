/**
 * Notifications — a thin configuration of the shared generic list screen.
 * See viewModels/genericList.js for how columns are derived from the response.
 */
define(['./genericList', '../services/endpoints'], function (GenericList, endpoints) {
  'use strict';

  function NotificationsViewModel() {
    GenericList.call(this, {
      title: 'Notifications',
      lede: 'Outbound customer messages and their delivery state.',
      load: function () {
        return endpoints.notifications.list({ page: 0, size: 50 });
      }
    });
  }

  NotificationsViewModel.prototype = Object.create(GenericList.prototype);
  NotificationsViewModel.prototype.constructor = NotificationsViewModel;

  return NotificationsViewModel;
});
