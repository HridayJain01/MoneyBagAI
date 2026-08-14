/**
 * Application entry point.
 *
 * Waits for JET to be ready, then binds the root controller to #globalBody.
 */
define(['knockout', 'ojs/ojcontext', './appController', 'ojs/ojknockout', 'ojs/ojmodule-element', 'ojs/ojbutton'], function (
  ko,
  Context,
  controller
) {
  'use strict';

  function bind() {
    ko.applyBindings(controller, document.getElementById('globalBody'));
    Context.getPageContext().getBusyContext().applicationBootstrapComplete();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bind);
  } else {
    bind();
  }
});
