package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.TransactionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StatusHistoryRepository extends JpaRepository<TransactionStatusHistory,String>{ List<TransactionStatusHistory> findByTransactionIdOrderByOccurredAt(String transactionId); }
