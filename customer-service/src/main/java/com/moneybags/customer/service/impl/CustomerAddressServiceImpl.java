package com.moneybags.customer.service.impl;

import com.moneybags.customer.dto.CustomerOperations.Address;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.entity.CustomerAddress;
import com.moneybags.customer.exception.ResourceNotFoundException;
import com.moneybags.customer.repository.CustomerAddressRepository;
import com.moneybags.customer.repository.CustomerRepository;
import com.moneybags.customer.service.CustomerAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAddressServiceImpl
        implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerRepository customerRepository;

    private Customer getCustomer(String cif) {
        return customerRepository.findById(cif)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Customer not found: " + cif
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAddress getByIdAndCif(
            String cif,
            Long addressId
    ) {
        CustomerAddress address = addressRepository
                .findById(addressId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Address not found: " + addressId
                        )
                );

        if (!address.getCustomer().getCifNo().equals(cif)) {
            throw new ResourceNotFoundException(
                    "Address not found: " + addressId
            );
        }

        return address;
    }

    @Override
    public CustomerAddress add(
            String cif,
            Address request
    ) {
        Customer customer = getCustomer(cif);
        CustomerAddress address = request.isCurrent()
                ? addressRepository.findFirstByCustomerCifNoAndAddressTypeAndIsCurrentTrue(
                                cif, request.addressType())
                        .orElseGet(() -> CustomerAddress.builder().customer(customer).build())
                : CustomerAddress.builder().customer(customer).build();

        address.setAddressType(request.addressType());
        address.setLine1(request.line1());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPincode(request.pincode());
        address.setCountry(request.country());
        address.setIsCurrent(request.isCurrent());

        return addressRepository.save(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddress> findByCustomer(String cif) {
        getCustomer(cif);

        return addressRepository.findByCustomerCifNo(cif);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAddress(String cif) {
        return addressRepository.existsByCustomerCifNo(cif);
    }
}
