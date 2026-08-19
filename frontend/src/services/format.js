/**
 * Display formatting. Presentation only — no arithmetic on money happens here.
 */
define([], function () {
  'use strict';

  var LOCALE = 'en-IN';
  var amountFormatters = {};

  function amountFormatter(currency) {
    if (!amountFormatters[currency]) {
      amountFormatters[currency] = new Intl.NumberFormat(LOCALE, {
        style: 'currency',
        currency: currency,
        currencyDisplay: 'narrowSymbol',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      });
    }
    return amountFormatters[currency];
  }

  /** "₹56,751.23". Falls back to a plain grouped number for unknown codes. */
  function money(value, currency) {
    var code = currency || 'INR';
    if (value === null || value === undefined || isNaN(value)) {
      return '—';
    }
    try {
      return amountFormatter(code).format(value);
    } catch (e) {
      return code + ' ' + Number(value).toFixed(2);
    }
  }

  /** Amount without the currency symbol, for hero figures that show it separately. */
  function amount(value) {
    if (value === null || value === undefined || isNaN(value)) {
      return '—';
    }
    return new Intl.NumberFormat(LOCALE, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
  }

  /** Compact form for dense stat tiles: 1.2L, 3.4Cr. */
  function compactAmount(value) {
    if (value === null || value === undefined || isNaN(value)) {
      return '—';
    }
    var abs = Math.abs(value);
    if (abs >= 10000000) {
      return (value / 10000000).toFixed(2) + 'Cr';
    }
    if (abs >= 100000) {
      return (value / 100000).toFixed(2) + 'L';
    }
    if (abs >= 1000) {
      return (value / 1000).toFixed(1) + 'K';
    }
    return amount(value);
  }

  function dateTime(iso) {
    if (!iso) {
      return '—';
    }
    var d = new Date(iso);
    if (isNaN(d.getTime())) {
      return '—';
    }
    return d.toLocaleString(LOCALE, {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  function dateOnly(iso) {
    if (!iso) {
      return '—';
    }
    var d = new Date(iso);
    if (isNaN(d.getTime())) {
      return '—';
    }
    return d.toLocaleDateString(LOCALE, { day: '2-digit', month: 'short', year: 'numeric' });
  }

  function sameDay(a, b) {
    return (
      a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
    );
  }

  /** "Today, 5:32 PM" for recent rows; a full date beyond yesterday. */
  function relativeDateTime(iso) {
    if (!iso) {
      return '—';
    }
    var d = new Date(iso);
    if (isNaN(d.getTime())) {
      return '—';
    }
    var time = d.toLocaleTimeString(LOCALE, { hour: '2-digit', minute: '2-digit' });
    var today = new Date();
    if (sameDay(d, today)) {
      return 'Today, ' + time;
    }
    var yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);
    if (sameDay(d, yesterday)) {
      return 'Yesterday, ' + time;
    }
    return dateOnly(iso) + ', ' + time;
  }

  /** ISO instant for the start of today, for the transaction `from` filter. */
  function startOfTodayIso() {
    var d = new Date();
    d.setHours(0, 0, 0, 0);
    return d.toISOString();
  }

  function endOfTodayIso() {
    var d = new Date();
    d.setHours(23, 59, 59, 999);
    return d.toISOString();
  }

  /** "INTERNAL_TRANSFER" -> "Internal transfer". */
  function humanize(value) {
    if (!value) {
      return '—';
    }
    var lower = String(value).replace(/_/g, ' ').toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  }

  /**
   * Field name -> column label. Unlike humanize, this splits camelCase, so
   * "productCode" reads "Product code" rather than "Productcode".
   */
  function labelize(key) {
    if (!key) {
      return '';
    }
    var spaced = String(key)
      .replace(/[_-]+/g, ' ')
      // camelCase and consecutive-capital boundaries (e.g. "cifNo", "panNo")
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
      .trim();
    var lower = spaced.toLowerCase();
    return lower.charAt(0).toUpperCase() + lower.slice(1);
  }

  function fullName(first, last) {
    return [first, last].filter(Boolean).join(' ') || '—';
  }

  /**
   * Which side of the ledger a transaction sits on, from the perspective of the
   * account being viewed. Without an account context we fall back to the type.
   */
  function direction(tx, accountId) {
    if (accountId) {
      if (tx.destinationAccountId === accountId) {
        return 'credit';
      }
      if (tx.sourceAccountId === accountId) {
        return 'debit';
      }
    }
    return tx.type === 'DEPOSIT' ? 'credit' : 'debit';
  }

  /** Maps backend status enums onto a pill tone. Unknown values stay neutral. */
  function toneFor(status) {
    switch (String(status || '').toUpperCase()) {
      case 'ACTIVE':
      case 'APPROVED':
      case 'SETTLED':
      case 'COMPLETED':
      case 'VERIFIED':
      case 'POSTED':
      case 'SENT':
        return 'success';
      case 'PENDING':
      case 'PENDING_APPROVAL':
      case 'DORMANT':
        return 'warning';
      case 'REJECTED':
      case 'FAILED':
      case 'BLOCKED':
      case 'CLOSED':
        return 'danger';
      case 'FROZEN':
      case 'CANCELLED':
      case 'REVERSED':
      case 'SUPPRESSED':
        return 'info';
      default:
        return 'neutral';
    }
  }

  return {
    money: money,
    amount: amount,
    compactAmount: compactAmount,
    dateTime: dateTime,
    dateOnly: dateOnly,
    relativeDateTime: relativeDateTime,
    startOfTodayIso: startOfTodayIso,
    endOfTodayIso: endOfTodayIso,
    humanize: humanize,
    labelize: labelize,
    fullName: fullName,
    direction: direction,
    toneFor: toneFor
  };
});
