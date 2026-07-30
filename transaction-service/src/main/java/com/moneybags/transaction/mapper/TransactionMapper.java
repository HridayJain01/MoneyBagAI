package com.moneybags.transaction.mapper;
import com.moneybags.transaction.dto.*;
import com.moneybags.transaction.entity.Transaction;
import org.mapstruct.*;
@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(target = "txnId", ignore = true)
    @Mapping(target = "txnRef", ignore = true)
    @Mapping(target = "runningBalance", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "txnDate", ignore = true)
    Transaction toEntity(TransactionRequest request);
    TransactionResponse toResponse(Transaction transaction);
}
