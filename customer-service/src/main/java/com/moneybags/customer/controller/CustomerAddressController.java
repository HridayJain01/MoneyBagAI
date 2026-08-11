package com.moneybags.customer.controller;

import com.moneybags.customer.dto.CustomerOperations.Address;
import com.moneybags.customer.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService service;

    @PutMapping("/{cif}/addresses")
    public Object putAddress(
            @PathVariable String cif,
            @Valid @RequestBody Address request
    ) {
        return service.add(cif, request);
    }

    @GetMapping("/{cif}/addresses")
    public List<?> addresses(@PathVariable String cif) {
        return service.findByCustomer(cif);
    }
}
