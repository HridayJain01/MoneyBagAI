package com.moneybags.account.repository;

import com.moneybags.account.entity.AccountHolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountHolderRepository extends JpaRepository<AccountHolder, String> {
    List<AccountHolder> findByAccountIdOrderByHolderSequence(String accountId);

    List<AccountHolder> findByCifNo(String cifNo);
}
