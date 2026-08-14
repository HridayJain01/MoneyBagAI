package com.moneybags.branch_employee_service.repository;

import com.moneybags.branch_employee_service.entity.BranchHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchHolidayRepository extends JpaRepository<BranchHoliday, Long> {
    List<BranchHoliday> findByBranchId(Long branchId);
    Optional<BranchHoliday> findByIdAndBranchId(Long id, Long branchId);
}
