package com.moneybags.product.service;

import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.entity.Product;
import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;
import com.moneybags.product.exception.ConflictException;
import com.moneybags.product.mapper.ProductMapper;
import com.moneybags.product.repository.ProductRepository;
import com.moneybags.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, productMapper);
    }

    @Test
    void createsProduct() {
        ProductRequest request = request();
        Product entity = Product.builder().productCode("SAV-001").build();
        ProductResponse response = response();
        when(productRepository.existsById("SAV-001")).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isEqualTo(response);
        verify(productRepository).save(entity);
    }

    @Test
    void rejectsDuplicateProductCode() {
        when(productRepository.existsById("SAV-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SAV-001");
    }

    private ProductRequest request() {
        return new ProductRequest("SAV-001", "Savings", ProductType.SAVINGS, "Retail savings",
                new BigDecimal("3.50"), new BigDecimal("1000.00"), new BigDecimal("50000.00"),
                5, ProductStatus.ACTIVE);
    }

    private ProductResponse response() {
        ProductRequest request = request();
        return new ProductResponse(request.productCode(), request.productName(), request.productType(),
                request.description(), request.interestRate(), request.minBalance(),
                request.maxWithdrawalPerDay(), request.freeTxnPerMonth(), request.status());
    }
}
