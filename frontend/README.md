# Moneybags Console

Employee-facing banking console for the Moneybags backend.
**Oracle JET 20, MVVM architecture — plain JavaScript (AMD modules) + Knockout.**
No TypeScript, no build-time compile step beyond `ojet build`.

## Who this is for

Only **employees** authenticate. `POST /api/v1/auth/login` returns an opaque
JWT access token which is sent as `Authorization: Bearer <accessToken>` on every later
request. Customers are data in this system, never callers — so screens that look
like personal banking (balance hero, recent activity, mini statement) are an
employee *servicing* a customer's account.

## Running it

The backend must be up first:

```powershell
.\run-all.ps1
```

Then, from `frontend/`:

```powershell
ojet serve
```

The app runs on <http://localhost:8000> and proxies `/api` to the gateway on
`http://127.0.0.1:8090`. Override the target with the `MB_GATEWAY` environment
variable.

Seeded logins are in `docs/SEED_FIXTURES.md` (`teller1`, `checker1`, `manager1`,
`opsadmin`). Each has a different permission set, which visibly changes the
navigation — `checker1` sees no Customers entry, `opsadmin` sees everything.

### The proxy is not optional

The gateway ships **no CORS configuration**, so a browser calling the gateway
directly from the dev-server origin is blocked outright. Every request the app
makes is relative (`/api/v1/...`), and `scripts/hooks/before_serve.js` proxies
it. Never point the client at an absolute gateway URL.

Three things in that hook are load-bearing:

- **It proxies at the app root with a `pathFilter`, not mounted on `/api`.**
  Mounting on a path makes Express strip the prefix, so the gateway would
  receive `/v1/auth/login` and 404.
- **The target is `127.0.0.1`, not `localhost`.** Node 17+ resolves `localhost`
  to `::1` first and the gateway binds IPv4 only.
- `/api/internal/**` is excluded; the gateway 403s those routes by design.

## Production

```powershell
ojet build --release
```

Copy `web/**` into `services/api-gateway/src/main/resources/static/`. Routing
state lives in the query string, so no server-side history fallback is required
and the app is same-origin with the API — no CORS work on the backend.

## Layout

```
src/
  index.html          shell markup, auth gate, and the #mbGenericList template
  index.js            bootstrap: applies Knockout bindings
  appController.js    shell chrome, session state, permission-filtered nav
  services/
    navigation.js     CoreRouter, routes, permission gate, param accessor
    session.js        session storage + permission helpers
    http.js           fetch wrapper: auth, idempotency, 401 handling
    providers.js      RESTDataProvider factories (three envelope shapes)
    endpoints.js      one function per backend endpoint
    format.js         money, dates, status tones, label casing
  viewModels/         one per screen
  views/              one .html per view model
  styles/             tokens.css, theme-overrides.css, layout.css, app.css
```

## Design

Crisp, not glassmorphic: solid white cards, hairline borders, soft low-opacity
shadows, generous whitespace. The violet/pink/blue mesh gradient is **hero art
only** — the login panel, the balance hero, the nav mark — and never sits behind
body copy or tabular data.

All values live in `styles/tokens.css`. `theme-overrides.css` binds Redwood's
`--oj-*` variables to those tokens so JET components inherit the look rather than
being overridden with `!important`. It loads after the injected theme
stylesheet, which is what lets plain CSS win.

Two Redwood specifics worth knowing, both verified at runtime:

- **The call-to-action button does not read `--oj-core-brand-1`.** It has its own
  `--oj-button-call-to-action-chrome-bg-color` (default: a near-black
  `rgb(49,45,42)`). Setting brand alone leaves every primary button dark.
- **Redwood assigns headings an explicit text colour**, so headings on the dark
  gradient panels must be told to `color: inherit`.

Chart colours must be set in JavaScript via `style-defaults` — CSS variables do
not reach chart marks.

## Things that will bite you

**Three list envelopes.** A provider assuming the wrong one renders a silently
empty table against a `200`:

| Shape | Services | JSON |
|---|---|---|
| `spring` | transaction-service | `{ content, totalElements, last }` |
| `envelope` | account, audit, identity, notification | `{ items, page, totalItems, totalPages }` |
| `list` | customer search, mini-statement, products, branches, ledger journals | bare array |

Always pass the right `shape` to `pagedProvider`.

**Permission names are not guessable.** The seeded permission is `CUSTOMER_READ`,
not `CUSTOMER_VIEW`; a wrong string hides the screen from everyone, silently.
Check `docs/SEED_FIXTURES.md` before gating anything new.

**Idempotency keys are mandatory** on every write in transaction-service — the
header is declared without `required = false`, so a missing key is a 400 before
any business logic runs. Mint the key with `http.beginCommand()` when the user
commits to the action and **reuse it across retries**; a fresh key per attempt
defeats the protection and can double-post.

**`ApplicationStatus` has no `PENDING`.** It is `DRAFT | SUBMITTED |
PENDING_APPROVAL | APPROVED | REJECTED | CANCELLED`, and an invalid value comes
back as a 500, not a 400.

**Customer search has no paging and no sorting**, and `query` is required. Don't
build a paged grid on it.

**`GET /{cif}/summary` returns `Map<String,Object>`** — its keys are unknown until
runtime. It is rendered generically for that reason.

**No card data.** There is no card service; linked cards live behind
`/internal/**`, which is blocked. Card tiles from consumer-fintech mockups have
nothing behind them here, as do FX, investments, rewards and revenue.

**Session caching.** The gateway caches session resolution for 30s, so a request
can briefly succeed after sign-out. Client state is cleared immediately rather
than relying on the server to reject.

## JET API notes

Discovered the hard way; each one fails silently or with an opaque error:

- **`ojs/ojrestdataprovider` exports a namespace, not the class.** Use
  `require('ojs/ojrestdataprovider').RESTDataProvider`, or `new` throws
  "not a constructor". `ojarraydataprovider`, `ojcorerouter` and the adapters
  *are* direct exports.
- **`RESTDataProvider` has no `refresh()`.** Dispatch a
  `DataProviderRefreshEvent` — that is what `providers.refreshProvider` does.
- **`ModuleRouterAdapter` does not forward router params** into the module's
  view-model context, and the `ojModule` *binding* does not forward its `params`
  to the view-model constructor either. Detail screens read parameters via
  `navigation.param('id')` instead, and the shared list screens inherit from a
  constructor rather than nesting an `ojModule`.
- **`UrlPathParamAdapter` needs parameters declared in the route path**, which
  then breaks plain `router.go({ path })` matching. `UrlParamAdapter`
  (query-string) handles parameters without that trade-off, which is what this
  app uses.
- Inside an `oj-table` column template, `$root` is **not** the app controller.
  With `data-oj-as="cell"` the outer view model stays available as `$data`;
  navigation goes through the `navigation` module rather than the binding
  context.
