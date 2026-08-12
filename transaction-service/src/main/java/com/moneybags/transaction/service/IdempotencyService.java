package com.moneybags.transaction.service;

import com.moneybags.transaction.domain.FinancialEnums.IdempotencyState;
import com.moneybags.transaction.entity.*;
import com.moneybags.transaction.exception.DomainException;
import com.moneybags.transaction.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service @RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRepository repository;
    public Claim claim(String scope,String operation,String key,String hash){
        if(key==null||key.isBlank()) throw DomainException.invalid("IDEMPOTENCY_KEY_REQUIRED","Idempotency-Key header is required");
        var existing=repository.findLocked(scope,operation,key);
        if(existing.isPresent()){
            IdempotencyRecord r=existing.get(); if(!r.getRequestHash().equals(hash)) throw DomainException.conflict("IDEMPOTENCY_KEY_REUSED","Idempotency key was already used with a different request");
            if(r.getTransaction()!=null) return new Claim(r,true); throw DomainException.conflict("IDEMPOTENCY_REQUEST_IN_PROGRESS","The original request is still processing");
        }
        return new Claim(repository.saveAndFlush(IdempotencyRecord.builder().callerScope(scope).operation(operation).key(key).requestHash(hash).state(IdempotencyState.PROCESSING).build()),false);
    }
    public void complete(IdempotencyRecord record,Transaction tx,int responseCode){record.setTransaction(tx);record.setState(IdempotencyState.COMPLETED);record.setResponseCode(responseCode);record.setCompletedAt(Instant.now());repository.save(record);}
    public record Claim(IdempotencyRecord record,boolean replay){}
}
