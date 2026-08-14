package com.moneybags.branch_employee_service.service;

import com.moneybags.branch_employee_service.dto.request.ApprovalAuthorityItemRequest;
import com.moneybags.branch_employee_service.dto.request.CreateEmployeeRequest;
import com.moneybags.branch_employee_service.dto.request.TransferEmployeeRequest;
import com.moneybags.branch_employee_service.dto.request.UpdateEmployeeRequest;
import com.moneybags.branch_employee_service.dto.request.UpdateManagerRequest;
import com.moneybags.branch_employee_service.dto.response.ApprovalAuthorityResponse;
import com.moneybags.branch_employee_service.entity.Employee;
import com.moneybags.branch_employee_service.entity.EmployeeApprovalAuthority;
import com.moneybags.branch_employee_service.entity.EmployeeBranchTransfer;
import com.moneybags.branch_employee_service.exception.ConflictException;
import com.moneybags.branch_employee_service.exception.NotFoundException;
import com.moneybags.branch_employee_service.repository.BranchRepository;
import com.moneybags.branch_employee_service.repository.EmployeeApprovalAuthorityRepository;
import com.moneybags.branch_employee_service.repository.EmployeeBranchTransferRepository;
import com.moneybags.branch_employee_service.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final EmployeeApprovalAuthorityRepository approvalAuthorityRepository;
    private final EmployeeBranchTransferRepository branchTransferRepository;
    private final IdentityClient identityClient;

    public EmployeeService(EmployeeRepository employeeRepository,
                           BranchRepository branchRepository,
                           EmployeeApprovalAuthorityRepository approvalAuthorityRepository,
                           EmployeeBranchTransferRepository branchTransferRepository,
                           IdentityClient identityClient) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.approvalAuthorityRepository = approvalAuthorityRepository;
        this.branchTransferRepository = branchTransferRepository;
        this.identityClient = identityClient;
    }

    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    public Employee getById(Long id) {
        return requireEmployee(id);
    }

    public Employee create(CreateEmployeeRequest request) {
        ensureBranchExists(request.getBranchId());
        employeeRepository.findByEmployeeCode(request.getEmployeeCode()).ifPresent(employee -> {
            throw new ConflictException("employeeCode already exists: " + request.getEmployeeCode());
        });
        employeeRepository.findByUserId(request.getUserId()).ifPresent(employee -> {
            throw new ConflictException("userId already linked to an employee: " + request.getUserId());
        });
        if (!identityClient.userExists(request.getUserId())) {
            throw new NotFoundException("Identity user not found: " + request.getUserId());
        }
        if (request.getReportingManagerId() != null) {
            requireEmployee(request.getReportingManagerId());
        }

        Employee employee = new Employee();
        employee.setId(employeeRepository.findMaxId() + 1);
        employee.setUserId(request.getUserId());
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setDob(request.getDob());
        employee.setBranchId(request.getBranchId());
        employee.setDesignation(request.getDesignation());
        employee.setReportingManagerId(request.getReportingManagerId());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setStatus("ACTIVE");
        Employee saved = employeeRepository.save(employee);

        // Denormalise employment onto the identity user so the gateway resolves a
        // session in one call. Best-effort by design -- see IdentityClient.
        branchRepository.findById(saved.getBranchId()).ifPresent(branch ->
                identityClient.publishEmployment(saved.getUserId(), saved.getId(), branch.getBranchCode()));
        return saved;
    }

    public Employee update(Long id, UpdateEmployeeRequest request) {
        Employee employee = requireEmployee(id);

        if (request.getDob() != null) employee.setDob(request.getDob());
        if (request.getDesignation() != null) employee.setDesignation(request.getDesignation());
        if (request.getReportingManagerId() != null) {
            ensureNotSelfManager(id, request.getReportingManagerId());
            requireEmployee(request.getReportingManagerId());
            employee.setReportingManagerId(request.getReportingManagerId());
        }
        if (request.getStatus() != null) employee.setStatus(request.getStatus());

        return employeeRepository.save(employee);
    }

    public List<Employee> getReports(Long id) {
        requireEmployee(id);
        return employeeRepository.findByReportingManagerId(id);
    }

    public Employee updateManager(Long id, UpdateManagerRequest request) {
        Employee employee = requireEmployee(id);
        Long managerId = request.getReportingManagerId();
        if (managerId != null) {
            ensureNotSelfManager(id, managerId);
            requireEmployee(managerId);
        }
        employee.setReportingManagerId(managerId);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee transfer(Long id, TransferEmployeeRequest request) {
        Employee employee = requireEmployee(id);
        ensureBranchExists(request.getToBranchId());

        Long previousBranchId = employee.getBranchId();
        employee.setBranchId(request.getToBranchId());
        Employee updatedEmployee = employeeRepository.save(employee);

        EmployeeBranchTransfer transfer = new EmployeeBranchTransfer();
        transfer.setEmployeeId(id);
        transfer.setFromBranchId(previousBranchId);
        transfer.setToBranchId(request.getToBranchId());
        transfer.setRemarks(request.getRemarks());
        branchTransferRepository.save(transfer);

        return updatedEmployee;
    }

    public List<EmployeeApprovalAuthority> getApprovalAuthorities(Long employeeId) {
        requireEmployee(employeeId);
        return approvalAuthorityRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public List<EmployeeApprovalAuthority> replaceApprovalAuthorities(
            Long employeeId, List<ApprovalAuthorityItemRequest> items) {
        requireEmployee(employeeId);
        approvalAuthorityRepository.deleteByEmployeeId(employeeId);

        List<EmployeeApprovalAuthority> saved = new ArrayList<>();
        for (ApprovalAuthorityItemRequest item : items) {
            EmployeeApprovalAuthority authority = new EmployeeApprovalAuthority();
            authority.setEmployeeId(employeeId);
            authority.setActionType(item.getActionType());
            authority.setMaxAmount(item.getMaxAmount());
            authority.setCurrency(item.getCurrency() == null || item.getCurrency().isBlank()
                    ? "INR" : item.getCurrency());
            saved.add(approvalAuthorityRepository.save(authority));
        }
        return saved;
    }

    public ApprovalAuthorityResponse getApprovalAuthority(Long employeeId, String actionType) {
        EmployeeApprovalAuthority authority = approvalAuthorityRepository
                .findByEmployeeIdAndActionType(employeeId, actionType)
                .orElseThrow(() -> new NotFoundException(
                        "Approval authority not found for employee " + employeeId + " and action " + actionType));
        return new ApprovalAuthorityResponse(
                authority.getEmployeeId(), authority.getActionType(), authority.getMaxAmount(), authority.getCurrency());
    }

    private Employee requireEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found: " + id));
    }

    private void ensureBranchExists(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new NotFoundException("Branch not found: " + id);
        }
    }

    private void ensureNotSelfManager(Long employeeId, Long managerId) {
        if (employeeId.equals(managerId)) {
            throw new ConflictException("Employee cannot be their own manager: " + employeeId);
        }
    }
}
