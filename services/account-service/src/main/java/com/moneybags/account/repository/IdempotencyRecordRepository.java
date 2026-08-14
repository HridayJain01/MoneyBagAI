package com.moneybags.account.repository;

import com.moneybags.account.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByCallerScopeAndOperationAndIdempotencyKey(
            String callerScope, String operation, String idempotencyKey);
}
