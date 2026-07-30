package com.moneybags.account.dto;
import com.moneybags.account.enums.AccountStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
public record AccountResponse(String accountNo, String cifNo, String productCode, String branchCode,
                              BigDecimal balance, BigDecimal minBalance, AccountStatus status,
                              LocalDate openedOn, LocalDate closedOn, Integer version) {}
