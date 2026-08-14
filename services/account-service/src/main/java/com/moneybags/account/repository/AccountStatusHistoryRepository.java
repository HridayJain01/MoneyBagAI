package com.moneybags.account.repository;

import com.moneybags.account.entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, Long> {
    List<AccountStatusHistory> findByAccountIdOrderByChangedAtDesc(String accountId);
}
