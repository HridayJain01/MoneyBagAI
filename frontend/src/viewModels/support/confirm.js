/**
 * Confirm-dialog state, as a mixin.
 *
 *   Confirm.call(self, { dialogId: 'tellerConfirmDialog' });
 *   ...
 *   self.openConfirm({ account: acct, amount: 5000 });
 *   self.runConfirm(function (payload, intent) {
 *     return endpoints.transactions.deposit(body, intent.idempotencyKey, intent.correlationId);
 *   }).then(...).catch(...);
 *
 * This exists for correctness, not for line count.
 *
 * Every write in transaction-service declares @RequestHeader("Idempotency-Key")
 * without required = false, so the header is mandatory. The rule that matters is
 * subtler than that: the key must be minted when the user COMMITS to the action
 * and then held across retries. Mint a fresh key per attempt and the protection
 * is gone — a retry after a timeout posts the money twice.
 *
 * That is invisible discipline. Written out by hand on every screen it will be
 * got right on the first three and wrong on the fourth, and the failure mode is
 * a duplicated financial transaction. runConfirm() encodes it once: the dialog
 * and its intent survive a failure so the same key is reused, and are torn down
 * only on success.
 *
 * Each screen keeps its OWN <oj-dialog> with a unique id rather than sharing
 * one. Legacy oj-dialog is addressed by document.getElementById, and during an
 * oj-module transition the outgoing view can still be in the DOM — a shared id
 * would resolve to whichever copy the browser found first.
 */
define(['knockout', '../../services/http'], function (ko, http) {
  'use strict';

  function Confirm(options) {
    var self = this;
    var dialogId = options && options.dialogId;

    /** { payload, intent } or null. `intent` is { idempotencyKey, correlationId }. */
    self.pending = ko.observable(null);
    self.busy = ko.observable(false);

    function dialogElement() {
      return dialogId ? document.getElementById(dialogId) : null;
    }

    /**
     * The user has committed. Mint the intent HERE — once — and open the dialog.
     */
    self.openConfirm = function (payload) {
      self.pending({ payload: payload, intent: http.beginIntent() });
      var element = dialogElement();
      if (element) {
        element.open();
      }
    };

    self.closeConfirm = function () {
      var element = dialogElement();
      if (element) {
        element.close();
      }
      self.pending(null);
    };

    /** Bound to the dialog's cancel button; refuses while a call is in flight. */
    self.cancelConfirm = function () {
      if (!self.busy()) {
        self.closeConfirm();
      }
    };

    self.confirmPayload = ko.pureComputed(function () {
      var current = self.pending();
      return current ? current.payload : null;
    });

    /**
     * Run the committed action.
     *
     * `action(payload, intent)` must return a promise. On success the dialog
     * closes and the result is passed through. On failure the dialog stays open
     * and `pending` is untouched, so the retry reuses the same idempotency key —
     * which is the whole point of this module. The rejection is re-thrown so the
     * caller can render it.
     */
    self.runConfirm = function (action) {
      var current = self.pending();
      if (!current || self.busy()) {
        return Promise.resolve(null);
      }

      self.busy(true);
      return Promise.resolve()
        .then(function () {
          return action(current.payload, current.intent);
        })
        .then(
          function (result) {
            self.busy(false);
            self.closeConfirm();
            return result;
          },
          function (error) {
            // Deliberately leaves the dialog open and the intent intact.
            self.busy(false);
            throw error;
          }
        );
    };
  }

  return Confirm;
});
