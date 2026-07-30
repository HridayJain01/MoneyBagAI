package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByRequestRef(String requestRef);
    List<Transaction> findByAccountNoAndTxnDateBetweenOrderByTxnDateDesc(
            String accountNo, LocalDateTime from, LocalDateTime to);
}
