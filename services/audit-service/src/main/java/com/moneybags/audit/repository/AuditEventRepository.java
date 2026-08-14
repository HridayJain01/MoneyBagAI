package com.moneybags.audit.repository;

import com.moneybags.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    /** The cross-service request trace: the whole point of this service without a broker. */
    List<AuditEvent> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    List<AuditEvent> findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
            String aggregateType, String aggregateId);

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE (:sourceService IS NULL OR e.sourceService = :sourceService)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:aggregateType IS NULL OR e.aggregateType = :aggregateType)
              AND (:aggregateId IS NULL OR e.aggregateId = :aggregateId)
              AND (:actorEmployeeId IS NULL OR e.actorEmployeeId = :actorEmployeeId)
              AND (:branchCode IS NULL OR e.branchCode = :branchCode)
              AND (:from IS NULL OR e.occurredAt >= :from)
              AND (:to IS NULL OR e.occurredAt < :to)
            ORDER BY e.occurredAt DESC
            """)
    Page<AuditEvent> search(@Param("sourceService") String sourceService,
                            @Param("eventType") String eventType,
                            @Param("aggregateType") String aggregateType,
                            @Param("aggregateId") String aggregateId,
                            @Param("actorEmployeeId") String actorEmployeeId,
                            @Param("branchCode") String branchCode,
                            @Param("from") Instant from,
                            @Param("to") Instant to,
                            Pageable pageable);
}
