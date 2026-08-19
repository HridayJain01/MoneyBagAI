/**
 * Branch-counter money movement: cash deposit, cash withdrawal and cheque
 * deposit. Account entry deliberately resolves a typed account number rather
 * than loading an unpageable account picker.
 */
define([
  'knockout',
  '../services/endpoints',
  '../services/txn',
  '../services/format',
  '../services/http',
  './support/banner',
  './support/confirm',
  'ojs/ojbutton',
  'ojs/ojdialog',
  'ojs/ojinputtext'
], function (ko, endpoints, txn, fmt, http, Banner, Confirm) {
  'use strict';

  function TellerOpsViewModel() {
    var self = this;
    var pollController = null;

    Banner.call(self);
    Confirm.call(self, { dialogId: 'tellerConfirmDialog' });

    self.operations = txn.railsFor('teller');
    self.selectedId = ko.observable('DEPOSIT');
    self.accountNumber = ko.observable('');
    self.account = ko.observable(null);
    self.resolving = ko.observable(false);
    self.amount = ko.observable('');
    self.currency = ko.observable('INR');
    self.chequeNumber = ko.observable('');
    self.narration = ko.observable('');
    self.quote = ko.observable(null);
    self.quoting = ko.observable(false);
    self.result = ko.observable(null);
    self.polling = ko.observable(false);
    self.fieldErrors = ko.observable({});

    self.entry = ko.pureComputed(function () {
      return txn.railById(self.selectedId());
    });

    self.isCheque = ko.pureComputed(function () {
      var entry = self.entry();
      return !!entry && entry.needs.chequeNumber;
    });

    self.accountRole = ko.pureComputed(function () {
      var entry = self.entry();
      return entry && entry.transactionType === 'WITHDRAWAL' ? 'Source account' : 'Destination account';
    });

    self.canReview = ko.pureComputed(function () {
      var value = Number(self.amount());
      return (
        !!self.account() &&
        isFinite(value) &&
        value > 0 &&
        (!self.isCheque() || !!(self.chequeNumber() || '').trim()) &&
        !self.quoting()
      );
    });

    self.accountStatusClass = ko.pureComputed(function () {
      var account = self.account();
      return 'mb-pill mb-pill--' + fmt.toneFor(account && account.status);
    });

    self.quoteVisible = ko.pureComputed(function () {
      return txn.quoteHasBounds(self.quote());
    });

    self.quoteClass = ko.pureComputed(function () {
      var quote = self.quote();
      if (!quote) {
        return 'mb-banner';
      }
      return 'mb-banner mb-banner--' + (!quote.allowed ? 'error' : quote.approvalRequired ? 'warning' : 'info');
    });

    self.resultStatusClass = ko.pureComputed(function () {
      var result = self.result();
      return 'mb-pill mb-pill--' + fmt.toneFor(result && result.status);
    });

    self.formAmount = ko.pureComputed(function () {
      return fmt.money(Number(self.amount()), self.currency());
    });

    self.selectedId.subscribe(function () {
      self.accountNumber('');
      self.account(null);
      self.amount('');
      self.chequeNumber('');
      self.narration('');
      self.quote(null);
      self.fieldErrors({});
      self.dismissBanner();
    });

    self.resolveAccount = function () {
      var number = (self.accountNumber() || '').trim();
      if (!number || self.resolving()) {
        if (!number) {
          self.notify('error', 'Account number required', 'Enter the account number from the counter slip.');
        }
        return;
      }

      self.resolving(true);
      self.account(null);
      self.quote(null);
      self.dismissBanner();
      endpoints.accounts
        .byNumber(number)
        .then(function (account) {
          self.account(account);
        })
        .catch(function (error) {
          self.failed('Account could not be resolved', error);
        })
        .then(function () {
          self.resolving(false);
        });
    };

    function formSnapshot() {
      var entry = self.entry();
      var account = self.account();
      return {
        sourceAccountId: entry.needs.source ? account.accountId : null,
        destinationAccountId: entry.needs.destination ? account.accountId : null,
        amount: self.amount(),
        currency: self.currency(),
        paymentChannel: 'BRANCH',
        feeAmount: 0,
        chequeNumber: (self.chequeNumber() || '').trim(),
        narration: (self.narration() || '').trim()
      };
    }

    self.review = function () {
      if (!self.canReview()) {
        self.notify('error', 'Complete the form', 'Resolve an account and enter every required value.');
        return;
      }

      var entry = self.entry();
      var form = formSnapshot();
      self.quoting(true);
      self.fieldErrors({});
      self.dismissBanner();

      txn
        .quote(entry, form)
        .then(function (quote) {
          self.quote(quote);
          if (quote && !quote.allowed) {
            self.notify('error', 'Transaction is outside limits', quote.reason || 'The configured limit refused this transaction.');
            return;
          }
          self.openConfirm({ entry: entry, form: form, account: self.account(), quote: quote });
        })
        .catch(function (error) {
          self.failed('Could not check transaction limits', error);
        })
        .then(function () {
          self.quoting(false);
        });
    };

    self.dialogTitle = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return payload ? 'Confirm ' + payload.entry.label.toLowerCase() : 'Confirm transaction';
    });

    self.dialogSummary = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return '';
      }
      return (
        payload.entry.label + ' of ' + fmt.money(Number(payload.form.amount), payload.form.currency) +
        ' for account ' + payload.account.accountNumber + '.'
      );
    });

    self.confirmLabel = ko.pureComputed(function () {
      return self.busy() ? 'Submitting…' : 'Submit transaction';
    });

    function beginPolling(created) {
      if (!created || !created.id) {
        self.notify('warning', 'Transaction accepted', 'The response did not include an id, so live status is unavailable.');
        return;
      }

      pollController = typeof AbortController === 'function' ? new AbortController() : null;
      self.polling(true);
      txn
        .pollStatus(created.id, {
          signal: pollController && pollController.signal,
          onStatus: function (view) {
            self.result(Object.assign({}, self.result(), {
              status: view.status,
              reference: view.transactionReference || self.result().reference,
              updatedAt: view.updatedAt
            }));
          }
        })
        .then(function (outcome) {
          self.polling(false);
          if (outcome.exhausted) {
            self.notify('info', 'Still processing', 'The transaction remains in progress. Its reference is safe to use in Transactions.');
          }
        })
        .catch(function (error) {
          self.polling(false);
          if (!error || error.name !== 'AbortError') {
            self.failed('Live status unavailable', error);
          }
        });
    }

    self.confirm = function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return;
      }

      self
        .runConfirm(function (committed, intent) {
          return committed.entry.submit(
            txn.bodyFor(committed.entry, committed.form),
            intent.idempotencyKey,
            intent.correlationId
          );
        })
        .then(function (created) {
          if (created === null) {
            return;
          }
          self.result({
            id: created.id,
            reference: created.reference,
            status: created.status,
            type: created.type,
            amount: Number(payload.form.amount),
            currency: payload.form.currency,
            accountNumber: payload.account.accountNumber,
            createdAt: created.createdAt
          });
          self.notify('success', 'Transaction accepted', (created.reference || 'The transaction') + ' was created.');
          beginPolling(created);
        })
        .catch(function (error) {
          self.fieldErrors(http.fieldErrorsFor(error) || {});
          self.failed('Transaction could not be created', error);
        });
    };

    self.startAnother = function () {
      if (pollController) {
        pollController.abort();
        pollController = null;
      }
      self.result(null);
      self.polling(false);
      self.accountNumber('');
      self.account(null);
      self.amount('');
      self.chequeNumber('');
      self.narration('');
      self.quote(null);
      self.fieldErrors({});
      self.dismissBanner();
    };

    self.disconnected = function () {
      if (pollController) {
        pollController.abort();
      }
    };
  }

  return TellerOpsViewModel;
});
