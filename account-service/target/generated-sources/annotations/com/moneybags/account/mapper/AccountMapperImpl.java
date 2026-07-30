package com.moneybags.account.mapper;

import com.moneybags.account.dto.AccountRequest;
import com.moneybags.account.dto.AccountResponse;
import com.moneybags.account.entity.Account;
import com.moneybags.account.enums.AccountStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T09:54:19+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class AccountMapperImpl implements AccountMapper {

    @Override
    public Account toEntity(AccountRequest request) {
        if ( request == null ) {
            return null;
        }

        Account.AccountBuilder account = Account.builder();

        account.balance( request.openingBalance() );
        account.accountNo( request.accountNo() );
        account.cifNo( request.cifNo() );
        account.productCode( request.productCode() );
        account.branchCode( request.branchCode() );

        return account.build();
    }

    @Override
    public AccountResponse toResponse(Account account) {
        if ( account == null ) {
            return null;
        }

        String accountNo = null;
        String cifNo = null;
        String productCode = null;
        String branchCode = null;
        BigDecimal balance = null;
        BigDecimal minBalance = null;
        AccountStatus status = null;
        LocalDate openedOn = null;
        LocalDate closedOn = null;
        Integer version = null;

        accountNo = account.getAccountNo();
        cifNo = account.getCifNo();
        productCode = account.getProductCode();
        branchCode = account.getBranchCode();
        balance = account.getBalance();
        minBalance = account.getMinBalance();
        status = account.getStatus();
        openedOn = account.getOpenedOn();
        closedOn = account.getClosedOn();
        version = account.getVersion();

        AccountResponse accountResponse = new AccountResponse( accountNo, cifNo, productCode, branchCode, balance, minBalance, status, openedOn, closedOn, version );

        return accountResponse;
    }
}
