package com.moneybags.account.mapper;
import com.moneybags.account.dto.*;
import com.moneybags.account.entity.Account;
import org.mapstruct.*;
@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "balance", source = "openingBalance")
    @Mapping(target = "minBalance", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "openedOn", ignore = true)
    @Mapping(target = "closedOn", ignore = true)
    @Mapping(target = "version", ignore = true)
    Account toEntity(AccountRequest request);
    AccountResponse toResponse(Account account);
}
