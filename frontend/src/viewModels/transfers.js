/**
 * Internal and external transfers. Only internal destinations are resolved
 * against account-service; NEFT/RTGS/IMPS/UPI destinations are deliberately
 * free text because the backend validates only the source side for those rails.
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

  function TransfersViewModel() {
    var self = this;
    var pollController = null;

    Banner.call(self);
    Confirm.call(self, { dialogId: 'transfersConfirmDialog' });

    self.operations = txn.railsFor('transfer');
    self.selectedId = ko.observable('INTERNAL');
    self.sourceNumber = ko.observable('');
    self.sourceAccount = ko.observable(null);
    self.destinationNumber = ko.observable('');
    self.destinationAccount = ko.observable(null);
    self.upiAddress = ko.observable('');
    self.amount = ko.observable('');
    self.currency = ko.observable('INR');
    self.narration = ko.observable('');
    self.resolvingSource = ko.observable(false);
    self.resolvingDestination = ko.observable(false);
    self.quote = ko.observable(null);
    self.quoting = ko.observable(false);
    self.result = ko.observable(null);
    self.polling = ko.observable(false);
    self.fieldErrors = ko.observable({});

    self.entry = ko.pureComputed(function () {
      return txn.railById(self.selectedId());
    });

    self.isInternal = ko.pureComputed(function () {
      var entry = self.entry();
      return !!entry && !entry.destinationIsExternal;
    });

    self.isUpi = ko.pureComputed(function () {
      return self.selectedId() === 'UPI';
    });

    self.destinationLabel = ko.pureComputed(function () {
      return self.isInternal() ? 'Destination account' : 'Beneficiary account number';
    });

    self.canReview = ko.pureComputed(function () {
      var entry = self.entry();
      var source = self.sourceAccount();
      var destination = self.destinationAccount();
      var value = Number(self.amount());
      var destinationReady = self.isInternal() ? !!destination : !!(self.destinationNumber() || '').trim();
      var distinct = !self.isInternal() || !source || !destination || source.accountId !== destination.accountId;
      return (
        !!entry &&
        !!source &&
        destinationReady &&
        distinct &&
        isFinite(value) &&
        value > 0 &&
        (!self.isUpi() || !!(self.upiAddress() || '').trim()) &&
        !self.quoting()
      );
    });

    self.sourceStatusClass = ko.pureComputed(function () {
      var account = self.sourceAccount();
      return 'mb-pill mb-pill--' + fmt.toneFor(account && account.status);
    });

    self.destinationStatusClass = ko.pureComputed(function () {
      var account = self.destinationAccount();
      return 'mb-pill mb-pill--' + fmt.toneFor(account && account.status);
    });

    self.quoteVisible = ko.pureComputed(function () {
      return txn.quoteHasBounds(self.quote());
    });

    self.quoteClass = ko.pureComputed(function () {
      var quote = self.quote();
      return 'mb-banner mb-banner--' + (!quote || quote.allowed ? (quote && quote.approvalRequired ? 'warning' : 'info') : 'error');
    });

    self.limitRows = ko.pureComputed(function () {
      var quote = self.quote();
      if (!quote) {
        return [];
      }
      return [
        { label: 'Minimum', value: quote.minAmount },
        { label: 'Maximum', value: quote.maxAmount },
        { label: 'Daily limit', value: quote.dailyLimit },
        { label: 'Approval from', value: quote.approvalThreshold }
      ].filter(function (row) {
        return row.value !== null && row.value !== undefined;
      }).map(function (row) {
        return { label: row.label, value: fmt.money(row.value, quote.currency) };
      });
    });

    self.resultStatusClass = ko.pureComputed(function () {
      var result = self.result();
      return 'mb-pill mb-pill--' + fmt.toneFor(result && result.status);
    });

    self.resultAmount = ko.pureComputed(function () {
      var result = self.result();
      return result ? fmt.money(result.amount, result.currency) : '—';
    });

    function resetForm() {
      self.sourceNumber('');
      self.sourceAccount(null);
      self.destinationNumber('');
      self.destinationAccount(null);
      self.upiAddress('');
      self.amount('');
      self.narration('');
      self.quote(null);
      self.fieldErrors({});
      self.dismissBanner();
    }

    self.selectedId.subscribe(resetForm);

    function resolve(number, target, busy, failureTitle) {
      var value = (number() || '').trim();
      if (!value || busy()) {
        if (!value) {
          self.notify('error', 'Account number required', 'Enter the full account number.');
        }
        return;
      }
      busy(true);
      target(null);
      self.quote(null);
      self.dismissBanner();
      endpoints.accounts
        .byNumber(value)
        .then(target)
        .catch(function (error) {
          self.failed(failureTitle, error);
        })
        .then(function () {
          busy(false);
        });
    }

    self.resolveSource = function () {
      resolve(self.sourceNumber, self.sourceAccount, self.resolvingSource, 'Source account could not be resolved');
    };

    self.resolveDestination = function () {
      resolve(
        self.destinationNumber,
        self.destinationAccount,
        self.resolvingDestination,
        'Destination account could not be resolved'
      );
    };

    function formSnapshot() {
      var entry = self.entry();
      return {
        sourceAccountId: self.sourceAccount().accountId,
        destinationAccountId: entry.destinationIsExternal
          ? (self.destinationNumber() || '').trim()
          : self.destinationAccount().accountId,
        amount: self.amount(),
        currency: self.currency(),
        paymentChannel: 'BRANCH',
        feeAmount: 0,
        upiAddress: (self.upiAddress() || '').trim(),
        narration: (self.narration() || '').trim()
      };
    }

    self.review = function () {
      if (!self.canReview()) {
        var same = self.isInternal() && self.sourceAccount() && self.destinationAccount() &&
          self.sourceAccount().accountId === self.destinationAccount().accountId;
        self.notify(
          'error',
          same ? 'Choose a different destination' : 'Complete the form',
          same ? 'An internal transfer cannot return to its source account.' : 'Resolve the source and enter every required value.'
        );
        return;
      }

      var entry = self.entry();
      var form = formSnapshot();
      self.quoting(true);
      self.quote(null);
      self.fieldErrors({});
      self.dismissBanner();
      txn
        .quote(entry, form)
        .then(function (quote) {
          self.quote(quote);
          if (quote && !quote.allowed) {
            self.notify('error', 'Transfer is outside limits', quote.reason || 'The configured limit refused this transfer.');
            return;
          }
          self.openConfirm({
            entry: entry,
            form: form,
            source: self.sourceAccount(),
            destination: self.destinationAccount(),
            destinationNumber: (self.destinationNumber() || '').trim(),
            quote: quote
          });
        })
        .catch(function (error) {
          self.failed('Could not check transfer limits', error);
        })
        .then(function () {
          self.quoting(false);
        });
    };

    self.dialogTitle = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      return payload ? 'Confirm ' + payload.entry.label : 'Confirm transfer';
    });

    self.dialogSummary = ko.pureComputed(function () {
      var payload = self.confirmPayload();
      if (!payload) {
        return '';
      }
      var destination = payload.destination ? payload.destination.accountNumber : payload.destinationNumber;
      return (
        payload.entry.label + ' of ' + fmt.money(Number(payload.form.amount), payload.form.currency) +
        ' from ' + payload.source.accountNumber + ' to ' + destination + '.'
      );
    });

    self.confirmLabel = ko.pureComputed(function () {
      return self.busy() ? 'Submitting…' : 'Submit transfer';
    });

    function beginPolling(created) {
      if (!created || !created.id) {
        self.notify('warning', 'Transfer accepted', 'The response did not include an id, so live status is unavailable.');
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
            self.notify('info', 'Still processing', 'The transfer remains in progress. Track it with the displayed reference.');
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
            sourceNumber: payload.source.accountNumber,
            destinationNumber: payload.destination ? payload.destination.accountNumber : payload.destinationNumber
          });
          self.notify('success', 'Transfer accepted', (created.reference || 'The transfer') + ' was created.');
          beginPolling(created);
        })
        .catch(function (error) {
          self.fieldErrors(http.fieldErrorsFor(error) || {});
          self.failed('Transfer could not be created', error);
        });
    };

    self.startAnother = function () {
      if (pollController) {
        pollController.abort();
        pollController = null;
      }
      self.result(null);
      self.polling(false);
      resetForm();
    };

    self.disconnected = function () {
      if (pollController) {
        pollController.abort();
      }
    };
  }

  return TransfersViewModel;
});
