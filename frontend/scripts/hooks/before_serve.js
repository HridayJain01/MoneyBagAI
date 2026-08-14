/**
  Copyright (c) 2015, 2026, Oracle and/or its affiliates.
  Licensed under The Universal Permissive License (UPL), Version 1.0
  as shown at https://oss.oracle.com/licenses/upl/

*/

'use strict';

/**
 * Proxies /api to the Moneybags API gateway during `ojet serve`.
 *
 * This is not a convenience: the gateway ships no CORS configuration at all,
 * so a browser calling http://localhost:8090 directly from the dev server
 * origin is blocked outright. Everything the app requests must be same-origin
 * and relative, and this hook is what makes that true in development.
 *
 * In production the built app is served from the gateway itself, so no proxy
 * is involved.
 *
 * JET 19+ runs the dev server on Express 5, which is why http-proxy-middleware
 * must be v3 — v2 targets Express 4 and will not mount correctly.
 */

// 127.0.0.1, not localhost: Node 17+ resolves "localhost" to ::1 first, and the
// gateway binds IPv4 only — using the hostname gives intermittent ECONNREFUSED.
const GATEWAY = process.env.MB_GATEWAY || 'http://127.0.0.1:8090';

module.exports = function (configObj) {
  return new Promise((resolve) => {
    let express;
    let createProxyMiddleware;
    try {
      express = require('express');
      ({ createProxyMiddleware } = require('http-proxy-middleware'));
    } catch (err) {
      console.warn(
        '[moneybags] express/http-proxy-middleware not installed — /api will NOT be proxied.\n' +
          '            Run: npm install --save-dev express http-proxy-middleware@^3'
      );
      resolve(configObj);
      return;
    }

    const app = express();

    // Mounted at the app root, NOT at '/api'. Mounting on a path makes Express
    // strip that prefix, so the gateway would receive /v1/auth/login and 404.
    // pathFilter selects the same requests while leaving the URL intact.
    app.use(
      createProxyMiddleware({
        target: GATEWAY,
        changeOrigin: true,
        ws: false,
        // The gateway rejects /internal/** with 403 by design. Excluding it
        // here makes a stray call obvious in development rather than looking
        // like a permissions bug.
        pathFilter: (path) => path.startsWith('/api/') && !path.startsWith('/api/internal'),
        on: {
          error(err, req, res) {
            console.error(`[moneybags] proxy error for ${req.url}: ${err.message}`);
            if (res && !res.headersSent && typeof res.writeHead === 'function') {
              res.writeHead(502, { 'Content-Type': 'application/json' });
              res.end(
                JSON.stringify({
                  code: 'GATEWAY_UNREACHABLE',
                  message: `Could not reach the API gateway at ${GATEWAY}. Is it running?`,
                })
              );
            }
          },
        },
      })
    );

    // History-API fallback.
    //
    // Routing uses UrlPathParamAdapter, so /overview and /accountDetail/ACC001
    // are real paths. The dev server only knows about files, and would 404
    // them. Rewrite HTML navigations to index.html and let the client router
    // take over. Registered after the proxy (so /api is untouched) and before
    // the static middleware ojet appends.
    app.use((req, res, next) => {
      if (req.method !== 'GET' && req.method !== 'HEAD') {
        return next();
      }
      const accepts = req.headers.accept || '';
      const path = req.url.split('?')[0];
      const looksLikeFile = path.lastIndexOf('.') > path.lastIndexOf('/');

      if (accepts.includes('text/html') && !looksLikeFile && !path.startsWith('/api/')) {
        req.url = '/index.html';
      }
      return next();
    });

    console.log(`[moneybags] proxying /api -> ${GATEWAY}`);
    console.log('[moneybags] history fallback enabled for client-side routes');
    configObj.express = app;
    resolve(configObj);
  });
};
