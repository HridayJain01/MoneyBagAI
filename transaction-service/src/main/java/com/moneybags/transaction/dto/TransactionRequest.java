package com.moneybags.transaction.dto;
import com.moneybags.transaction.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record TransactionRequest(
        @NotBlank @Size(max = 80) String requestRef,
        @NotBlank @Size(max = 30) String accountNo,
        @NotNull TransactionType txnType,
        @NotNull DebitCredit drCr,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @Size(max = 30) String counterpartyAcct,
        @Size(max = 500) String narration,
        @NotBlank @Size(max = 80) String postedBy
) {}
