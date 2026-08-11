package com.moneybags.customer.service.impl;

import com.moneybags.customer.dto.CustomerOperations.BeneficiaryRequest;
import com.moneybags.customer.entity.Beneficiary;
import com.moneybags.customer.entity.Customer;
import com.moneybags.customer.enums.CustomerStatus;
import com.moneybags.customer.enums.KycStatus;
import com.moneybags.customer.exception.ConflictException;
import com.moneybags.customer.exception.ResourceNotFoundException;
import com.moneybags.customer.repository.BeneficiaryRepository;
import com.moneybags.customer.repository.CustomerRepository;
import com.moneybags.customer.service.BeneficiaryHistoryService;
import com.moneybags.customer.service.BeneficiaryService;
import com.moneybags.customer.service.CustomerEventOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryServiceImpl
        implements BeneficiaryService {

    private static final long COOLING_PERIOD_HOURS = 0; // for testing later on will increase

    private static final String STATUS_PENDING =
            "PENDING_ACTIVATION";

    private static final String STATUS_ACTIVE =
            "ACTIVE";

    private static final String STATUS_BLOCKED =
            "BLOCKED";

    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;
    private final BeneficiaryHistoryService historyService;
    private final CustomerEventOutboxService eventOutboxService;

    private Customer getCustomer(String cif) {
        return customerRepository.findById(cif)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Customer not found: " + cif
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Beneficiary getByIdAndCif(
            String cif,
            Long beneficiaryId
    ) {
        Beneficiary beneficiary = beneficiaryRepository
                .findById(beneficiaryId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Beneficiary not found: "
                                        + beneficiaryId
                        )
                );

        if (!beneficiary.getCustomer().getCifNo().equals(cif)) {
            throw new ResourceNotFoundException(
                    "Beneficiary not found: " + beneficiaryId
            );
        }

        return beneficiary;
    }

    @Override
    public Beneficiary add(
            String cif,
            BeneficiaryRequest request
    ) {
        boolean alreadyExists = beneficiaryRepository
                .existsByCustomerCifNoAndBeneficiaryAccountNoAndBeneficiaryIfsc(
                        cif,
                        request.beneficiaryAccountNo(),
                        request.beneficiaryIfsc()
                );

        if (alreadyExists) {
            throw new ConflictException(
                    "Beneficiary already exists"
            );
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .customer(getCustomer(cif))
                .beneficiaryName(request.beneficiaryName())
                .beneficiaryAccountNo(
                        request.beneficiaryAccountNo()
                )
                .beneficiaryBankName(
                        request.beneficiaryBankName()
                )
                .beneficiaryIfsc(request.beneficiaryIfsc())
                .beneficiaryNickname(
                        request.beneficiaryNickname()
                )
                .beneficiaryType(request.beneficiaryType())
                .status(STATUS_PENDING)
                .build();

        Beneficiary savedBeneficiary =
                beneficiaryRepository.save(beneficiary);

        historyService.record(
                savedBeneficiary,
                "CREATED"
        );

        return savedBeneficiary;
    }

    @Override
    public Beneficiary update(
            String cif,
            Long beneficiaryId,
            BeneficiaryRequest request
    ) {
        Beneficiary beneficiary =
                getByIdAndCif(cif, beneficiaryId);

        boolean duplicate = beneficiaryRepository
                .existsByCustomerCifNoAndBeneficiaryAccountNoAndBeneficiaryIfscAndBeneficiaryIdNot(
                        cif,
                        request.beneficiaryAccountNo(),
                        request.beneficiaryIfsc(),
                        beneficiaryId
                );
        if (duplicate) {
            throw new ConflictException("Beneficiary already exists");
        }

        beneficiary.setBeneficiaryName(
                request.beneficiaryName()
        );
        beneficiary.setBeneficiaryAccountNo(
                request.beneficiaryAccountNo()
        );
        beneficiary.setBeneficiaryBankName(
                request.beneficiaryBankName()
        );
        beneficiary.setBeneficiaryIfsc(
                request.beneficiaryIfsc()
        );
        beneficiary.setBeneficiaryNickname(
                request.beneficiaryNickname()
        );
        beneficiary.setBeneficiaryType(
                request.beneficiaryType()
        );

        beneficiary.setStatus(STATUS_PENDING);
        beneficiary.setAddedAt(LocalDateTime.now());
        beneficiary.setActivatedAt(null);

        Beneficiary savedBeneficiary =
                beneficiaryRepository.save(beneficiary);

        historyService.record(
                savedBeneficiary,
                "UPDATED_COOLING_RESET"
        );

        return savedBeneficiary;
    }

    @Override
    public Beneficiary activate(
            String cif,
            Long beneficiaryId
    ) {
        Beneficiary beneficiary =
                getByIdAndCif(cif, beneficiaryId);

        if (!STATUS_PENDING.equals(beneficiary.getStatus())) {
            throw new ConflictException("Only a pending beneficiary can be activated");
        }

        LocalDateTime activationTime =
                beneficiary.getAddedAt()
                        .plusHours(COOLING_PERIOD_HOURS);

        if (activationTime.isAfter(LocalDateTime.now())) {
            throw new ConflictException(
                    "Beneficiary cooling period has not completed"
            );
        }

        beneficiary.setStatus(STATUS_ACTIVE);
        beneficiary.setActivatedAt(LocalDateTime.now());

        Beneficiary savedBeneficiary =
                beneficiaryRepository.save(beneficiary);

        historyService.record(
                savedBeneficiary,
                "ACTIVATED"
        );

        eventOutboxService.record(
                "BENEFICIARY",
                savedBeneficiary.getBeneficiaryId().toString(),
                "BeneficiaryActivated",
                Map.of(
                        "cifNo", cif,
                        "beneficiaryId", savedBeneficiary.getBeneficiaryId(),
                        "beneficiaryType", savedBeneficiary.getBeneficiaryType(),
                        "activatedAt", savedBeneficiary.getActivatedAt().toString()
                )
        );

        return savedBeneficiary;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> eligibility(
            String cif,
            Long beneficiaryId
    ) {
        Beneficiary beneficiary =
                getByIdAndCif(cif, beneficiaryId);

        LocalDateTime activationTime =
                beneficiary.getAddedAt()
                        .plusHours(COOLING_PERIOD_HOURS);

        long remainingSeconds = Math.max(
                0L,
                Duration.between(
                        LocalDateTime.now(),
                        activationTime
                ).toSeconds()
        );

        Customer customer = beneficiary.getCustomer();
        boolean beneficiaryActive = STATUS_ACTIVE.equals(beneficiary.getStatus());
        boolean customerEligible = customer.getStatus() == CustomerStatus.ACTIVE
                && customer.getKycStatus() == KycStatus.VERIFIED;
        boolean eligible = beneficiaryActive && customerEligible;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put(
                "beneficiaryId",
                beneficiary.getBeneficiaryId()
        );
        response.put("eligible", eligible);
        response.put(
                "coolingPeriodRemainingSeconds",
                remainingSeconds
        );
        response.put("activationAt", activationTime);
        response.put("beneficiaryStatus", beneficiary.getStatus());
        response.put("customerStatus", customer.getStatus());
        response.put("kycStatus", customer.getKycStatus());

        return response;
    }

    @Override
    public Beneficiary setBlocked(
            String cif,
            Long beneficiaryId,
            boolean blocked
    ) {
        Beneficiary beneficiary =
                getByIdAndCif(cif, beneficiaryId);

        String nextStatus;
        if (blocked) {
            nextStatus = STATUS_BLOCKED;
        } else if (beneficiary.getAddedAt().plusHours(COOLING_PERIOD_HOURS).isAfter(LocalDateTime.now())) {
            nextStatus = STATUS_PENDING;
        } else {
            nextStatus = STATUS_ACTIVE;
        }
        beneficiary.setStatus(nextStatus);

        Beneficiary savedBeneficiary =
                beneficiaryRepository.save(beneficiary);

        historyService.record(
                savedBeneficiary,
                blocked ? "BLOCKED" : "UNBLOCKED"
        );

        return savedBeneficiary;
    }

    @Override
    public void remove(
            String cif,
            Long beneficiaryId
    ) {
        Beneficiary beneficiary =
                getByIdAndCif(cif, beneficiaryId);

        historyService.record(
                beneficiary,
                "REMOVED"
        );

        beneficiaryRepository.delete(beneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Beneficiary> findByCustomer(
            String cif,
            String status
    ) {
        return findByCustomer(cif, status, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Beneficiary> findByCustomer(
            String cif,
            String status,
            String beneficiaryType
    ) {
        getCustomer(cif);

        List<Beneficiary> result =
                beneficiaryRepository
                        .findByCustomerCifNo(cif);

        return result.stream()
                .filter(beneficiary -> status == null || status.isBlank()
                        || status.equalsIgnoreCase(beneficiary.getStatus()))
                .filter(beneficiary -> beneficiaryType == null || beneficiaryType.isBlank()
                        || beneficiaryType.equalsIgnoreCase(beneficiary.getBeneficiaryType()))
                .toList();
    }
}
