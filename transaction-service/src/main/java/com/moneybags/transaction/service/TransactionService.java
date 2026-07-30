package com.moneybags.transaction.service;
import com.moneybags.transaction.dto.*;
import java.time.LocalDateTime;
import java.util.List;
public interface TransactionService {
    TransactionResponse post(TransactionRequest request);
    TransactionResponse findById(Long txnId);
    List<TransactionResponse> findForAccount(String accountNo, LocalDateTime from, LocalDateTime to);
}
