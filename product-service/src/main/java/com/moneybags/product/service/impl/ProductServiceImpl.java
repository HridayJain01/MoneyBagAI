package com.moneybags.product.service.impl;

import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.dto.ProductUpdateRequest;
import com.moneybags.product.entity.Product;
import com.moneybags.product.exception.ConflictException;
import com.moneybags.product.exception.ResourceNotFoundException;
import com.moneybags.product.mapper.ProductMapper;
import com.moneybags.product.repository.ProductRepository;
import com.moneybags.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsById(request.productCode())) {
            throw new ConflictException("Product already exists: " + request.productCode());
        }
        return productMapper.toResponse(productRepository.save(productMapper.toEntity(request)));
    }

    @Override
    public ProductResponse findByCode(String productCode) {
        return productMapper.toResponse(getProduct(productCode));
    }

    @Override
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(productMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ProductResponse update(String productCode, ProductUpdateRequest request) {
        Product product = getProduct(productCode);
        productMapper.update(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(String productCode) {
        productRepository.delete(getProduct(productCode));
    }

    private Product getProduct(String productCode) {
        return productRepository.findById(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productCode));
    }
}
