/**
 * RESTDataProvider factories for oj-table.
 *
 * The backend speaks three different list shapes, and a provider that assumes
 * the wrong one renders a silently empty table against a 200 response:
 *
 *   spring    transaction-service        { content, totalElements, last, ... }
 *   envelope  account/audit/identity/... { items, page, size, totalItems, totalPages }
 *   list      customer search, mini-statement — a bare array, no paging at all
 *
 * Both paged flavours take `page` and `size` query params, so only the response
 * parsing differs.
 */
define(['ojs/ojrestdataprovider', 'ojs/ojdataprovider', './http'], function (
  restModule,
  ojdataprovider,
  api
) {
  'use strict';

  // The AMD build exports a namespace object, not the class itself — requiring
  // the module and calling `new` on it directly fails with "not a constructor".
  var RESTDataProvider = restModule.RESTDataProvider;

  function parse(shape, body) {
    if (shape === 'list') {
      var rows = Array.isArray(body) ? body : [];
      return { data: rows, totalSize: rows.length, hasMore: false };
    }
    if (shape === 'spring') {
      var content = (body && body.content) || [];
      return {
        data: content,
        totalSize: body && body.totalElements !== undefined ? body.totalElements : content.length,
        hasMore: !!body && body.last === false
      };
    }
    var items = (body && body.items) || [];
    var page = (body && body.page) || 0;
    var totalPages = body && body.totalPages !== undefined ? body.totalPages : 1;
    return {
      data: items,
      totalSize: body && body.totalItems !== undefined ? body.totalItems : items.length,
      hasMore: page + 1 < totalPages
    };
  }

  /**
   * Note we deliberately do not implement `fetchByOffset`. RESTDataProvider
   * delegates it to `fetchFirst` when its capability is undefined, which is
   * what oj-table's loadMoreOnScroll paging needs.
   *
   * options: { url, idKey, shape, query(), pageSize, sort }
   */
  function pagedProvider(options) {
    var size = options.pageSize || 25;

    return new RESTDataProvider({
      keyAttributes: options.idKey,
      url: options.url,
      transforms: {
        fetchFirst: {
          request: function (opts) {
            var offset = (opts.fetchParameters && opts.fetchParameters.offset) || 0;
            var query = {};
            var extra = options.query ? options.query() : {};
            Object.keys(extra).forEach(function (k) {
              query[k] = extra[k];
            });
            // Both paging styles are page-based; oj-table thinks in offsets.
            query.page = Math.floor(offset / size);
            query.size = size;
            if (options.sort) {
              query.sort = options.sort;
            }
            return new Request(api.buildUrl(options.url, query), {
              headers: api.authHeaders(),
              credentials: 'same-origin'
            });
          },
          response: function (result) {
            return parse(options.shape, result.body);
          }
        }
      }
    });
  }

  /**
   * Makes a bound oj-table refetch from offset 0.
   *
   * RESTDataProvider has no refresh() method — the documented way to invalidate
   * it is to dispatch a refresh event, which consumers listen for. Use this
   * after a write, or when filters change.
   */
  function refreshProvider(provider) {
    provider.dispatchEvent(new ojdataprovider.DataProviderRefreshEvent());
  }

  return { pagedProvider: pagedProvider, refreshProvider: refreshProvider };
});
