package com.moneybags.transaction.service;

import com.moneybags.transaction.domain.TransactionStatus;
import com.moneybags.transaction.entity.Transaction;
import com.moneybags.transaction.exception.DomainException;
import com.moneybags.transaction.repository.StatusHistoryRepository;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionStateMachineTest {
    private final StatusHistoryRepository history=mock(StatusHistoryRepository.class);
    private final TransactionStateMachine machine=new TransactionStateMachine(history);
    @Test void permitsAuditedTransition(){Transaction tx=Transaction.builder().status(TransactionStatus.RECEIVED).correlationId("c").build();machine.transition(tx,TransactionStatus.VALIDATED,"u","API","ok");assertThat(tx.getStatus()).isEqualTo(TransactionStatus.VALIDATED);verify(history).save(argThat(h->h.getFromStatus()==TransactionStatus.RECEIVED&&h.getToStatus()==TransactionStatus.VALIDATED));}
    @Test void rejectsArbitraryMutation(){Transaction tx=Transaction.builder().status(TransactionStatus.RECEIVED).correlationId("c").build();assertThatThrownBy(()->machine.transition(tx,TransactionStatus.COMPLETED,"u","API",null)).isInstanceOf(DomainException.class).hasMessageContaining("Cannot transition");}
}
