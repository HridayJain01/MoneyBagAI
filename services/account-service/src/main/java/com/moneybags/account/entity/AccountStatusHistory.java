package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "account_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "from_status", length = 24)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 24)
    private String toStatus;

    @Column(length = 500)
    private String reason;

    @Column(name = "changed_by_employee_id", length = 64)
    private String changedByEmployeeId;

    @Column(nullable = false, length = 24)
    private String source;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
