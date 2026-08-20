package com.moneybags.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTemplate {

    @Id
    @Column(name = "template_code", length = 60)
    private String templateCode;

    @Column(nullable = false, length = 10)
    private String channel;

    @Column(name = "subject_template", length = 255)
    private String subjectTemplate;

    @Lob
    @Column(name = "body_template", nullable = false)
    private String bodyTemplate;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
