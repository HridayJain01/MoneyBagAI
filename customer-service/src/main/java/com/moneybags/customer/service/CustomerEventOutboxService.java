package com.moneybags.customer.service;

import com.moneybags.customer.entity.CustomerDomainEvent;
import com.moneybags.customer.enums.EventPublicationStatus;

import java.util.List;
import java.util.Map;

public interface CustomerEventOutboxService {
    CustomerDomainEvent record(String aggregateType, String aggregateId, String eventType, Map<String, ?> payload);

    List<CustomerDomainEvent> find(EventPublicationStatus status);

    CustomerDomainEvent markPublished(Long eventId);

    CustomerDomainEvent markFailed(Long eventId, String reason);
}
