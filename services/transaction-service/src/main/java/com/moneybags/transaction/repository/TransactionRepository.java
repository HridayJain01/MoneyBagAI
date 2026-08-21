package com.moneybags.transaction.repository;

import com.moneybags.transaction.domain.TransactionStatus;
import com.moneybags.transaction.entity.Transaction;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.QueryHint;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;

public interface TransactionRepository extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByReference(String reference);
    boolean existsByReversalOfId(String transactionId);
    Page<Transaction> findBySourceAccountIdOrDestinationAccountId(String source, String destination, Pageable pageable);
    List<Transaction> findByStatusIn(Collection<TransactionStatus> statuses);
    Page<Transaction> findByStatusIn(Collection<TransactionStatus> statuses, Pageable pageable);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name="jakarta.persistence.lock.timeout", value="-2"))
    @Query("select t from Transaction t where t.status in :statuses order by t.updatedAt")
    List<Transaction> findProjectionBackfillCandidates(Collection<TransactionStatus> statuses, Pageable pageable);
    @Query("select coalesce(sum(t.amount+t.feeAmount),0) from Transaction t where (t.sourceAccountId=:accountId or t.destinationAccountId=:accountId) and t.createdAt>=:from and t.createdAt<:to and t.status not in (com.moneybags.transaction.domain.TransactionStatus.FAILED,com.moneybags.transaction.domain.TransactionStatus.REJECTED,com.moneybags.transaction.domain.TransactionStatus.CANCELLED)")
    BigDecimal sumDailyUsage(String accountId, Instant from, Instant to);
}
