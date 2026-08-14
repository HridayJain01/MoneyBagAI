package com.moneybags.branch_employee_service.controller;

import com.moneybags.branch_employee_service.dto.response.ApprovalAuthorityResponse;
import com.moneybags.branch_employee_service.service.EmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/approval-authority")
public class InternalApprovalAuthorityController {

    private final EmployeeService employeeService;

    public InternalApprovalAuthorityController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ApprovalAuthorityResponse getApprovalAuthority(
            @RequestParam Long employeeId,
            @RequestParam String actionType) {
        return employeeService.getApprovalAuthority(employeeId, actionType);
    }
}
