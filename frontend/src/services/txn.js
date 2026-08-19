/**
 * Money movement: the rail catalogue, request assembly, limit quoting and
 * status polling.
 *
 * Everything the transaction endpoints get wrong when called naively lives here
 * once, so the screens stay declarative. Four rules are encoded, all of them
 * verified against TransactionOrchestrator rather than the documentation:
 *
 * 1. paymentMethod is DERIVED, never chosen. validateShape() requires
 *    method.name() == rail.name(), with exactly two exceptions: rail INTERNAL
 *    takes method ACCOUNT, rail CASH takes method CASH. Anything else is a 400
 *    INVALID_PAYMENT_METHOD. Since each endpoint hard-codes its rail, offering
 *    the user a payment-method picker would only let them break the request.
 *
 * 2. The validated account FLIPS. validateAccounts() resolves the DESTINATION
 *    for DEPOSIT and CHEQUE, and the SOURCE for everything else. Only
 *    INTERNAL_TRANSFER validates both. For NEFT/RTGS/IMPS/UPI the destination is
 *    an unvalidated free-form string — the money is leaving this bank, so there
 *    is nothing here to resolve it against.
 *
 * 3. The server quotes amount PLUS fee, against the account from rule 2. Quoting
 *    the bare amount, or against the other side, diverges silently: the quote
 *    says yes and the create says no.
 *
 * 4. Only ONE limit rule is seeded (RTGS/INR, min 200000, approval >= 1000000).
 *    Every other rail answers allowed:true with all four bounds null. A bounds
 *    panel is therefore worth rendering only when something in it is non-null.
 *
 * CARD_PAYMENT and PRODUCT_PURCHASE are deliberately absent from RAILS.
 * CARD_PAYMENT requires a cardId whose context is served only from
 * /internal/v1/cards/{cardId}/payment-context, which the gateway blocks — a
 * teller has no way to obtain one. PRODUCT_PURCHASE takes a different request
 * shape entirely and belongs with account opening, not with a rail selector.
 */
define(['./endpoints'], function (endpoints) {
  'use strict';

  /** Statuses that accept no further action. */
  var TERMINAL = ['COMPLETED', 'FAILED', 'REJECTED', 'CANCELLED', 'REVERSED'];

  function isTerminal(status) {
    return TERMINAL.indexOf(status) !== -1;
  }

  /**
   * PENDING_APPROVAL is non-terminal but STABLE: a checker has to act, which
   * could be hours away. Polling it is pointless and looks broken.
   */
  function awaitsHuman(status) {
    return status === 'PENDING_APPROVAL';
  }

  /**
   * One entry per operation this console can actually build.
   *
   * `needs` drives which fields a form renders and which it sends.
   * `destinationIsExternal` means the destination leaves this bank and must NOT
   * be resolved against account-service — a naive lookup dead-ends on a 404.
   */
  var RAILS = [
    {
      id: 'DEPOSIT',
      label: 'Cash deposit',
      group: 'teller',
      transactionType: 'DEPOSIT',
      rail: 'CASH',
      paymentMethod: 'CASH',
      needs: { source: false, destination: true, upiAddress: false, chequeNumber: false },
      destinationIsExternal: false,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.deposit(body, key, correlationId);
      }
    },
    {
      id: 'WITHDRAWAL',
      label: 'Cash withdrawal',
      group: 'teller',
      transactionType: 'WITHDRAWAL',
      rail: 'CASH',
      paymentMethod: 'CASH',
      needs: { source: true, destination: false, upiAddress: false, chequeNumber: false },
      destinationIsExternal: false,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.withdrawal(body, key, correlationId);
      }
    },
    {
      id: 'CHEQUE',
      label: 'Cheque deposit',
      group: 'teller',
      transactionType: 'CHEQUE',
      rail: 'CHEQUE',
      paymentMethod: 'CHEQUE',
      // Like a deposit, the money is arriving: the DESTINATION is validated.
      needs: { source: false, destination: true, upiAddress: false, chequeNumber: true },
      destinationIsExternal: false,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.cheque(body, key, correlationId);
      }
    },
    {
      id: 'INTERNAL',
      label: 'Internal transfer',
      group: 'transfer',
      transactionType: 'INTERNAL_TRANSFER',
      rail: 'INTERNAL',
      paymentMethod: 'ACCOUNT',
      needs: { source: true, destination: true, upiAddress: false, chequeNumber: false },
      // The only transfer whose destination is a real account here.
      destinationIsExternal: false,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.internalTransfer(body, key, correlationId);
      }
    },
    {
      id: 'NEFT',
      label: 'NEFT',
      group: 'transfer',
      transactionType: 'NEFT',
      rail: 'NEFT',
      paymentMethod: 'NEFT',
      needs: { source: true, destination: true, upiAddress: false, chequeNumber: false },
      destinationIsExternal: true,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.neft(body, key, correlationId);
      }
    },
    {
      id: 'RTGS',
      label: 'RTGS',
      group: 'transfer',
      transactionType: 'RTGS',
      rail: 'RTGS',
      paymentMethod: 'RTGS',
      needs: { source: true, destination: true, upiAddress: false, chequeNumber: false },
      destinationIsExternal: true,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.rtgs(body, key, correlationId);
      }
    },
    {
      id: 'IMPS',
      label: 'IMPS',
      group: 'transfer',
      transactionType: 'IMPS',
      rail: 'IMPS',
      paymentMethod: 'IMPS',
      needs: { source: true, destination: true, upiAddress: false, chequeNumber: false },
      destinationIsExternal: true,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.imps(body, key, correlationId);
      }
    },
    {
      id: 'UPI',
      label: 'UPI',
      group: 'transfer',
      transactionType: 'UPI',
      rail: 'UPI',
      paymentMethod: 'UPI',
      needs: { source: true, destination: true, upiAddress: true, chequeNumber: false },
      destinationIsExternal: true,
      submit: function (body, key, correlationId) {
        return endpoints.transactions.upi(body, key, correlationId);
      }
    }
  ];

  function railsFor(group) {
    return RAILS.filter(function (entry) {
      return entry.group === group;
    });
  }

  function railById(id) {
    var found = RAILS.filter(function (entry) {
      return entry.id === id;
    });
    return found.length ? found[0] : null;
  }

  /**
   * The account the SERVER quotes and validates against — see rule 2 above.
   * Deposits and cheques are quoted on the destination; everything else on the
   * source.
   */
  function quoteAccountFor(entry, form) {
    var inbound = entry.transactionType === 'DEPOSIT' || entry.transactionType === 'CHEQUE';
    return inbound ? form.destinationAccountId : form.sourceAccountId;
  }

  /**
   * Pre-flight limit check. Resolves null when there is no account to quote
   * against yet, so a caller can treat "not ready" and "no limits" alike.
   */
  function quote(entry, form) {
    var accountId = quoteAccountFor(entry, form);
    if (!accountId || !form.amount) {
      return Promise.resolve(null);
    }
    return endpoints.transactions.limitQuote({
      accountId: accountId,
      transactionType: entry.transactionType,
      rail: entry.rail,
      channel: 'BRANCH',
      currency: form.currency || 'INR',
      // Rule 3: the server validates amount + fee, so the quote must too.
      amount: Number(form.amount) + Number(form.feeAmount || 0)
    });
  }

  /** True when a quote carries anything worth putting on screen. */
  function quoteHasBounds(view) {
    if (!view) {
      return false;
    }
    return ['minAmount', 'maxAmount', 'dailyLimit', 'approvalThreshold'].some(function (key) {
      return view[key] !== null && view[key] !== undefined;
    });
  }

  /**
   * Assemble a CreateRequest.
   *
   * Only the fields this rail needs are sent. paymentMethod comes from the
   * catalogue, never from the form. feeAmount is intentionally not exposed by
   * any screen: there is no fee schedule anywhere in this backend, and a fee the
   * user can type is a fee the quote and the create can disagree about.
   */
  function bodyFor(entry, form) {
    var body = {
      amount: Number(form.amount),
      currency: form.currency || 'INR',
      paymentChannel: form.paymentChannel || 'BRANCH',
      paymentMethod: entry.paymentMethod
    };

    if (entry.needs.source) {
      body.sourceAccountId = form.sourceAccountId;
    }
    if (entry.needs.destination) {
      body.destinationAccountId = form.destinationAccountId;
    }
    if (entry.needs.upiAddress) {
      body.upiAddress = form.upiAddress;
    }
    if (entry.needs.chequeNumber) {
      body.chequeNumber = form.chequeNumber;
    }
    if (form.narration) {
      body.narration = form.narration;
    }
    return body;
  }

  /**
   * Backoff schedule for status polling: seven requests over roughly 29 seconds.
   *
   * Not a fixed tick. Cash and internal transfers almost always terminate on the
   * first poll, so a 1s tick would spend thirty requests learning nothing, while
   * an externally-cleared rail can sit in PROCESSING for far longer than any
   * polling budget — which is what the exhausted result is for.
   */
  var POLL_BACKOFF_MS = [1000, 2000, 3000, 5000, 5000, 5000, 8000];

  /**
   * Poll /status until terminal or the budget runs out.
   *
   * Resolves { status, view, exhausted, aborted }. Never rejects on abort — a
   * teardown is not an error. Pass an AbortSignal and abort it from the view
   * model's disconnected() hook: left running, the timer keeps firing after
   * navigation and produces a burst of SessionExpiredError once the user signs
   * out.
   */
  function pollStatus(transactionId, options) {
    var opts = options || {};
    var onStatus = opts.onStatus || function () {};
    var signal = opts.signal;

    return new Promise(function (resolve, reject) {
      var step = 0;
      var timer = null;
      var stopped = false;

      function stop() {
        stopped = true;
        if (timer) {
          window.clearTimeout(timer);
          timer = null;
        }
      }

      if (signal) {
        if (signal.aborted) {
          resolve({ aborted: true, exhausted: false });
          return;
        }
        signal.addEventListener('abort', function () {
          if (!stopped) {
            stop();
            resolve({ aborted: true, exhausted: false });
          }
        });
      }

      function attempt() {
        if (stopped) {
          return;
        }
        endpoints.transactions
          .status(transactionId)
          .then(function (view) {
            if (stopped) {
              return;
            }
            onStatus(view);
            if (isTerminal(view.status) || awaitsHuman(view.status)) {
              stop();
              resolve({ status: view.status, view: view, exhausted: false, aborted: false });
              return;
            }
            schedule();
          })
          .catch(function (error) {
            if (!stopped) {
              stop();
              reject(error);
            }
          });
      }

      function schedule() {
        if (step >= POLL_BACKOFF_MS.length) {
          stop();
          resolve({ exhausted: true, aborted: false });
          return;
        }
        timer = window.setTimeout(attempt, POLL_BACKOFF_MS[step]);
        step += 1;
      }

      schedule();
    });
  }

  return {
    RAILS: RAILS,
    TERMINAL: TERMINAL,
    railsFor: railsFor,
    railById: railById,
    isTerminal: isTerminal,
    awaitsHuman: awaitsHuman,
    quoteAccountFor: quoteAccountFor,
    quote: quote,
    quoteHasBounds: quoteHasBounds,
    bodyFor: bodyFor,
    pollStatus: pollStatus
  };
});
