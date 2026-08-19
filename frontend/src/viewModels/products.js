/** Product catalogue plus permission-gated product lifecycle administration. */
define([
  'knockout',
  '../services/endpoints',
  '../services/format',
  '../services/http',
  '../services/session',
  './support/banner',
  './support/confirm',
  'ojs/ojdialog'
], function (ko, endpoints, fmt, http, session, Banner, Confirm) {
  'use strict';

  function ProductsViewModel() {
    var self = this;

    Banner.call(self);
    Confirm.call(self, { dialogId: 'productAdminDialog' });

    self.loading = ko.observable(true);
    self.error = ko.observable('');
    self.products = ko.observableArray([]);
    self.selectedProduct = ko.observable(null);
    self.versions = ko.observableArray([]);
    self.versionsLoading = ko.observable(false);
    self.versionError = ko.observable('');
    self.status = ko.observable('');
    self.productType = ko.observable('');
    self.statuses = ['', 'ACTIVE', 'INACTIVE'];
    self.productTypes = ['', 'SAVINGS', 'CURRENT', 'TERM_DEPOSIT', 'RECURRING_DEPOSIT'];
    self.manageTypes = ['SAVINGS', 'CURRENT', 'TERM_DEPOSIT', 'RECURRING_DEPOSIT'];
    self.canManage = session.hasPermission('PRODUCT_MANAGE');

    self.formCode = ko.observable('');
    self.formName = ko.observable('');
    self.formType = ko.observable('SAVINGS');
    self.formDescription = ko.observable('');
    self.formCurrency = ko.observable('INR');
    self.formRate = ko.observable('0');
    self.formMinBalance = ko.observable('0');
    self.formOpeningMinimum = ko.observable('0');
    self.formWithdrawalLimit = ko.observable('0');
    self.formFreeTransactions = ko.observable('0');
    self.formTenure = ko.observable('');
    self.formAllowsOverdraft = ko.observable(false);
    self.formRequiresFunding = ko.observable(false);
    self.formMinAge = ko.observable('18');
    self.formEffectiveFrom = ko.observable(new Date().toISOString().slice(0, 10));
    self.formEffectiveTo = ko.observable('');
    self.formError = ko.observable('');

    self.fundingCount = ko.pureComputed(function () {
      return self.products().filter(function (row) { return row.requiresFunding; }).length;
    });
    self.activeCount = ko.pureComputed(function () {
      return self.products().filter(function (row) { return row.status === 'ACTIVE'; }).length;
    });
    self.dialogAction = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return payload ? payload.action : '';
    });
    self.isProductForm = ko.pureComputed(function () {
      return self.dialogAction() === 'create' || self.dialogAction() === 'edit';
    });
    self.isCreate = ko.pureComputed(function () { return self.dialogAction() === 'create'; });
    self.dialogTitle = ko.pureComputed(function () {
      var action = self.dialogAction();
      return action === 'create' ? 'Create product' : action === 'edit' ? 'Edit product terms' : action === 'activate' ? 'Activate product' : 'Deactivate product';
    });
    self.dialogSummary = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload || self.isProductForm()) { return ''; }
      return (payload.action === 'activate' ? 'Activate ' : 'Stop new sales for ') + payload.row.productCode + ' — ' + payload.row.productName + '? Existing accounts are not changed.';
    });
    self.confirmLabel = ko.pureComputed(function () {
      if (self.busy()) { return 'Working…'; }
      var action = self.dialogAction();
      return action === 'create' ? 'Create product' : action === 'edit' ? 'Save new version' : action === 'activate' ? 'Activate' : 'Deactivate';
    });

    function decorateVersion(row) {
      row.recordedDisplay = fmt.dateTime(row.recordedAt);
      row.rateDisplay = Number(row.interestRate || 0).toFixed(2) + '%';
      row.effectiveDisplay = fmt.dateOnly(row.effectiveFrom) + (row.effectiveTo ? ' – ' + fmt.dateOnly(row.effectiveTo) : ' onward');
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.status);
      return row;
    }

    function loadVersions(productCode) {
      self.versionsLoading(true);
      self.versionError('');
      return endpoints.products.versions(productCode).then(function (rows) {
        self.versions((rows || []).map(decorateVersion));
      }).catch(function (err) {
        if (!err || !err.isSessionExpired) { self.versionError(http.messageFor(err)); }
      }).then(function () { self.versionsLoading(false); });
    }

    function decorate(row) {
      row.typeDisplay = fmt.humanize(row.productType);
      row.rateDisplay = Number(row.interestRate || 0).toFixed(2) + '%';
      row.minimumDisplay = fmt.money(row.minOpeningDeposit, row.currency);
      row.balanceDisplay = fmt.money(row.minBalance, row.currency);
      row.withdrawalDisplay = fmt.money(row.maxWithdrawalPerDay, row.currency);
      row.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(row.status);
      row.fundingDisplay = row.requiresFunding ? 'Required' : 'Optional';
      row.tenureDisplay = row.tenureMonths ? row.tenureMonths + ' months' : 'No fixed tenure';
      row.effectiveDisplay = fmt.dateOnly(row.effectiveFrom) + (row.effectiveTo ? ' – ' + fmt.dateOnly(row.effectiveTo) : ' onward');
      row.charges = (row.charges || []).map(function (charge) {
        charge.typeDisplay = fmt.humanize(charge.chargeType);
        charge.amountDisplay = fmt.money(charge.amount, row.currency);
        charge.frequencyDisplay = fmt.humanize(charge.frequency);
        return charge;
      });
      row.rules = (row.rules || []).map(function (rule) {
        rule.keyDisplay = fmt.humanize(rule.ruleKey);
        rule.statusClass = 'mb-pill mb-pill--' + fmt.toneFor(rule.active ? 'ACTIVE' : 'CLOSED');
        return rule;
      });
      row.inspect = function () {
        self.selectedProduct(row);
        loadVersions(row.productCode);
      };
      row.closeDetail = function () {
        self.selectedProduct(null);
        self.versions([]);
      };
      row.edit = function () { self.beginEdit(row); };
      row.changeStatus = function () { self.beginStatus(row); };
      return row;
    }

    self.load = function () {
      self.loading(true);
      self.error('');
      return endpoints.products.list({ status: self.status() || undefined, productType: self.productType() || undefined })
        .then(function (rows) {
          self.products((rows || []).map(decorate));
          var selected = self.selectedProduct();
          if (selected) {
            var refreshed = self.products().find(function (row) { return row.productCode === selected.productCode; }) || null;
            self.selectedProduct(refreshed);
            if (refreshed) { loadVersions(refreshed.productCode); }
          }
        })
        .catch(function (err) {
          if (!err || !err.isSessionExpired) { self.error(http.messageFor(err)); }
        })
        .then(function () { self.loading(false); });
    };

    self.applyFilters = self.load;
    self.clearFilters = function () {
      self.status(''); self.productType(''); self.selectedProduct(null); self.versions([]); self.load();
    };

    function resetForm(row) {
      var item = row || {};
      self.formCode(item.productCode || '');
      self.formName(item.productName || '');
      self.formType(item.productType || 'SAVINGS');
      self.formDescription(item.description || '');
      self.formCurrency(item.currency || 'INR');
      self.formRate(String(item.interestRate === undefined ? 0 : item.interestRate));
      self.formMinBalance(String(item.minBalance === undefined ? 0 : item.minBalance));
      self.formOpeningMinimum(String(item.minOpeningDeposit === undefined ? 0 : item.minOpeningDeposit));
      self.formWithdrawalLimit(String(item.maxWithdrawalPerDay === undefined ? 0 : item.maxWithdrawalPerDay));
      self.formFreeTransactions(String(item.freeTxnPerMonth === undefined ? 0 : item.freeTxnPerMonth));
      self.formTenure(item.tenureMonths ? String(item.tenureMonths) : '');
      self.formAllowsOverdraft(!!item.allowsOverdraft);
      self.formRequiresFunding(!!item.requiresFunding);
      self.formMinAge(String(item.minAge === undefined ? 18 : item.minAge));
      self.formEffectiveFrom(item.effectiveFrom || new Date().toISOString().slice(0, 10));
      self.formEffectiveTo(item.effectiveTo || '');
      self.formError('');
    }

    self.beginCreate = function () { resetForm(null); self.openConfirm({ action: 'create' }); };
    self.beginEdit = function (row) { resetForm(row); self.openConfirm({ action: 'edit', row: row }); };
    self.beginStatus = function (row) { self.formError(''); self.openConfirm({ action: row.status === 'ACTIVE' ? 'deactivate' : 'activate', row: row }); };

    function numberValue(observable, label, optional) {
      var raw = String(observable() || '').trim();
      if (optional && !raw) { return null; }
      var value = Number(raw);
      if (!isFinite(value) || value < 0) { throw new Error(label + ' must be zero or greater.'); }
      return value;
    }

    function formBody(action) {
      var name = (self.formName() || '').trim();
      if (!name) { throw new Error('Product name is required.'); }
      var body = {
        productName: name,
        description: (self.formDescription() || '').trim() || null,
        interestRate: numberValue(self.formRate, 'Interest rate'),
        minBalance: numberValue(self.formMinBalance, 'Minimum balance'),
        minOpeningDeposit: numberValue(self.formOpeningMinimum, 'Opening minimum'),
        maxWithdrawalPerDay: numberValue(self.formWithdrawalLimit, 'Withdrawal limit'),
        freeTxnPerMonth: numberValue(self.formFreeTransactions, 'Free transactions'),
        minAge: numberValue(self.formMinAge, 'Minimum age'),
        effectiveFrom: self.formEffectiveFrom() || null
      };
      if (action === 'edit') {
        body.effectiveTo = self.formEffectiveTo() || null;
        return body;
      }
      var code = (self.formCode() || '').trim().toUpperCase();
      if (!/^[A-Z0-9-]{2,30}$/.test(code)) { throw new Error('Product code must use 2–30 uppercase letters, numbers or hyphens.'); }
      body.productCode = code;
      body.productType = self.formType();
      body.currency = (self.formCurrency() || '').trim().toUpperCase();
      body.tenureMonths = numberValue(self.formTenure, 'Tenure', true);
      body.allowsOverdraft = !!self.formAllowsOverdraft();
      body.requiresFunding = !!self.formRequiresFunding();
      return body;
    }

    self.confirm = function () {
      var payload = self.confirmPayload();
      if (!payload) { return; }
      var body = null;
      try {
        if (payload.action === 'create' || payload.action === 'edit') { body = formBody(payload.action); }
      } catch (validationError) {
        self.formError(validationError.message);
        return;
      }
      self.formError('');
      self.runConfirm(function (committed, intent) {
        if (committed.action === 'create') { return endpoints.products.create(body, intent.idempotencyKey); }
        if (committed.action === 'edit') { return endpoints.products.update(committed.row.productCode, body, intent.idempotencyKey); }
        return committed.action === 'activate' ? endpoints.products.activate(committed.row.productCode, intent.idempotencyKey) : endpoints.products.deactivate(committed.row.productCode, intent.idempotencyKey);
      }).then(function (result) {
        if (result === null) { return; }
        self.notify('success', 'Product updated', result.productCode + ' is now ' + result.status + '.');
        return self.load();
      }).catch(function (err) { self.failed('Product update failed', err); });
    };

    self.load();
  }

  return ProductsViewModel;
});
