/**
 * Account-opening maker flow.
 *
 * Eligibility is a server-owned gate and is resolved before products are shown.
 * The selected product is fetched again so funding rules are checked against its
 * current detail, not a potentially stale list row. Approval remains a separate
 * checker action in applications.js.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/navigation',
  '../services/format',
  '../services/http',
  './support/banner',
  './support/confirm',
  'ojs/ojbutton',
  'ojs/ojdialog'
], function (ko, endpoints, navigation, fmt, http, Banner, Confirm) {
  'use strict';

  function OpenAccountViewModel() {
    var self = this;

    Banner.call(self);
    Confirm.call(self, { dialogId: 'openAccountConfirmDialog' });

    self.cifNo = ko.observable(navigation.param('cif') || '');
    self.eligibility = ko.observable(null);
    self.checkingEligibility = ko.observable(false);
    self.products = ko.observableArray([]);
    self.loadingProducts = ko.observable(false);
    self.productCode = ko.observable('');
    self.product = ko.observable(null);
    self.loadingProduct = ko.observable(false);
    self.accountName = ko.observable('');
    self.initialDeposit = ko.observable('');
    self.result = ko.observable(null);
    self.fieldErrors = ko.observable({});

    self.isEligible = ko.pureComputed(function () {
      var value = self.eligibility();
      return !!value && value.eligible === true;
    });

    self.requiresFunding = ko.pureComputed(function () {
      return !!(self.product() && self.product().requiresFunding);
    });

    self.minimumOpening = ko.pureComputed(function () {
      var product = self.product();
      return product && product.minOpeningDeposit !== null && product.minOpeningDeposit !== undefined
        ? Number(product.minOpeningDeposit)
        : 0;
    });

    self.canReview = ko.pureComputed(function () {
      if (!self.isEligible() || !self.product() || self.loadingProduct()) {
        return false;
      }
      if (!self.requiresFunding()) {
        return self.initialDeposit() === '' || Number(self.initialDeposit()) >= 0;
      }
      var amount = Number(self.initialDeposit());
      return isFinite(amount) && amount >= self.minimumOpening();
    });

    self.productSummary = ko.pureComputed(function () {
      var product = self.product();
      if (!product) {
        return '';
      }
      var details = [fmt.humanize(product.productType), product.currency];
      if (product.tenureMonths) {
        details.push(product.tenureMonths + ' months');
      }
      return details.join(' · ');
    });

    self.depositHint = ko.pureComputed(function () {
      if (!self.product()) {
        return 'Select a product first.';
      }
      if (self.requiresFunding()) {
        return 'Required now · minimum ' + fmt.money(self.minimumOpening(), self.product().currency);
      }
      return self.minimumOpening() > 0
        ? 'Optional here · product minimum ' + fmt.money(self.minimumOpening(), self.product().currency)
        : 'Optional for this product.';
    });

    self.resultStatusClass = ko.pureComputed(function () {
      return 'mb-pill mb-pill--' + fmt.toneFor(self.result() && self.result().status);
    });

    function resetAfterCustomerChange() {
      self.eligibility(null);
      self.productCode('');
      self.product(null);
      self.accountName('');
      self.initialDeposit('');
      self.fieldErrors({});
    }

    self.checkEligibility = function () {
      var cif = (self.cifNo() || '').trim().toUpperCase();
      if (!cif || self.checkingEligibility()) {
        self.notify('error', 'CIF required', 'Enter the customer CIF before continuing.');
        return;
      }
      resetAfterCustomerChange();
      self.cifNo(cif);
      self.checkingEligibility(true);
      self.dismissBanner();
      endpoints.customers
        .eligibility(cif)
        .then(function (result) {
          self.eligibility(result);
          if (!result.eligible) {
            self.notify('warning', 'Customer is not eligible', (result.reasons || []).join(' · '));
          }
        })
        .catch(function (error) {
          self.failed('Eligibility could not be checked', error);
        })
        .then(function () {
          self.checkingEligibility(false);
        });
    };

    self.productCode.subscribe(function (code) {
      self.product(null);
      self.initialDeposit('');
      if (!code) {
        return;
      }
      self.loadingProduct(true);
      self.dismissBanner();
      endpoints.products
        .get(code)
        .then(function (detail) {
          self.product(detail);
        })
        .catch(function (error) {
          self.failed('Product details could not be loaded', error);
        })
        .then(function () {
          self.loadingProduct(false);
        });
    });

    function requestBody() {
      var product = self.product();
      var body = {
        cifNo: self.cifNo(),
        productCode: product.productCode,
        currency: product.currency
      };
      var name = (self.accountName() || '').trim();
      if (name) {
        body.accountName = name;
      }
      if (self.initialDeposit() !== '') {
        body.initialDeposit = Number(self.initialDeposit());
      }
      return body;
    }

    self.review = function () {
      if (!self.canReview()) {
        self.notify('error', 'Opening request is incomplete', 'Check eligibility, select a product, and meet its funding minimum.');
        return;
      }
      self.dismissBanner();
      self.fieldErrors({});
      self.openConfirm({ body: requestBody(), product: self.product() });
    };

    self.dialogSummary = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return '';
      }
      var funding = payload.body.initialDeposit !== undefined
        ? ' with ' + fmt.money(payload.body.initialDeposit, payload.body.currency) + ' requested funding'
        : '';
      return 'Create a ' + payload.product.productName + ' application for ' + payload.body.cifNo + funding + '?';
    });

    self.confirmLabel = ko.pureComputed(function () {
      return self.busy() ? 'Submitting…' : 'Create application';
    });

    self.confirm = function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return;
      }
      self.runConfirm(function (committed, intent) {
        return endpoints.accounts.createApplication(committed.body, intent.idempotencyKey);
      }).then(function (created) {
        if (created === null) {
          return;
        }
        self.result(created);
        self.notify('success', 'Application created', (created.applicationReference || 'The application') + ' is ready for checker review.');
      }).catch(function (error) {
        self.fieldErrors(http.fieldErrorsFor(error) || {});
        self.failed('Application could not be created', error);
      });
    };

    self.openQueue = function () {
      navigation.openApplications();
    };

    self.startAnother = function () {
      self.result(null);
      self.cifNo('');
      resetAfterCustomerChange();
      self.dismissBanner();
    };

    self.loadingProducts(true);
    endpoints.products.list({ status: 'ACTIVE' }).then(function (items) {
      self.products(Array.isArray(items) ? items : []);
    }).catch(function (error) {
      self.failed('Products could not be loaded', error);
    }).then(function () {
      self.loadingProducts(false);
    });

    if (self.cifNo()) {
      self.checkEligibility();
    }
  }

  return OpenAccountViewModel;
});
