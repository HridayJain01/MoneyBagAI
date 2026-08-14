package com.moneybags.transaction.repository;
import com.moneybags.transaction.domain.FinancialEnums.ReconciliationStatus;
import com.moneybags.transaction.entity.ReconciliationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationException,String>{
 Page<ReconciliationException> findByStatus(ReconciliationStatus status,Pageable pageable);
 boolean existsByTypeAndTransactionIdAndStatus(String type,String transactionId,ReconciliationStatus status);
}
