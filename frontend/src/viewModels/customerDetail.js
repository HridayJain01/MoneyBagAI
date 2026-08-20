/**
 * Customer detail.
 *
 * Note GET /{cif}/summary returns Map<String,Object> on the Java side, so its
 * keys are unknown at compile time. It is rendered generically rather than
 * against assumed field names — hard-coding keys here would be guesswork.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/format',
  '../services/http',
  '../services/navigation',
  '../services/session',
  'ojs/ojbutton',
  'ojs/ojprogress-bar'
], function (ko, endpoints, fmt, http, navigation, session) {
  'use strict';

  function CustomerDetailViewModel(context) {
    var self = this;
    var params = (context && context.params) || {};
    // ModuleRouterAdapter does not pass router params into the module context,
    // so the navigation module is the reliable source.
    var cifNo = params.cif || navigation.param('cif');
    var tellerView = session.hasRole('TELLER');

    self.cifNo = cifNo;
    self.tab = ko.observable(tellerView ? 'accounts' : 'profile');
    self.loading = ko.observable(true);
    self.error = ko.observable(null);
    self.hasError = ko.pureComputed(function () {
      return !!self.error();
    });

    self.name = ko.observable('Customer');
    self.status = ko.observable('');
    self.kycStatus = ko.observable('');
    self.riskLabel = ko.observable('—');
    self.kycFailures = ko.observable('0');

    self.statusClass = ko.pureComputed(function () {
      return 'mb-pill mb-pill--' + fmt.toneFor(self.status());
    });
    self.kycClass = ko.pureComputed(function () {
      return 'mb-pill mb-pill--' + fmt.toneFor(self.kycStatus());
    });

    self.cifLabel = ko.pureComputed(function () {
      var value = cifNo || '—';
      return String(value).indexOf('CIF') === 0 ? value : 'CIF ' + value;
    });

    self.profile = ko.observableArray([]);
    self.completenessPct = ko.observable(null);
    self.completenessRows = ko.observableArray([]);
    self.hasCompleteness = ko.pureComputed(function () {
      return self.completenessPct() !== null;
    });

    self.summaryRows = ko.observableArray([]);
    self.accounts = ko.observableArray([]);
    self.accountsLoaded = ko.observable(false);
    self.accountsEmpty = ko.pureComputed(function () {
      return self.accountsLoaded() && self.accounts().length === 0;
    });

    self.tabs = tellerView
      ? [{ id: 'accounts', label: 'Accounts' }]
      : [
          { id: 'profile', label: 'Profile' },
          { id: 'accounts', label: 'Accounts' },
          { id: 'kyc', label: 'KYC & risk' }
        ];

    self.isTab = function (id) {
      return self.tab() === id;
    };

    self.selectTab = function (id) {
      self.tab(id);
      if (id === 'accounts' && !self.accountsLoaded()) {
        loadAccounts();
      }
    };

    self.tabClass = function (id) {
      return 'mb-nav__item mb-tab' + (self.tab() === id ? ' is-active' : '');
    };

    self.openAccount = function (accountId) {
      navigation.openAccount(accountId);
    };

    function guard(promise) {
      return promise.catch(function (err) {
        if (!err || !err.isSessionExpired) {
          self.error(http.messageFor(err));
        }
      });
    }

    function loadAccounts() {
      self.accountsLoaded(false);
      return guard(
        endpoints.accounts.search({ cifNo: cifNo, page: 0, size: 50 }).then(function (envelope) {
          var items = (envelope && envelope.items) || [];
          self.accounts(
            items.map(function (a) {
              return {
                accountId: a.accountId,
                accountName: a.accountName,
                meta: [a.maskedAccountNumber, a.productCode].filter(Boolean).join(' · '),
                balance: fmt.money(a.availableBalance, a.currency),
                status: a.status,
                statusClass: 'mb-pill mb-pill--' + fmt.toneFor(a.status)
              };
            })
          );
        })
      ).then(function () {
        self.accountsLoaded(true);
      });
    }

    if (!cifNo) {
      self.error('No customer selected.');
      self.loading(false);
      return;
    }

    var loadCustomer = endpoints.customers.get(cifNo).then(function (person) {
      self.name(fmt.fullName(person.firstName, person.lastName));
      self.status(person.status);
      self.kycStatus(person.kycStatus);
      self.riskLabel(fmt.humanize(person.riskClassification));
      self.kycFailures(String(person.kycFailureCount || 0));

      self.profile([
        { key: 'Name', value: fmt.fullName(person.firstName, person.lastName), mono: false },
        { key: 'Date of birth', value: fmt.dateOnly(person.dob), mono: false },
        { key: 'Gender', value: fmt.humanize(person.gender), mono: false },
        { key: 'Mobile', value: person.mobile || '—', mono: false },
        { key: 'Email', value: person.email || '—', mono: false },
        { key: 'PAN', value: person.panNo || '—', mono: true },
        {
          key: 'Relationship manager',
          value: person.relationshipManagerEmpId ? 'Emp ' + person.relationshipManagerEmpId : 'Unassigned',
          mono: false
        },
        { key: 'Preferred channel', value: fmt.humanize(person.preferredCommunicationChannel), mono: false }
      ]);
    });

    var loadCompleteness = tellerView ? Promise.resolve() : endpoints.customers.completeness(cifNo).then(function (map) {
      var entries = Object.keys(map || {}).map(function (key) {
        return { key: fmt.labelize(key), value: String(map[key]) };
      });
      self.completenessRows(entries);

      // The response's other numeric fields are counts. Prefer its explicit
      // percentage; only derive it when talking to an older service version.
      var percentage = map && map.percentage;
      if (typeof percentage === 'number') {
        self.completenessPct(percentage);
      } else if (map && typeof map.completedFields === 'number' && typeof map.totalFields === 'number' && map.totalFields > 0) {
        self.completenessPct(Math.round((map.completedFields / map.totalFields) * 100));
      } else {
        self.completenessPct(null);
      }
    });

    var loadSummary = tellerView ? Promise.resolve() : endpoints.customers.summary(cifNo).then(function (map) {
      self.summaryRows(
        Object.keys(map || {}).map(function (key) {
          var value = map[key];
          return {
            key: fmt.labelize(key),
            value:
              value === null || value === undefined
                ? '—'
                : typeof value === 'object'
                  ? JSON.stringify(value)
                  : String(value)
          };
        })
      );
    });

    var initialAccounts = tellerView ? loadAccounts() : Promise.resolve();
    Promise.all([guard(loadCustomer), guard(loadCompleteness), guard(loadSummary), initialAccounts]).then(function () {
      self.loading(false);
    });
  }

  return CustomerDetailViewModel;
});
