package com.moneybags.account.entity;

import com.moneybags.account.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_approvals")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountApproval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;
    @Column(name = "account_no", nullable = false, length = 30)
    private String accountNo;
    @Column(name = "emp_id", nullable = false)
    private Long empId;
    @Enumerated(EnumType.STRING) @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus;
    @Column(length = 500)
    private String remarks;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
