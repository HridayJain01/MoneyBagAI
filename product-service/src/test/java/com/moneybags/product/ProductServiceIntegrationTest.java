package com.moneybags.product;

import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;
import com.moneybags.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {
    @Autowired
    private ProductService productService;

    @Test
    void createsAndReadsProduct() {
        ProductRequest request = new ProductRequest("CUR-001", "Current", ProductType.CURRENT,
                "Business current account", BigDecimal.ZERO, new BigDecimal("5000.00"),
                new BigDecimal("100000.00"), 10, ProductStatus.ACTIVE);

        productService.create(request);
        ProductResponse result = productService.findByCode("CUR-001");

        assertThat(result.productName()).isEqualTo("Current");
        assertThat(result.status()).isEqualTo(ProductStatus.ACTIVE);
    }
}
