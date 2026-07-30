package com.moneybags.product.service;

import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.dto.ProductUpdateRequest;

import java.util.List;

public interface ProductService {
    ProductResponse create(ProductRequest request);

    ProductResponse findByCode(String productCode);

    List<ProductResponse> findAll();

    ProductResponse update(String productCode, ProductUpdateRequest request);

    void delete(String productCode);
}
