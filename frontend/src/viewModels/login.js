/**
 * Sign in.
 *
 * POST /api/v1/auth/login is one of the gateway's public paths, so this is the
 * only screen that runs without a session. Only employees authenticate —
 * customers are data in this system, never callers.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/session',
  '../services/http',
  'ojs/ojinputtext',
  'ojs/ojbutton',
  'ojs/ojmessages',
  'ojs/ojformlayout'
], function (ko, endpoints, session, http) {
  'use strict';

  var EMPLOYEE_ROLES = ['TELLER', 'CHECKER', 'BRANCH_MANAGER', 'OPS_ADMIN'];

  function LoginViewModel(context) {
    var self = this;
    var params = (context && context.params) || {};

    self.username = ko.observable('');
    self.password = ko.observable('');
    self.busy = ko.observable(false);
    self.error = ko.observable(null);
    self.hasError = ko.pureComputed(function () {
      return !!self.error();
    });

    self.submitLabel = ko.pureComputed(function () {
      return self.busy() ? 'Signing in…' : 'Sign in';
    });

    self.submit = function () {
      if (self.busy()) {
        return;
      }
      var username = (self.username() || '').trim();
      var password = self.password() || '';

      if (!username || !password) {
        self.error('Enter your username and password.');
        return;
      }

      self.busy(true);
      self.error(null);

      endpoints.auth
        .login({ username: username, password: password })
        .then(function (result) {
          var roles = result.roles || [];
          if (!roles.some(function (role) { return EMPLOYEE_ROLES.indexOf(role) !== -1; })) {
            self.error('This is the employee operations console. Customer logins must use the customer banking application.');
            return endpoints.auth.logout().catch(function () {}).then(function () { return null; });
          }
          session.setSession(result);
          if (typeof params.onSignedIn === 'function') {
            params.onSignedIn();
          }
          return result;
        })
        .catch(function (err) {
          // A failed login is expected traffic, so it renders inline rather
          // than as a toast.
          self.error(http.messageFor(err));
        })
        .then(function () {
          self.busy(false);
        });
    };

    // Enter anywhere in the form submits.
    self.onKeyUp = function (data, event) {
      if (event.key === 'Enter') {
        self.submit();
      }
      return true;
    };
  }

  return LoginViewModel;
});
