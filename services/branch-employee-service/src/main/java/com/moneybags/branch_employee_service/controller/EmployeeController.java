package com.moneybags.branch_employee_service.controller;

import com.moneybags.branch_employee_service.dto.request.ApprovalAuthorityItemRequest;
import com.moneybags.branch_employee_service.dto.request.CreateEmployeeRequest;
import com.moneybags.branch_employee_service.dto.request.TransferEmployeeRequest;
import com.moneybags.branch_employee_service.dto.request.UpdateEmployeeRequest;
import com.moneybags.branch_employee_service.dto.request.UpdateManagerRequest;
import com.moneybags.branch_employee_service.entity.Employee;
import com.moneybags.branch_employee_service.entity.EmployeeApprovalAuthority;
import com.moneybags.branch_employee_service.security.TrustedHeaderAuthorization;
import com.moneybags.branch_employee_service.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "Employees", description = "Employee master data")
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final TrustedHeaderAuthorization authorization;

    public EmployeeController(EmployeeService employeeService, TrustedHeaderAuthorization authorization) {
        this.employeeService = employeeService;
        this.authorization = authorization;
    }

    @GetMapping
    public List<Employee> getAll() {
        return employeeService.getAll();
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Employee> create(
            @Valid @RequestBody CreateEmployeeRequest request,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        authorization.require(permissions, "EMPLOYEE_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @PatchMapping("/{id}")
    public Employee update(
            @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        authorization.require(permissions, "EMPLOYEE_MANAGE");
        return employeeService.update(id, request);
    }

    @GetMapping("/{id}/reports")
    public List<Employee> getReports(@PathVariable Long id) {
        return employeeService.getReports(id);
    }

    @PutMapping("/{id}/manager")
    public Employee updateManager(
            @PathVariable Long id, @Valid @RequestBody UpdateManagerRequest request,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        authorization.require(permissions, "EMPLOYEE_MANAGE");
        return employeeService.updateManager(id, request);
    }

    @PostMapping("/{id}/transfer")
    public Employee transfer(
            @PathVariable Long id, @Valid @RequestBody TransferEmployeeRequest request,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        authorization.require(permissions, "EMPLOYEE_MANAGE");
        return employeeService.transfer(id, request);
    }

    @GetMapping("/{id}/approval-authority")
    public List<EmployeeApprovalAuthority> getApprovalAuthorities(@PathVariable Long id) {
        return employeeService.getApprovalAuthorities(id);
    }

    @PutMapping("/{id}/approval-authority")
    public List<EmployeeApprovalAuthority> replaceApprovalAuthorities(
            @PathVariable Long id,
            @RequestBody List<@Valid ApprovalAuthorityItemRequest> items,
            @RequestHeader(value = "X-Permissions", required = false) String permissions) {
        authorization.require(permissions, "EMPLOYEE_MANAGE");
        return employeeService.replaceApprovalAuthorities(id, items);
    }
}
