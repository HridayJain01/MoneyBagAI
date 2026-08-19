package com.moneybags.customer.entity;

import com.moneybags.customer.enums.EventPublicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_domain_events", indexes = {
        @Index(name = "idx_customer_event_status", columnList = "publication_status, occurred_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDomainEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 50)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    private EventPublicationStatus publicationStatus;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @PrePersist
    void onCreate() {
        publicationStatus = publicationStatus == null
                ? EventPublicationStatus.PENDING
                : publicationStatus;
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }
}
