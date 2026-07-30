package com.moneybags.statement.dto;
import com.moneybags.statement.client.TransactionClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record StatementResponse(String accountNo, String cifNo, BigDecimal currentBalance,
                                LocalDateTime from, LocalDateTime to,
                                List<TransactionClient.TransactionSummary> transactions) {}
