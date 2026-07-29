package com.moneybags.customer.mapper;

import com.moneybags.customer.dto.CustomerRequest;
import com.moneybags.customer.dto.CustomerResponse;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.enums.CustomerStatus;
import com.moneybags.customer.enums.Gender;
import com.moneybags.customer.enums.KycStatus;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T15:48:19+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public Customer toEntity(CustomerRequest request) {
        if ( request == null ) {
            return null;
        }

        Customer.CustomerBuilder customer = Customer.builder();

        customer.userId( request.userId() );
        customer.dob( request.dob() );
        customer.gender( request.gender() );
        customer.panNo( request.panNo() );
        customer.status( request.status() );
        customer.kycStatus( request.kycStatus() );

        return customer.build();
    }

    @Override
    public CustomerResponse toResponse(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        Long cifNo = null;
        Long userId = null;
        LocalDate dob = null;
        Gender gender = null;
        String panNo = null;
        CustomerStatus status = null;
        KycStatus kycStatus = null;

        cifNo = customer.getCifNo();
        userId = customer.getUserId();
        dob = customer.getDob();
        gender = customer.getGender();
        panNo = customer.getPanNo();
        status = customer.getStatus();
        kycStatus = customer.getKycStatus();

        CustomerResponse customerResponse = new CustomerResponse( cifNo, userId, dob, gender, panNo, status, kycStatus );

        return customerResponse;
    }
}
