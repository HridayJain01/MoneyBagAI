package com.moneybags.customer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.customer.entity.CustomerDomainEvent;
import com.moneybags.customer.enums.EventPublicationStatus;
import com.moneybags.customer.exception.ResourceNotFoundException;
import com.moneybags.customer.repository.CustomerDomainEventRepository;
import com.moneybags.customer.service.CustomerEventOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerEventOutboxServiceImpl implements CustomerEventOutboxService {
    private final CustomerDomainEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public CustomerDomainEvent record(
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, ?> payload
    ) {
        try {
            return repository.save(CustomerDomainEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .publicationStatus(EventPublicationStatus.PENDING)
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Event payload cannot be serialized", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDomainEvent> find(EventPublicationStatus status) {
        return status == null
                ? repository.findAll()
                : repository.findByPublicationStatusOrderByOccurredAtAsc(status);
    }

    @Override
    public CustomerDomainEvent markPublished(Long eventId) {
        CustomerDomainEvent event = getById(eventId);
        event.setPublicationStatus(EventPublicationStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        event.setFailureReason(null);
        return repository.save(event);
    }

    @Override
    public CustomerDomainEvent markFailed(Long eventId, String reason) {
        CustomerDomainEvent event = getById(eventId);
        event.setPublicationStatus(EventPublicationStatus.FAILED);
        event.setFailureReason(reason);
        return repository.save(event);
    }

    private CustomerDomainEvent getById(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer event not found: " + eventId));
    }
}
