package com.moneybags.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "account_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountApproval {

    @Id
    @Column(name = "approval_id", length = 36)
    private String approvalId;

    @Column(name = "application_id", nullable = false, length = 36)
    private String applicationId;

    @Column(name = "account_id", length = 36)
    private String accountId;

    @Column(name = "emp_id", nullable = false, length = 64)
    private String empId;

    @Column(nullable = false, length = 16)
    private String decision;

    @Column(length = 500)
    private String remarks;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
}
