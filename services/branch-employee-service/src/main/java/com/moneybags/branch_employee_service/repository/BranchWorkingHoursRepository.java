package com.moneybags.branch_employee_service.repository;


import com.moneybags.branch_employee_service.entity.BranchWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BranchWorkingHoursRepository extends JpaRepository<BranchWorkingHours, Long> {
    List<BranchWorkingHours> findByBranchId(Long branchId);
    void deleteByBranchId(Long branchId);
}
