package com.moneybags.statement.service.impl;
import com.moneybags.statement.client.*;
import com.moneybags.statement.dto.StatementResponse;
import com.moneybags.statement.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
@Service @RequiredArgsConstructor
public class StatementServiceImpl implements StatementService {
    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
    @Override
    public StatementResponse generate(String accountNo, LocalDateTime from, LocalDateTime to) {
        AccountClient.AccountSummary account = accountClient.findAccount(accountNo);
        // TODO add pagination, opening/closing balances, export formats, and Feign fallback/error decoding.
        return new StatementResponse(accountNo, account.cifNo(), account.balance(), from, to,
                transactionClient.findTransactions(accountNo, from, to));
    }
}
