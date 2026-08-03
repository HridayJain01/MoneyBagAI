package com.moneybags.customer.service;
import com.moneybags.customer.dto.CustomerOperations.*;
import java.util.*;
public interface CustomerOperationsService {
    Object update(String cif, Update request); Object summary(String cif); Object completeness(String cif); Object setStatus(String cif, com.moneybags.customer.enums.CustomerStatus status);
    Object assignManager(String cif, Long employeeId); Object removeManager(String cif); List<?> byManager(Long employeeId);
    Object addAddress(String cif, Address request); Object updateAddress(String cif, Long id, Address request); void removeAddress(String cif, Long id); List<?> addresses(String cif);
    Object submitKyc(String cif, KycSubmit request); Object assignKyc(String cif, Long docId, Long employeeId); Object decideKyc(String cif, Long docId, KycDecision request); List<?> pendingKyc(); List<?> reKycRequired(); List<?> kycHistory(String cif);
    Object addBeneficiary(String cif, BeneficiaryRequest request); Object updateBeneficiary(String cif, Long id, BeneficiaryRequest request); Object activateBeneficiary(String cif, Long id); Object beneficiaryEligibility(String cif, Long id); Object setBeneficiaryBlocked(String cif, Long id, boolean blocked); void removeBeneficiary(String cif, Long id); List<?> beneficiaries(String cif, String status); List<?> beneficiaryHistory(Long id);
}
