package com.moneybags.account.repository;

import com.moneybags.account.entity.AccountLimits;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountLimitsRepository extends JpaRepository<AccountLimits, String> {
}
