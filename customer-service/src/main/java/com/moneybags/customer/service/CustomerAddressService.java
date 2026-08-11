package com.moneybags.customer.service;

import com.moneybags.customer.dto.CustomerOperations.Address;
import com.moneybags.customer.entity.CustomerAddress;

import java.util.List;

public interface CustomerAddressService {

    CustomerAddress getByIdAndCif(
            String cif,
            Long addressId
    );

    CustomerAddress add(
            String cif,
            Address request
    );

    List<CustomerAddress> findByCustomer(String cif);

    boolean hasAddress(String cif);
}