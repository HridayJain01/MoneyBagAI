package com.moneybags.security.entity;

import com.moneybags.security.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Branch {
    @Id @Column(name = "branch_code", length = 20)
    private String branchCode;
    @Column(name = "branch_name", nullable = false, length = 120)
    private String branchName;
    @Column(name = "ifsc_code", nullable = false, length = 20)
    private String ifscCode;
    @Column(nullable = false, length = 255)
    private String address;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(nullable = false, length = 80)
    private String state;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecordStatus status;
}
