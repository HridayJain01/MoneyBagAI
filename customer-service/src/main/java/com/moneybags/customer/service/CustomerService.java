package com.moneybags.customer.service;
import com.moneybags.customer.dto.*;
import java.util.List;
public interface CustomerService {
    CustomerResponse create(CustomerRequest request);
    CustomerResponse findByCif(Long cifNo);
    List<CustomerResponse> findAll();
}
