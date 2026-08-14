package com.moneybags.branch_employee_service.controller;

import com.moneybags.branch_employee_service.dto.request.CreateBranchRequest;
import com.moneybags.branch_employee_service.dto.request.CreateHolidayRequest;
import com.moneybags.branch_employee_service.dto.request.UpdateBranchRequest;
import com.moneybags.branch_employee_service.dto.request.WorkingHoursDayRequest;
import com.moneybags.branch_employee_service.entity.Branch;
import com.moneybags.branch_employee_service.entity.BranchHoliday;
import com.moneybags.branch_employee_service.entity.BranchWorkingHours;
import com.moneybags.branch_employee_service.exception.NotFoundException;
import com.moneybags.branch_employee_service.repository.BranchRepository;
import com.moneybags.branch_employee_service.service.BranchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Branches", description = "Branch master data")
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private final BranchRepository branchRepository;
    private final BranchService branchService;

    public BranchController(BranchRepository branchRepository, BranchService branchService) {
        this.branchRepository = branchRepository;
        this.branchService = branchService;
    }

    @GetMapping
    public List<Branch> getAll() {
        return branchRepository.findAll();
    }

    @GetMapping("/{id}")
    public Branch getById(@PathVariable Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Branch not found: " + id));
    }

    @PostMapping
    public ResponseEntity<Branch> create(@Valid @RequestBody CreateBranchRequest request) {
        Branch created = branchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public Branch update(@PathVariable Long id, @Valid @RequestBody UpdateBranchRequest request) {
        return branchService.update(id, request);
    }

    @PostMapping("/{id}/activate")
public Branch activate(@PathVariable Long id) {
    return branchService.setStatus(id, "ACTIVE");
}

@PostMapping("/{id}/deactivate")
public Branch deactivate(@PathVariable Long id) {
    return branchService.setStatus(id, "INACTIVE");
}

@GetMapping("/by-ifsc/{ifsc}")
    public Branch getByIfsc(@PathVariable String ifsc) {
        return branchRepository.findByIfscCode(ifsc)
                .orElseThrow(() -> new NotFoundException("Branch not found for IFSC: " + ifsc));
    }

    @GetMapping("/{id}/working-hours")
    public List<BranchWorkingHours> getWorkingHours(@PathVariable Long id) {
        return branchService.getWorkingHours(id);
    }

    @PutMapping("/{id}/working-hours")
    public List<BranchWorkingHours> replaceWorkingHours(
            @PathVariable Long id,
            @RequestBody List<@Valid WorkingHoursDayRequest> days) {
        return branchService.replaceWorkingHours(id, days);
    }

    @GetMapping("/{id}/holidays")
    public List<BranchHoliday> getHolidays(@PathVariable Long id) {
        return branchService.getHolidays(id);
    }

    @PostMapping("/{id}/holidays")
    public ResponseEntity<BranchHoliday> addHoliday(
            @PathVariable Long id,
            @Valid @RequestBody CreateHolidayRequest request) {
        BranchHoliday created = branchService.addHoliday(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}/holidays/{holidayId}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id, @PathVariable Long holidayId) {
        branchService.deleteHoliday(id, holidayId);
        return ResponseEntity.noContent().build();
    }
}
