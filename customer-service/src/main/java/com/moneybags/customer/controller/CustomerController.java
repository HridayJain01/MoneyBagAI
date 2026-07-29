package com.moneybags.customer.controller;
import com.moneybags.customer.dto.*;
import com.moneybags.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/customers") @RequiredArgsConstructor
public class CustomerController {
    private final CustomerService service;
    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
    @GetMapping("/{cifNo}")
    CustomerResponse findByCif(@PathVariable Long cifNo) { return service.findByCif(cifNo); }
    @GetMapping
    List<CustomerResponse> findAll() { return service.findAll(); }
}
