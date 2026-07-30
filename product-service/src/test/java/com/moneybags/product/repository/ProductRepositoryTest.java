package com.moneybags.product.repository;

import com.moneybags.product.entity.Product;
import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    void persistsAndFindsProduct() {
        Product product = Product.builder()
                .productCode("SAV-001")
                .productName("Savings")
                .productType(ProductType.SAVINGS)
                .description("Retail savings")
                .interestRate(new BigDecimal("3.5000"))
                .minBalance(new BigDecimal("1000.00"))
                .maxWithdrawalPerDay(new BigDecimal("50000.00"))
                .freeTxnPerMonth(5)
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.saveAndFlush(product);

        assertThat(productRepository.findById("SAV-001"))
                .isPresent()
                .get()
                .extracting(Product::getProductName)
                .isEqualTo("Savings");
    }
}
