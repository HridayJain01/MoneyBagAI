package com.moneybags.customer.controller;

import com.moneybags.customer.entity.CustomerDomainEvent;
import com.moneybags.customer.enums.EventPublicationStatus;
import com.moneybags.customer.service.CustomerEventOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/events")
@RequiredArgsConstructor
public class CustomerEventController {
    private final CustomerEventOutboxService service;

    @GetMapping
    public List<CustomerDomainEvent> find(
            @RequestParam(required = false) EventPublicationStatus status
    ) {
        return service.find(status);
    }
}
