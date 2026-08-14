package com.moneybags.branch_employee_service.repository;

import com.moneybags.branch_employee_service.entity.EmployeeBranchTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeBranchTransferRepository extends JpaRepository<EmployeeBranchTransfer, Long> {
}
