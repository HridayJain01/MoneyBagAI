package com.moneybags.statement;

import com.moneybags.statement.ApiModels.RequestStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

interface AccountReadRepository extends JpaRepository<AccountReadModel,String> {
    Page<AccountReadModel> findByStatusIgnoreCase(String status,Pageable page);
}
interface TransactionReadRepository extends JpaRepository<TransactionReadModel,TransactionReadId> {
    List<TransactionReadModel> findTop100ByAccountIdOrderByPostedAtDesc(String accountId);
    List<TransactionReadModel> findByAccountIdAndPostedAtGreaterThanEqualAndPostedAtLessThanAndPostedAtLessThanEqualOrderByPostedAtAsc(String accountId,Instant from,Instant to,Instant snapshot);
    List<TransactionReadModel> findByPostedAtGreaterThanEqualAndPostedAtLessThanOrderByPostedAtAsc(Instant from,Instant to);
    List<TransactionReadModel> findByBranchIdAndPostedAtGreaterThanEqualAndPostedAtLessThanOrderByPostedAtAsc(String branch,Instant from,Instant to);
    List<TransactionReadModel> findByAccountIdAndPostedAtGreaterThanEqualAndPostedAtLessThanOrderByPostedAtAsc(String accountId,Instant from,Instant to);
    long countByPostedAtGreaterThanEqualAndPostedAtLessThanAndLedgerEntryIdIsNull(Instant from,Instant to);
    long countByPostedAtGreaterThanEqualAndPostedAtLessThanAndReversalOfTransactionIdIsNotNull(Instant from,Instant to);
}
interface ConsumedEventRepository extends JpaRepository<ConsumedSourceEvent,String> {}
interface StatementRequestRepository extends JpaRepository<StatementRequestEntity,String> {
    Optional<StatementRequestEntity> findByRequestRef(String requestRef);
    Page<StatementRequestEntity> findByRequestedByUserId(String userId,Pageable page);
    Page<StatementRequestEntity> findByRequesterBranchId(String branchId,Pageable page);
    Optional<StatementRequestEntity> findFirstByStatusOrderByCreatedAtAsc(RequestStatus status);
}
interface GeneratedFileRepository extends JpaRepository<GeneratedFileEntity,String> { Optional<GeneratedFileEntity> findByRequestId(String requestId); }
interface DownloadHistoryRepository extends JpaRepository<DownloadHistoryEntity,String> { Page<DownloadHistoryEntity> findByDownloadedByUserId(String userId,Pageable page); }
interface ReportScheduleRepository extends JpaRepository<ReportScheduleEntity,String> {
    Page<ReportScheduleEntity> findByOwnerUserId(String owner,Pageable page);
    List<ReportScheduleEntity> findTop25ByActiveTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant now);
}
