package com.moneybags.customer.repository;
import com.moneybags.customer.entity.BeneficiaryChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BeneficiaryChangeHistoryRepository extends JpaRepository<BeneficiaryChangeHistory, Long> {
    List<BeneficiaryChangeHistory> findByBeneficiaryIdOrderByChangedAtDesc(Long beneficiaryId);
}
