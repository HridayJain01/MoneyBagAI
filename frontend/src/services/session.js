/**
 * Session store.
 *
 * The gateway issues an opaque session id (not a JWT) from
 * POST /api/v1/auth/login and resolves it per-request via identity-service.
 * We keep it in sessionStorage rather than localStorage: a bearer credential
 * that outlives the tab is the wrong default for a banking console.
 */
define([], function () {
  'use strict';

  var STORAGE_KEY = 'mb.session';
  var current = null;
  var loaded = false;
  var listeners = [];

  function read() {
    try {
      var raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      var parsed = JSON.parse(raw);
      // Defend against a hand-edited or half-written entry.
      return parsed && typeof parsed.sessionId === 'string' ? parsed : null;
    } catch (e) {
      return null;
    }
  }

  function notify() {
    listeners.forEach(function (fn) {
      fn(current);
    });
  }

  /** Reads sessionStorage once, then serves from memory. */
  function getSession() {
    if (!loaded) {
      current = read();
      loaded = true;
    }
    return current;
  }

  function setSession(session) {
    current = session;
    loaded = true;
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch (e) {
      /* Private-mode storage failures must not break sign-in. */
    }
    notify();
  }

  function clearSession() {
    current = null;
    loaded = true;
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch (e) {
      /* ignore */
    }
    notify();
  }

  function subscribe(fn) {
    listeners.push(fn);
    return function () {
      listeners = listeners.filter(function (item) {
        return item !== fn;
      });
    };
  }

  /**
   * True once the server-stated expiry has passed. A 30s skew keeps us from
   * firing a request we already know the gateway will reject.
   */
  function isExpired(session) {
    var s = session === undefined ? getSession() : session;
    if (!s || !s.expiresAt) {
      return false;
    }
    var expiry = Date.parse(s.expiresAt);
    return isFinite(expiry) && expiry - 30000 <= Date.now();
  }

  function isAuthenticated() {
    var s = getSession();
    return s !== null && !isExpired(s);
  }

  /**
   * Client-side permission checks are cosmetic — they hide affordances the user
   * cannot use. The gateway and each service remain the real authority, so
   * every caller must still handle a 403.
   */
  function hasPermission(permission) {
    var s = getSession();
    return !!(s && s.permissions && s.permissions.indexOf(permission) !== -1);
  }

  function hasAny(permissions) {
    if (!permissions || permissions.length === 0) {
      return true;
    }
    var s = getSession();
    if (!s || !s.permissions) {
      return false;
    }
    return permissions.some(function (p) {
      return s.permissions.indexOf(p) !== -1;
    });
  }

  function hasRole(role) {
    var s = getSession();
    return !!(s && s.roles && s.roles.indexOf(role) !== -1);
  }

  /** "Hriday Jain" -> "HJ", for the top-bar avatar. */
  function initials(name) {
    if (!name) {
      return '?';
    }
    var parts = String(name).trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
      return '?';
    }
    var first = parts[0].charAt(0);
    var last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
    return (first + last).toUpperCase();
  }

  return {
    getSession: getSession,
    setSession: setSession,
    clearSession: clearSession,
    subscribe: subscribe,
    isExpired: isExpired,
    isAuthenticated: isAuthenticated,
    hasPermission: hasPermission,
    hasAny: hasAny,
    hasRole: hasRole,
    initials: initials
  };
});
