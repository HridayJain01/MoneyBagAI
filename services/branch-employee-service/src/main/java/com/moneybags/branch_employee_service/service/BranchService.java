package com.moneybags.branch_employee_service.service;

import com.moneybags.branch_employee_service.dto.request.CreateBranchRequest;
import com.moneybags.branch_employee_service.dto.request.CreateHolidayRequest;
import com.moneybags.branch_employee_service.dto.request.UpdateBranchRequest;
import com.moneybags.branch_employee_service.dto.request.WorkingHoursDayRequest;
import com.moneybags.branch_employee_service.entity.Branch;
import com.moneybags.branch_employee_service.entity.BranchHoliday;
import com.moneybags.branch_employee_service.entity.BranchWorkingHours;
import com.moneybags.branch_employee_service.exception.ConflictException;
import com.moneybags.branch_employee_service.exception.NotFoundException;
import com.moneybags.branch_employee_service.repository.BranchRepository;
import com.moneybags.branch_employee_service.repository.BranchHolidayRepository;
import com.moneybags.branch_employee_service.repository.BranchWorkingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchHolidayRepository holidayRepository;
    private final BranchWorkingHoursRepository workingHoursRepository;

    public BranchService(BranchRepository branchRepository,
                         BranchHolidayRepository holidayRepository,
                         BranchWorkingHoursRepository workingHoursRepository) {
        this.branchRepository = branchRepository;
        this.holidayRepository = holidayRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    public Branch create(CreateBranchRequest request) {
        branchRepository.findByBranchCode(request.getBranchCode()).ifPresent(b -> {
            throw new ConflictException("branchCode already exists: " + request.getBranchCode());
        });
        branchRepository.findByIfscCode(request.getIfscCode()).ifPresent(b -> {
            throw new ConflictException("ifscCode already exists: " + request.getIfscCode());
        });

        Branch branch = new Branch();
        branch.setId(branchRepository.findMaxId() + 1);
        branch.setBranchCode(request.getBranchCode());
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setState(request.getState());
        branch.setPincode(request.getPincode());
        branch.setIfscCode(request.getIfscCode());
        branch.setStatus("ACTIVE");

        return branchRepository.save(branch);
    }

    public Branch update(Long id, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Branch not found: " + id));

        if (request.getName() != null) branch.setName(request.getName());
        if (request.getAddress() != null) branch.setAddress(request.getAddress());
        if (request.getCity() != null) branch.setCity(request.getCity());
        if (request.getState() != null) branch.setState(request.getState());
        if (request.getPincode() != null) branch.setPincode(request.getPincode());

        return branchRepository.save(branch);
    }

    public Branch setStatus(Long id, String newStatus) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Branch not found: " + id));
        branch.setStatus(newStatus);
        return branchRepository.save(branch);
    }

    public List<BranchWorkingHours> getWorkingHours(Long branchId) {
        ensureBranchExists(branchId);
        return workingHoursRepository.findByBranchId(branchId);
    }

    public List<BranchHoliday> getHolidays(Long branchId) {
        ensureBranchExists(branchId);
        return holidayRepository.findByBranchId(branchId);
    }

    public BranchHoliday addHoliday(Long branchId, CreateHolidayRequest request) {
        ensureBranchExists(branchId);

        BranchHoliday holiday = new BranchHoliday();
        holiday.setBranchId(branchId);
        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setDescription(request.getDescription());
        return holidayRepository.save(holiday);
    }

    public void deleteHoliday(Long branchId, Long holidayId) {
        ensureBranchExists(branchId);
        BranchHoliday holiday = holidayRepository.findByIdAndBranchId(holidayId, branchId)
                .orElseThrow(() -> new NotFoundException(
                        "Holiday " + holidayId + " not found for branch " + branchId));
        holidayRepository.delete(holiday);
    }

    @Transactional
    public List<BranchWorkingHours> replaceWorkingHours(Long branchId, List<WorkingHoursDayRequest> days) {
        ensureBranchExists(branchId);

        workingHoursRepository.deleteByBranchId(branchId);

        List<BranchWorkingHours> saved = new ArrayList<>();
        for (WorkingHoursDayRequest day : days) {
            BranchWorkingHours entity = new BranchWorkingHours();
            entity.setBranchId(branchId);
            entity.setDayOfWeek(day.getDayOfWeek());
            entity.setOpenTime(day.getOpenTime());
            entity.setCloseTime(day.getCloseTime());
            entity.setIsClosed(day.isClosed() ? "Y" : "N");
            saved.add(workingHoursRepository.save(entity));
        }
        return saved;
    }

    private void ensureBranchExists(Long branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new NotFoundException("Branch not found: " + branchId);
        }
    }
}
