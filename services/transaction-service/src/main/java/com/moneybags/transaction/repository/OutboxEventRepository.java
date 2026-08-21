package com.moneybags.transaction.repository;
import com.moneybags.transaction.domain.FinancialEnums.OutboxStatus;
import com.moneybags.transaction.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.List;
public interface OutboxEventRepository extends JpaRepository<OutboxEvent,String>{
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @QueryHints(@QueryHint(name="jakarta.persistence.lock.timeout",value="-2"))
 @Query("select e from OutboxEvent e where e.status=:status and (e.nextAttemptAt is null or e.nextAttemptAt<=:now) order by e.createdAt")
 List<OutboxEvent> findDeliverable(OutboxStatus status, Instant now, Pageable pageable);
 boolean existsByAggregateIdAndEventType(String aggregateId,String eventType);
 boolean existsByDeduplicationKey(String deduplicationKey);
 List<OutboxEvent> findByAggregateId(String aggregateId);
}
