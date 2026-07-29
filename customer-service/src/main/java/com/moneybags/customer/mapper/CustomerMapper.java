package com.moneybags.customer.mapper;
import com.moneybags.customer.dto.*;
import com.moneybags.customer.entity.Customer;
import org.mapstruct.*;
@Mapper(componentModel = "spring")
public interface CustomerMapper {
    @Mapping(target = "cifNo", ignore = true)
    Customer toEntity(CustomerRequest request);
    CustomerResponse toResponse(Customer customer);
}
