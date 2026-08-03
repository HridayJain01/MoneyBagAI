package com.moneybags.customer.repository;
import com.moneybags.customer.entity.KycRejectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface KycRejectionHistoryRepository extends JpaRepository<KycRejectionHistory, Long> { List<KycRejectionHistory> findByCifNoOrderByRejectedAtDesc(String cifNo); }
