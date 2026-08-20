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
  '../services/locations',
  './support/banner',
  './support/confirm',
  'ojs/ojbutton',
  'ojs/ojdialog'
], function (ko, endpoints, session, fmt, http, navigation, locations, Banner, Confirm) {
  'use strict';

  var MAX_ROWS = 100;

  function CustomersViewModel() {
    var self = this;

    Banner.call(self);
    Confirm.call(self, { dialogId: 'customerOnboardingDialog' });

    self.term = ko.observable('');
    self.submitted = ko.observable('');
    self.loading = ko.observable(true);
    self.error = ko.observable(null);
    self.rows = ko.observableArray([]);
    self.totalFound = ko.observable(0);
    self.canCreate = session.hasPermission('CUSTOMER_UPDATE');
    self.isTeller = session.hasRole('TELLER');
    self.isChecker = session.hasRole('CHECKER') && session.hasPermission('KYC_VERIFY');
    self.formFirstName = ko.observable('');
    self.formLastName = ko.observable('');
    self.formDob = ko.observable('');
    self.formGender = ko.observable('');
    self.formMobile = ko.observable('');
    self.formEmail = ko.observable('');
    self.formPassword = ko.observable('');
    self.formPan = ko.observable('');
    self.formAddress = ko.observable('');
    self.formCity = ko.observable('');
    self.formState = ko.observable('');
    self.formPincode = ko.observable('');
    self.stateOptions = locations.states;
    self.cityOptions = ko.pureComputed(function () {
      return locations.citiesFor(self.formState());
    });
    self.formError = ko.observable('');
    self.createdIdentityId = ko.observable(null);

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
          return Promise.all(
            list.slice(0, MAX_ROWS).map(function (c) {
              var name = fmt.fullName(c.firstName, c.lastName);
              var meta = [c.cifNo, c.mobile, c.email].filter(Boolean).join(' · ');
              var row = {
                cifNo: c.cifNo,
                name: name,
                initials: session.initials(name),
                meta: meta,
                status: c.status,
                statusClass: 'mb-pill mb-pill--' + fmt.toneFor(c.status),
                kycStatus: c.kycStatus,
                kycClass: 'mb-pill mb-pill--' + fmt.toneFor(c.kycStatus),
                kycWorkflowLabel: '',
                showTellerKycAction: false,
                showReviewKycAction: false,
                tellerKycLabel: 'Start KYC'
              };
              if (c.kycStatus !== 'PENDING' || (!self.isTeller && !self.isChecker)) {
                return row;
              }
              return endpoints.kyc.pendingSessions(c.cifNo).then(function (sessions) {
                var pending = Array.isArray(sessions) ? sessions : [];
                var submitted = pending.find(function (item) {
                  return item.status === 'VERIFICATION_IN_PROGRESS';
                });
                if (self.isTeller) {
                  if (submitted) {
                    row.kycWorkflowLabel = 'Submitted for Checker approval';
                  } else {
                    row.showTellerKycAction = true;
                    row.tellerKycLabel = pending.length ? 'Continue KYC' : 'Start KYC';
                  }
                }
                if (self.isChecker) {
                  row.kycWorkflowLabel = submitted
                    ? 'Submitted by Teller · awaiting review'
                    : 'KYC pending · not submitted by Teller';
                  row.showReviewKycAction = !!submitted;
                  row.reviewSessionId = submitted ? submitted.sessionId : null;
                }
                return row;
              }).catch(function () {
                row.kycWorkflowLabel = 'KYC workflow status unavailable';
                return row;
              });
            })
          ).then(function (rows) {
            self.rows(rows);
          });
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

    function returnToCustomers() {
      return window.location.origin + '/?ojr=customers';
    }

    self.openTellerKyc = function (cifNo) {
      window.location.assign('/kyc-ui/?cif=' + encodeURIComponent(cifNo) +
        '&returnTo=' + encodeURIComponent(returnToCustomers()));
    };

    self.openKycReview = function (row) {
      window.location.assign('/kyc-ui/reviewer.html?cif=' + encodeURIComponent(row.cifNo) +
        '&sessionId=' + encodeURIComponent(row.reviewSessionId) +
        '&returnTo=' + encodeURIComponent(returnToCustomers()));
    };

    function resetOnboarding() {
      self.formFirstName(''); self.formLastName(''); self.formDob(''); self.formGender('');
      self.formMobile(''); self.formEmail(''); self.formPassword(''); self.formPan('');
      self.formAddress(''); self.formCity(''); self.formState(''); self.formPincode('');
      self.formError(''); self.createdIdentityId(null);
    }

    self.beginCreate = function () {
      resetOnboarding();
      self.openConfirm({ action: 'create-customer' });
    };

    self.confirmLabel = ko.pureComputed(function () {
      return self.busy() ? 'Creating…' : 'Create customer';
    });

    function value(observable, label) {
      var result = String(observable() || '').trim();
      if (!result) { throw new Error(label + ' is required.'); }
      return result;
    }

    function onboardingBodies() {
      var firstName = value(self.formFirstName, 'First name');
      var lastName = value(self.formLastName, 'Last name');
      var dob = value(self.formDob, 'Date of birth');
      var gender = value(self.formGender, 'Gender');
      var mobile = value(self.formMobile, 'Mobile');
      var email = value(self.formEmail, 'Email').toLowerCase();
      var password = value(self.formPassword, 'Temporary password');
      var pan = value(self.formPan, 'PAN').toUpperCase();
      if (!/^[6-9][0-9]{9}$/.test(mobile)) { throw new Error('Mobile must be a valid 10-digit Indian number.'); }
      if (!/^[A-Z]{5}[0-9]{4}[A-Z]$/.test(pan)) { throw new Error('PAN must use the format ABCDE1234F.'); }
      if (password.length < 8) { throw new Error('Temporary password must contain at least 8 characters.'); }
      return {
        registration: { firstName: firstName, lastName: lastName, email: email, password: password, dob: dob, gender: gender, mobile: mobile },
        customer: { firstName: firstName, lastName: lastName, dob: dob, gender: gender, mobile: mobile, email: email, panNo: pan, status: 'INACTIVE', kycStatus: 'PENDING' },
        address: { addressType: 'RESIDENTIAL', line1: value(self.formAddress, 'Address'), city: value(self.formCity, 'City'), state: value(self.formState, 'State'), pincode: value(self.formPincode, 'Pincode'), country: 'India', isCurrent: true }
      };
    }

    self.confirmCreate = function () {
      var bodies;
      try { bodies = onboardingBodies(); } catch (validationError) {
        self.formError(validationError.message);
        return;
      }
      self.formError('');
      self.runConfirm(function (payload, intent) {
        return endpoints.auth.register(bodies.registration, intent.idempotencyKey)
          .then(function (registration) {
            var user = registration.user || {};
            self.createdIdentityId(user.userId);
            bodies.customer.userId = user.userId;
            return endpoints.customers.create(bodies.customer, intent.idempotencyKey);
          })
          .then(function (customer) {
            return endpoints.customers.addAddress(customer.cifNo, bodies.address, intent.idempotencyKey)
              .then(function () { return { customer: customer, addressSaved: true }; })
              .catch(function (error) { return { customer: customer, addressSaved: false, addressError: error }; });
          });
      }).then(function (outcome) {
        if (outcome === null) { return; }
        var customer = outcome.customer;
        self.notify(outcome.addressSaved ? 'success' : 'warning', 'Customer created',
          customer.cifNo + (outcome.addressSaved ? ' is ready for KYC.' : ' was created, but the address must be added before account opening.'));
        self.term(customer.cifNo); self.submitted(customer.cifNo); load();
      }).catch(function (error) {
        var userId = self.createdIdentityId();
        self.notify('error', 'Customer onboarding stopped',
          (userId ? 'Login user ' + userId + ' was created; do not register it again. ' : '') + http.messageFor(error));
      });
    };

    load();
  }

  return CustomersViewModel;
});
