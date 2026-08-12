package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.ClearingInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ClearingInstructionRepository extends JpaRepository<ClearingInstruction,String>{ Optional<ClearingInstruction> findByTransactionId(String transactionId); }
