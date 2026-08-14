package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.TransactionLeg;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TransactionLegRepository extends JpaRepository<TransactionLeg,String>{ List<TransactionLeg> findByTransactionIdOrderBySequenceNo(String transactionId); }
