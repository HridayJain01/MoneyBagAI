package com.moneybags.customer.mapper;
import com.moneybags.customer.dto.*;
import com.moneybags.customer.entity.Customer;
import org.mapstruct.*;
@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(target = "cifNo", ignore = true)
    @Mapping(target = "relationshipManagerEmpId", ignore = true)
    @Mapping(target = "kycFailureCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerRequest request);
    CustomerResponse toResponse(Customer customer);
}
