package com.moneybags.statement.service;
import com.moneybags.statement.dto.StatementResponse;
import java.time.LocalDateTime;
public interface StatementService {
    StatementResponse generate(String accountNo, LocalDateTime from, LocalDateTime to);
}
