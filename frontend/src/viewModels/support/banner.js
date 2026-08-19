/**
 * Banner state, as a mixin.
 *
 *   define([... , './support/banner'], function (..., Banner) {
 *     function MyViewModel() {
 *       var self = this;
 *       Banner.call(self);
 *       ...
 *       self.notify('success', 'Approved', reference + ' was approved.');
 *     }
 *   }
 *
 * Five screens had each grown their own copy of this — approvals.js had the
 * full banner/hasBanner/bannerClass/dismiss set, while login, accountDetail,
 * customerDetail and genericList carried thinner error/hasError pairs. There is
 * no per-screen variation in any of them, so this is pure deletion rather than
 * an abstraction anyone has to learn.
 *
 * Tones map to .mb-banner--{tone} in layout.css: error, success, info, warning.
 */
define(['knockout', '../../services/http'], function (ko, http) {
  'use strict';

  function Banner() {
    var self = this;

    /** { tone, title, detail } or null. */
    self.banner = ko.observable(null);

    self.hasBanner = ko.pureComputed(function () {
      return !!self.banner();
    });

    self.bannerClass = ko.pureComputed(function () {
      var current = self.banner();
      return current ? 'mb-banner mb-banner--' + current.tone : 'mb-banner';
    });

    self.dismissBanner = function () {
      self.banner(null);
    };

    self.notify = function (tone, title, detail) {
      self.banner({ tone: tone, title: title, detail: detail || '' });
    };

    /**
     * Render a rejected promise.
     *
     * An expired session is swallowed on purpose: http.js has already torn down
     * the session and told the shell to route to login, so a red banner on a
     * screen that is about to be replaced is noise the user never asked for.
     */
    self.failed = function (title, error) {
      if (error && error.isSessionExpired) {
        return;
      }
      self.notify('error', title, http.messageFor(error));
    };
  }

  return Banner;
});
