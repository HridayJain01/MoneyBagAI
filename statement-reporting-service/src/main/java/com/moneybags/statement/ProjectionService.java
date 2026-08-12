package com.moneybags.statement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.statement.ApiModels.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service @RequiredArgsConstructor
class ProjectionService {
    private final AccountReadRepository accounts; private final TransactionReadRepository transactions; private final ConsumedEventRepository events; private final ObjectMapper mapper;

    @Transactional IngestResult account(AccountEvent e){
        String hash=hash(e); if(replay(e.sourceEventId(),hash))return new IngestResult(e.sourceEventId(),"DUPLICATE");
        AccountReadModel current=accounts.findById(e.accountId()).orElse(null);
        if(current==null||!current.sourceUpdatedAt.isAfter(e.sourceUpdatedAt()))accounts.save(AccountReadModel.builder().accountId(e.accountId()).customerId(e.customerId()).branchId(e.branchId())
                .maskedAccountNumber(e.maskedAccountNumber()).accountName(e.accountName()).status(e.status()).currency(e.currency()).currentBalance(e.currentBalance())
                .dormantSince(e.dormantSince()).sourceUpdatedAt(e.sourceUpdatedAt()).build());
        record(e.sourceEventId(),"ACCOUNT",hash);return new IngestResult(e.sourceEventId(),"APPLIED");
    }
    @Transactional IngestResult transaction(TransactionEvent e){
        String hash=hash(e);if(replay(e.sourceEventId(),hash))return new IngestResult(e.sourceEventId(),"DUPLICATE");
        TransactionReadId id=new TransactionReadId(e.transactionId(),e.accountId(),e.direction());TransactionReadModel current=transactions.findById(id).orElse(null);
        if(current==null||!current.sourceUpdatedAt.isAfter(e.sourceUpdatedAt()))transactions.save(TransactionReadModel.builder().transactionId(e.transactionId()).ledgerEntryId(e.ledgerEntryId())
                .transactionReference(e.transactionReference()).accountId(e.accountId()).customerId(e.customerId()).branchId(e.branchId()).direction(e.direction())
                .amount(e.amount()).feeAmount(e.feeAmount()==null?java.math.BigDecimal.ZERO:e.feeAmount()).currency(e.currency()).transactionType(e.transactionType())
                .status(e.status()).narration(e.narration()).reversalOfTransactionId(e.reversalOfTransactionId()).postedAt(e.postedAt()).balanceAfter(e.balanceAfter()).sourceUpdatedAt(e.sourceUpdatedAt()).build());
        record(e.sourceEventId(),"TRANSACTION_LEDGER",hash);return new IngestResult(e.sourceEventId(),"APPLIED");
    }
    private boolean replay(String id,String hash){return events.findById(id).map(x->{if(!x.requestHash.equals(hash))throw ApiException.conflict("EVENT_REPLAY_CONFLICT","sourceEventId was replayed with different content");return true;}).orElse(false);}
    private void record(String id,String type,String hash){events.save(new ConsumedSourceEvent(id,type,hash,Instant.now()));}
    private String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
