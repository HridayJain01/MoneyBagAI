package com.moneybags.account.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record AccountRequest(
        @NotBlank @Size(max = 30) String accountNo,
        @NotBlank @Size(max = 30) String cifNo,
        @NotBlank @Size(max = 30) String productCode,
        @NotBlank @Size(max = 20) String branchCode,
        @NotNull @DecimalMin("0.0") BigDecimal openingBalance
) {}
