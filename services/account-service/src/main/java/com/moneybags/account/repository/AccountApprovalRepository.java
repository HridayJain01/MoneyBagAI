package com.moneybags.account.repository;

import com.moneybags.account.entity.AccountApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountApprovalRepository extends JpaRepository<AccountApproval, String> {
    List<AccountApproval> findByApplicationIdOrderByDecidedAtDesc(String applicationId);
}
