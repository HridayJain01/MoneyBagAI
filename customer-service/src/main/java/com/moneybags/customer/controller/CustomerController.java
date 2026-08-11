package com.moneybags.customer.controller;

import com.moneybags.customer.dto.CustomerRequest;
import com.moneybags.customer.dto.CustomerResponse;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerRequest request
    ) {
        System.out.println("hi");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/{cifNo}")
    public Customer findByCif(
            @PathVariable String cifNo
    ) {
        return service.getByCif(cifNo);
    }

    @GetMapping
    public List<CustomerResponse> findAll() {
        System.out.println("lol");
        return service.findAll();
    }

    @GetMapping("/search")
    public List<CustomerResponse> search(
            @RequestParam String query
    ) {
        return service.search(query);
    }
}