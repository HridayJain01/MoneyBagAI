/**
 * Products — a thin configuration of the shared generic list screen.
 * See viewModels/genericList.js for how columns are derived from the response.
 */
define(['./genericList', '../services/endpoints'], function (GenericList, endpoints) {
  'use strict';

  function ProductsViewModel() {
    GenericList.call(this, {
      title: 'Products',
      lede: 'Deposit and lending products offered by the bank.',
      load: function () {
        return endpoints.products.list();
      }
    });
  }

  ProductsViewModel.prototype = Object.create(GenericList.prototype);
  ProductsViewModel.prototype.constructor = ProductsViewModel;

  return ProductsViewModel;
});
