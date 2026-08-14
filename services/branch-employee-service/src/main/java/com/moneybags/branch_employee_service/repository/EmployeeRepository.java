package com.moneybags.branch_employee_service.repository;

import com.moneybags.branch_employee_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findByUserId(Long userId);
    List<Employee> findByReportingManagerId(Long reportingManagerId);

    @Query("SELECT COALESCE(MAX(e.id), 0) FROM Employee e")
    Long findMaxId();
}
