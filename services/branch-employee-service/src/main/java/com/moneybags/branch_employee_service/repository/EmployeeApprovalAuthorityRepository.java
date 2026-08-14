package com.moneybags.branch_employee_service.repository;

import com.moneybags.branch_employee_service.entity.EmployeeApprovalAuthority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeApprovalAuthorityRepository extends JpaRepository<EmployeeApprovalAuthority, Long> {
    List<EmployeeApprovalAuthority> findByEmployeeId(Long employeeId);
    void deleteByEmployeeId(Long employeeId);
    Optional<EmployeeApprovalAuthority> findByEmployeeIdAndActionType(Long employeeId, String actionType);
}
