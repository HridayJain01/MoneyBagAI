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
          session.setSession(result);
          if (typeof params.onSignedIn === 'function') {
            params.onSignedIn();
          }
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
