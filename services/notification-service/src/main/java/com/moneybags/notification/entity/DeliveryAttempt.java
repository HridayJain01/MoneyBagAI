package com.moneybags.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "delivery_attempts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false, length = 36)
    private String notificationId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(nullable = false, length = 16)
    private String outcome;

    @Column(length = 500)
    private String detail;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}
