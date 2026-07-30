package com.moneybags.transaction.mapper;

import com.moneybags.transaction.dto.TransactionRequest;
import com.moneybags.transaction.dto.TransactionResponse;
import com.moneybags.transaction.entity.Transaction;
import com.moneybags.transaction.enums.DebitCredit;
import com.moneybags.transaction.enums.TransactionStatus;
import com.moneybags.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T16:57:02+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public Transaction toEntity(TransactionRequest request) {
        if ( request == null ) {
            return null;
        }

        Transaction.TransactionBuilder transaction = Transaction.builder();

        transaction.requestRef( request.requestRef() );
        transaction.accountNo( request.accountNo() );
        transaction.txnType( request.txnType() );
        transaction.drCr( request.drCr() );
        transaction.amount( request.amount() );
        transaction.counterpartyAcct( request.counterpartyAcct() );
        transaction.narration( request.narration() );
        transaction.postedBy( request.postedBy() );

        return transaction.build();
    }

    @Override
    public TransactionResponse toResponse(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        Long txnId = null;
        String txnRef = null;
        String requestRef = null;
        String accountNo = null;
        TransactionType txnType = null;
        DebitCredit drCr = null;
        BigDecimal amount = null;
        BigDecimal runningBalance = null;
        String counterpartyAcct = null;
        String narration = null;
        TransactionStatus status = null;
        LocalDateTime txnDate = null;
        String postedBy = null;

        txnId = transaction.getTxnId();
        txnRef = transaction.getTxnRef();
        requestRef = transaction.getRequestRef();
        accountNo = transaction.getAccountNo();
        txnType = transaction.getTxnType();
        drCr = transaction.getDrCr();
        amount = transaction.getAmount();
        runningBalance = transaction.getRunningBalance();
        counterpartyAcct = transaction.getCounterpartyAcct();
        narration = transaction.getNarration();
        status = transaction.getStatus();
        txnDate = transaction.getTxnDate();
        postedBy = transaction.getPostedBy();

        TransactionResponse transactionResponse = new TransactionResponse( txnId, txnRef, requestRef, accountNo, txnType, drCr, amount, runningBalance, counterpartyAcct, narration, status, txnDate, postedBy );

        return transactionResponse;
    }
}
