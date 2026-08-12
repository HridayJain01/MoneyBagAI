package com.moneybags.ledger.repository;

import com.moneybags.ledger.entity.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByCode(String code);
    List<LedgerAccount> findAllByOrderByCodeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LedgerAccount a where a.code in :codes order by a.code")
    List<LedgerAccount> lockAllByCodeIn(@Param("codes") Collection<String> codes);
}
