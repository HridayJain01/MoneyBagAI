package com.moneybags.identity.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "permission_code", nullable = false, length = 60, unique = true)
    private String permissionCode;

    @Column(length = 255)
    private String description;

    @Column(name = "service_name", length = 60)
    private String serviceName;

    @Column(length = 20)
    private String action;
}
