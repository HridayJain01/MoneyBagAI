package com.moneybags.account.repository;

import com.moneybags.account.entity.FundsHold;
import com.moneybags.account.entity.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FundsHoldRepository extends JpaRepository<FundsHold, String> {

    Optional<FundsHold> findByTransactionId(String transactionId);

    List<FundsHold> findByAccountIdAndStatus(String accountId, HoldStatus status);

    /** Ground truth for held_amount; the reconciler compares this against the column. */
    @Query("SELECT COALESCE(SUM(h.amount), 0) FROM FundsHold h "
            + "WHERE h.accountId = :accountId AND h.status = :status")
    BigDecimal sumByAccountAndStatus(@Param("accountId") String accountId,
                                     @Param("status") HoldStatus status);
}
