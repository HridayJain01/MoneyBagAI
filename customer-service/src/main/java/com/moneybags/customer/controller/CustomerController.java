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
    CustomerResponse findByCif(@PathVariable String cifNo) { return service.findByCif(cifNo); }
    @GetMapping
    List<CustomerResponse> findAll() { return service.findAll(); }
    @GetMapping("/search")
    List<CustomerResponse> search(@RequestParam String query) {
        String value = query.toLowerCase();
        return service.findAll().stream().filter(c ->
                c.cifNo().toLowerCase().contains(value) || c.panNo().toLowerCase().contains(value) ||
                c.firstName().toLowerCase().contains(value) || (c.lastName() != null && c.lastName().toLowerCase().contains(value)) ||
                c.mobile().contains(query) || (c.email() != null && c.email().toLowerCase().contains(value)) ||
                (c.userId() != null && c.userId().toString().equals(query))).toList();
    }
}
