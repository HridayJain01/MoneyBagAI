package com.moneybags.transaction.repository;

import com.moneybags.transaction.entity.ProductPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductPurchaseRepository extends JpaRepository<ProductPurchase, String> {
    Optional<ProductPurchase> findByTransaction_Id(String transactionId);
}
