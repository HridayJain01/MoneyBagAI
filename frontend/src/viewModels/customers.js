/**
 * Customer search.
 *
 * GET /api/v1/customers/search takes a required `query` and returns a bare List
 * — customer-service offers no paging and no sorting here, so the UI must not
 * imply either. With no query we fall back to GET /api/v1/customers, which is
 * also unpaged; results are capped client-side to keep the page honest about
 * what it is showing.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/session',
  '../services/format',
  '../services/http',
  '../services/navigation',
  'ojs/ojbutton'
], function (ko, endpoints, session, fmt, http, navigation) {
  'use strict';

  var MAX_ROWS = 100;

  function CustomersViewModel() {
    var self = this;

    self.term = ko.observable('');
    self.submitted = ko.observable('');
    self.loading = ko.observable(true);
    self.error = ko.observable(null);
    self.rows = ko.observableArray([]);
    self.totalFound = ko.observable(0);

    self.hasError = ko.pureComputed(function () {
      return !!self.error();
    });

    self.isEmpty = ko.pureComputed(function () {
      return !self.loading() && !self.hasError() && self.rows().length === 0;
    });

    self.capped = ko.pureComputed(function () {
      return self.totalFound() > MAX_ROWS;
    });

    self.heading = ko.pureComputed(function () {
      var q = self.submitted();
      return q ? 'Results for “' + q + '”' : 'All customers';
    });

    self.subtitle = ko.pureComputed(function () {
      if (self.loading()) {
        return '';
      }
      if (self.capped()) {
        return 'Showing first ' + MAX_ROWS + ' of ' + self.totalFound() + ' — narrow your search';
      }
      var n = self.totalFound();
      return n + (n === 1 ? ' customer' : ' customers');
    });

    self.emptyMessage = ko.pureComputed(function () {
      return self.submitted() ? 'Try a different search term.' : 'No customers exist yet.';
    });

    function load() {
      self.loading(true);
      self.error(null);

      var query = (self.submitted() || '').trim();
      var call = query ? endpoints.customers.search(query) : endpoints.customers.list();

      call
        .then(function (result) {
          var list = Array.isArray(result) ? result : [];
          self.totalFound(list.length);
          self.rows(
            list.slice(0, MAX_ROWS).map(function (c) {
              var name = fmt.fullName(c.firstName, c.lastName);
              var meta = [c.cifNo, c.mobile, c.email].filter(Boolean).join(' · ');
              return {
                cifNo: c.cifNo,
                name: name,
                initials: session.initials(name),
                meta: meta,
                status: c.status,
                statusClass: 'mb-pill mb-pill--' + fmt.toneFor(c.status),
                kycStatus: c.kycStatus,
                kycClass: 'mb-pill mb-pill--' + fmt.toneFor(c.kycStatus)
              };
            })
          );
        })
        .catch(function (err) {
          if (!err || !err.isSessionExpired) {
            self.error(http.messageFor(err));
          }
        })
        .then(function () {
          self.loading(false);
        });
    }

    self.search = function () {
      self.submitted(self.term());
      load();
    };

    self.onKeyUp = function (data, event) {
      if (event.key === 'Enter') {
        self.search();
      }
      return true;
    };

    // Navigation goes through the navigation module rather than the binding
    // root, which is not the app controller inside a foreach template.
    self.openCustomer = function (cifNo) {
      navigation.openCustomer(cifNo);
    };

    load();
  }

  return CustomersViewModel;
});
