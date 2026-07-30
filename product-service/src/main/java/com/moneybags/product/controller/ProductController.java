package com.moneybags.product.controller;

import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.dto.ProductUpdateRequest;
import com.moneybags.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping("/{productCode}")
    ProductResponse findByCode(@PathVariable String productCode) {
        return productService.findByCode(productCode);
    }

    @GetMapping
    List<ProductResponse> findAll() {
        return productService.findAll();
    }

    @PutMapping("/{productCode}")
    ProductResponse update(@PathVariable String productCode,
                           @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(productCode, request);
    }

    @DeleteMapping("/{productCode}")
    ResponseEntity<Void> delete(@PathVariable String productCode) {
        productService.delete(productCode);
        return ResponseEntity.noContent().build();
    }
}
