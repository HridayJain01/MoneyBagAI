/**
 * The one HTTP entry point for the console.
 *
 * Requests are always relative ("/api/v1/..."). In development `ojet serve`
 * proxies /api to the gateway (see scripts/hooks/before_serve.js); in
 * production the app is served from the gateway itself. Either way the browser
 * makes a same-origin call, which matters because the gateway ships no CORS
 * configuration at all.
 */
define(['./session'], function (session) {
  'use strict';

  /** Errors the UI can branch on without string-matching. */
  function ApiError(status, code, message, body) {
    var error = new Error(message);
    error.name = 'ApiError';
    error.status = status;
    error.code = code;
    error.body = body;
    error.isApiError = true;
    error.isForbidden = status === 403;
    error.isConflict = status === 409;
    error.isBusinessRule = status === 422;
    error.isServerFault = status >= 500;
    return error;
  }

  /**
   * Thrown after the session has been torn down and a redirect to login is
   * already in flight. Screens should swallow this rather than render an error.
   */
  function SessionExpiredError() {
    var error = new Error('Session expired');
    error.name = 'SessionExpiredError';
    error.isSessionExpired = true;
    return error;
  }

  var authListeners = [];

  /** Fires when the gateway rejects our session, so the shell can route to login. */
  function onAuthExpired(fn) {
    authListeners.push(fn);
    return function () {
      authListeners = authListeners.filter(function (item) {
        return item !== fn;
      });
    };
  }

  function uuid() {
    var c = window.crypto;
    if (c && typeof c.randomUUID === 'function') {
      return c.randomUUID();
    }
    // Fallback for non-secure contexts, where randomUUID is unavailable.
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (ch) {
      var r = (Math.random() * 16) | 0;
      var v = ch === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  /**
   * An idempotency key scoped to one user intent.
   *
   * Every write in transaction-service declares @RequestHeader("Idempotency-Key")
   * without required = false, so the header is mandatory. Generate the key when
   * the user commits to the action (e.g. when a confirm dialog opens) and hold
   * it across retries — minting a fresh key per attempt defeats the protection
   * and can double-post on a flaky network.
   */
  function beginCommand() {
    return uuid();
  }

  function buildUrl(path, query) {
    if (!query) {
      return path;
    }
    var params = new URLSearchParams();
    Object.keys(query).forEach(function (key) {
      var value = query[key];
      if (value !== undefined && value !== null && value !== '') {
        params.append(key, String(value));
      }
    });
    var qs = params.toString();
    return qs ? path + '?' + qs : path;
  }

  /** Headers every authenticated call carries. Also used by the data providers. */
  function authHeaders(extra) {
    var headers = { Accept: 'application/json', 'X-Correlation-Id': uuid() };
    if (extra) {
      Object.keys(extra).forEach(function (k) {
        headers[k] = extra[k];
      });
    }
    var s = session.getSession();
    if (s) {
      headers.Authorization = 'Bearer ' + s.sessionId;
    }
    return headers;
  }

  function extractCode(body) {
    if (body && typeof body === 'object') {
      var code = body.code || body.errorCode;
      if (typeof code === 'string') {
        return code;
      }
    }
    return null;
  }

  function extractMessage(body, status) {
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    if (body && typeof body === 'object') {
      var keys = ['message', 'detail', 'error', 'reason'];
      for (var i = 0; i < keys.length; i++) {
        var value = body[keys[i]];
        if (typeof value === 'string' && value.trim()) {
          return value;
        }
      }
    }
    return 'Request failed with status ' + status;
  }

  function parseBody(response) {
    if (response.status === 204) {
      return Promise.resolve(null);
    }
    return response.text().then(function (text) {
      if (!text) {
        return null;
      }
      var type = response.headers.get('content-type') || '';
      if (type.indexOf('json') !== -1) {
        try {
          return JSON.parse(text);
        } catch (e) {
          return text;
        }
      }
      return text;
    });
  }

  function request(method, path, options) {
    var opts = options || {};
    var isWrite = method !== 'GET' && method !== 'HEAD';
    var headers = authHeaders(opts.headers);

    if (opts.body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }
    if (isWrite) {
      // Mint one only if the caller did not supply an intent-scoped key.
      // Callers that can be retried should always pass their own.
      headers['Idempotency-Key'] = opts.idempotencyKey || beginCommand();
    }

    return fetch(buildUrl(path, opts.query), {
      method: method,
      headers: headers,
      signal: opts.signal,
      body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
      credentials: 'same-origin'
    })
      .catch(function (cause) {
        if (cause && cause.name === 'AbortError') {
          throw cause;
        }
        throw ApiError(0, 'NETWORK_ERROR', 'Could not reach the server. Check your connection.', cause);
      })
      .then(function (response) {
        return parseBody(response).then(function (body) {
          if (response.ok) {
            return body;
          }

          // SESSION_REQUIRED / SESSION_INVALID come from the gateway's
          // SessionAuthenticationGlobalFilter. Either way the credential is dead.
          if (response.status === 401) {
            session.clearSession();
            authListeners.forEach(function (fn) {
              fn();
            });
            throw SessionExpiredError();
          }

          throw ApiError(response.status, extractCode(body), extractMessage(body, response.status), body);
        });
      });
  }

  /** Turns any thrown value into something safe to show a user. */
  function messageFor(error) {
    if (!error) {
      return 'Something went wrong.';
    }
    if (error.isSessionExpired) {
      return 'Your session has expired. Please sign in again.';
    }
    if (error.isApiError) {
      if (error.isForbidden) {
        // A 403 is not always "you lack a permission". Business rules such as
        // MAKER_SELF_APPROVAL also come back as 403, and their server message
        // explains exactly which rule refused — that is far more useful than a
        // generic line. Only PERMISSION_DENIED gets the friendly rewrite.
        if (error.code && error.code !== 'PERMISSION_DENIED' && error.message) {
          return error.message;
        }
        return 'You do not have permission to perform this action.';
      }
      if (error.isServerFault) {
        return 'The service is unavailable right now. Please try again.';
      }
      return error.message;
    }
    return error.message || 'Something went wrong.';
  }

  return {
    get: function (path, options) {
      return request('GET', path, options);
    },
    post: function (path, options) {
      return request('POST', path, options);
    },
    put: function (path, options) {
      return request('PUT', path, options);
    },
    patch: function (path, options) {
      return request('PATCH', path, options);
    },
    del: function (path, options) {
      return request('DELETE', path, options);
    },
    buildUrl: buildUrl,
    authHeaders: authHeaders,
    beginCommand: beginCommand,
    uuid: uuid,
    onAuthExpired: onAuthExpired,
    messageFor: messageFor
  };
});
