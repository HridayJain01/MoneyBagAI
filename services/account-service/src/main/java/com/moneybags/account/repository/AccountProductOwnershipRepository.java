package com.moneybags.account.repository;

import com.moneybags.account.entity.AccountProductOwnership;
import com.moneybags.account.entity.ProductAcquisitionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountProductOwnershipRepository
        extends JpaRepository<AccountProductOwnership, String> {

    List<AccountProductOwnership> findByOwnerAccountIdOrderByAcquiredOnDescCreatedAtDesc(
            String ownerAccountId);

    boolean existsByOwnerAccountIdAndAcquisitionType(
            String ownerAccountId, ProductAcquisitionType acquisitionType);

    List<AccountProductOwnership> findAllByOwnerAccountIdAndAcquisitionType(
            String ownerAccountId, ProductAcquisitionType acquisitionType);

    Optional<AccountProductOwnership> findByPurchaseTransactionId(String purchaseTransactionId);
}
