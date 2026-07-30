package com.moneybags.security.entity;

import com.moneybags.security.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_id")
    private Long empId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;
    @Column(nullable = false, length = 80)
    private String designation;
    @Column(name = "branch_code", nullable = false, length = 20)
    private String branchCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RecordStatus status;
    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;
}
