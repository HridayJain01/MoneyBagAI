package com.moneybags.branch_employee_service.repository;

import com.moneybags.branch_employee_service.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByIfscCode(String ifscCode);
    Optional<Branch> findByBranchCode(String branchCode);

    @Query("SELECT COALESCE(MAX(b.id), 0) FROM Branch b")
    Long findMaxId();
}
