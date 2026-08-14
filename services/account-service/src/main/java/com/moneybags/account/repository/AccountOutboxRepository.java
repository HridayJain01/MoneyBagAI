package com.moneybags.account.repository;

import com.moneybags.account.entity.AccountOutbox;
import com.moneybags.account.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AccountOutboxRepository extends JpaRepository<AccountOutbox, String> {

    @Query("SELECT o FROM AccountOutbox o WHERE o.status = :status AND o.nextAttemptAt <= :now "
            + "ORDER BY o.createdAt ASC")
    List<AccountOutbox> findDeliverable(@Param("status") OutboxStatus status,
                                        @Param("now") Instant now,
                                        Pageable pageable);
}
