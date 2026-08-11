package com.moneybags.customer.repository;

import com.moneybags.customer.entity.CustomerDomainEvent;
import com.moneybags.customer.enums.EventPublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerDomainEventRepository extends JpaRepository<CustomerDomainEvent, Long> {
    List<CustomerDomainEvent> findByPublicationStatusOrderByOccurredAtAsc(EventPublicationStatus status);
}
