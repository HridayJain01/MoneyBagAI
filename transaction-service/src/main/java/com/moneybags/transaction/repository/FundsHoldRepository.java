package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.FundsHold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FundsHoldRepository extends JpaRepository<FundsHold,String>{ Optional<FundsHold> findByTransactionId(String transactionId); }
