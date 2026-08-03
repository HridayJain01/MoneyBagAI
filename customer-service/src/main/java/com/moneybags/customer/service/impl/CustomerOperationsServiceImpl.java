package com.moneybags.customer.service.impl;

import com.moneybags.customer.dto.CustomerOperations.*;
import com.moneybags.customer.entity.*;
import com.moneybags.customer.enums.*;
import com.moneybags.customer.exception.*;
import com.moneybags.customer.repository.*;
import com.moneybags.customer.service.CustomerOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class CustomerOperationsServiceImpl implements CustomerOperationsService {
    private final CustomerRepository customers; private final CustomerAddressRepository addresses; private final KycDocumentRepository kyc;
    private final BeneficiaryRepository beneficiaries; private final KycRejectionHistoryRepository rejections; private final BeneficiaryChangeHistoryRepository beneficiaryHistory;
    private Customer customer(String cif) { return customers.findById(cif).orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + cif)); }
    private Beneficiary beneficiary(String cif, Long id) { var b=beneficiaries.findById(id).orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + id)); if (!b.getCustomer().getCifNo().equals(cif)) throw new ResourceNotFoundException("Beneficiary not found: " + id); return b; }
    private void history(Beneficiary b, String type) { beneficiaryHistory.save(BeneficiaryChangeHistory.builder().beneficiaryId(b.getBeneficiaryId()).cifNo(b.getCustomer().getCifNo()).changeType(type).build()); }
    public Object update(String cif, Update r) { var c=customer(cif); c.setFirstName(r.firstName()); c.setLastName(r.lastName()); c.setDob(r.dob()); c.setGender(r.gender()); c.setMobile(r.mobile()); c.setEmail(r.email()); return customers.save(c); }
    public Object summary(String cif) { var c=customer(cif); return Map.of("cifNo",c.getCifNo(),"status",c.getStatus(),"kycStatus",c.getKycStatus(),"profileComplete", completeness(cif)); }
    public Object completeness(String cif) { var c=customer(cif); int complete=0, total=7; if(c.getFirstName()!=null)complete++; if(c.getDob()!=null)complete++; if(c.getGender()!=null)complete++; if(c.getMobile()!=null)complete++; if(c.getEmail()!=null)complete++; if(c.getPanNo()!=null)complete++; if(!addresses.findByCustomerCifNo(cif).isEmpty())complete++; return Map.of("completedFields",complete,"totalFields",total,"percentage",complete*100/total); }
    public Object setStatus(String cif, CustomerStatus status) { var c=customer(cif); c.setStatus(status); return customers.save(c); }
    public Object assignManager(String cif, Long id) { var c=customer(cif); c.setRelationshipManagerEmpId(id); return customers.save(c); }
    public Object removeManager(String cif) { var c=customer(cif); c.setRelationshipManagerEmpId(null); return customers.save(c); }
    public List<?> byManager(Long id) { return customers.findByRelationshipManagerEmpId(id); }
    public Object addAddress(String cif, Address r) { return addresses.save(CustomerAddress.builder().customer(customer(cif)).addressType(r.addressType()).line1(r.line1()).city(r.city()).state(r.state()).pincode(r.pincode()).country(r.country()).isCurrent(r.isCurrent()).build()); }
    public Object updateAddress(String cif, Long id, Address r) { var a=addresses.findById(id).orElseThrow(()->new ResourceNotFoundException("Address not found: "+id)); if(!a.getCustomer().getCifNo().equals(cif))throw new ResourceNotFoundException("Address not found: "+id); a.setAddressType(r.addressType());a.setLine1(r.line1());a.setCity(r.city());a.setState(r.state());a.setPincode(r.pincode());a.setCountry(r.country());a.setIsCurrent(r.isCurrent());return addresses.save(a); }
    public void removeAddress(String cif, Long id) { var a=addresses.findById(id).orElseThrow(()->new ResourceNotFoundException("Address not found: "+id)); if(!a.getCustomer().getCifNo().equals(cif))throw new ResourceNotFoundException("Address not found: "+id); addresses.delete(a); }
    public List<?> addresses(String cif) { customer(cif); return addresses.findByCustomerCifNo(cif); }
    public Object submitKyc(String cif, KycSubmit r) { var c=customer(cif); c.setKycStatus(KycStatus.PENDING); return kyc.save(KycDocument.builder().customer(c).docType(r.docType()).docNumber(r.docNumber()).expiryDate(r.expiryDate()).filePath(r.filePath()).verifyStatus(DocumentVerifyStatus.PENDING).build()); }
    public Object assignKyc(String cif, Long id, Long employeeId) { var d=kyc.findById(id).orElseThrow(()->new ResourceNotFoundException("KYC document not found: "+id)); if(!d.getCustomer().getCifNo().equals(cif))throw new ResourceNotFoundException("KYC document not found: "+id); d.setAssignedToEmpId(employeeId); return kyc.save(d); }
    public Object decideKyc(String cif, Long id, KycDecision r) { var d=kyc.findById(id).orElseThrow(()->new ResourceNotFoundException("KYC document not found: "+id)); if(!d.getCustomer().getCifNo().equals(cif))throw new ResourceNotFoundException("KYC document not found: "+id); if(r.status()==DocumentVerifyStatus.REJECTED && (r.rejectionReason()==null||r.rejectionReason().isBlank())) throw new ConflictException("Rejection reason is required"); d.setVerifyStatus(r.status()); d.setVerifiedByEmpId(r.employeeId()); d.setVerifiedAt(LocalDateTime.now()); d.setRejectionReason(r.rejectionReason()); var c=d.getCustomer(); if(r.status()==DocumentVerifyStatus.REJECTED){ c.setKycStatus(KycStatus.REJECTED); c.setKycFailureCount(c.getKycFailureCount()+1); rejections.save(KycRejectionHistory.builder().cifNo(cif).docId(id).failureReason(r.rejectionReason()).rejectedByEmpId(r.employeeId()).attemptNumber(c.getKycFailureCount()).build()); } else if(r.status()==DocumentVerifyStatus.VERIFIED) c.setKycStatus(KycStatus.VERIFIED); customers.save(c); return kyc.save(d); }
    public List<?> pendingKyc() { return kyc.findByVerifyStatus(DocumentVerifyStatus.PENDING); }
    public List<?> reKycRequired() { return kyc.findByExpiryDateBefore(LocalDate.now().plusDays(30)); }
    public List<?> kycHistory(String cif) { customer(cif); return rejections.findByCifNoOrderByRejectedAtDesc(cif); }
    public Object addBeneficiary(String cif, BeneficiaryRequest r) { if(beneficiaries.existsByCustomerCifNoAndBeneficiaryAccountNoAndBeneficiaryIfsc(cif,r.beneficiaryAccountNo(),r.beneficiaryIfsc()))throw new ConflictException("Beneficiary already exists"); var b=beneficiaries.save(Beneficiary.builder().customer(customer(cif)).beneficiaryName(r.beneficiaryName()).beneficiaryAccountNo(r.beneficiaryAccountNo()).beneficiaryBankName(r.beneficiaryBankName()).beneficiaryIfsc(r.beneficiaryIfsc()).beneficiaryNickname(r.beneficiaryNickname()).beneficiaryType(r.beneficiaryType()).status("PENDING_ACTIVATION").build()); history(b,"CREATED"); return b; }
    public Object updateBeneficiary(String cif, Long id, BeneficiaryRequest r) { var b=beneficiary(cif,id); b.setBeneficiaryName(r.beneficiaryName());b.setBeneficiaryAccountNo(r.beneficiaryAccountNo());b.setBeneficiaryBankName(r.beneficiaryBankName());b.setBeneficiaryIfsc(r.beneficiaryIfsc());b.setBeneficiaryNickname(r.beneficiaryNickname());b.setBeneficiaryType(r.beneficiaryType());b.setStatus("PENDING_ACTIVATION");b.setAddedAt(LocalDateTime.now());b.setActivatedAt(null); b=beneficiaries.save(b);history(b,"UPDATED_COOLING_RESET");return b; }
    public Object activateBeneficiary(String cif, Long id) { var b=beneficiary(cif,id); if(b.getAddedAt().plusHours(24).isAfter(LocalDateTime.now())) throw new ConflictException("Beneficiary cooling period has not completed"); b.setStatus("ACTIVE");b.setActivatedAt(LocalDateTime.now());b=beneficiaries.save(b);history(b,"ACTIVATED");return b; }
    public Object beneficiaryEligibility(String cif, Long id) { var b=beneficiary(cif,id); var ready=b.getAddedAt().plusHours(24); long remaining=Math.max(0,Duration.between(LocalDateTime.now(),ready).toSeconds()); return Map.of("beneficiaryId",id,"eligible","ACTIVE".equals(b.getStatus()),"coolingPeriodRemainingSeconds",remaining,"activationAt",ready); }
    public Object setBeneficiaryBlocked(String cif, Long id, boolean blocked) { var b=beneficiary(cif,id); b.setStatus(blocked?"BLOCKED":"ACTIVE");b=beneficiaries.save(b);history(b,blocked?"BLOCKED":"UNBLOCKED");return b; }
    public void removeBeneficiary(String cif, Long id) { var b=beneficiary(cif,id);history(b,"REMOVED");beneficiaries.delete(b); }
    public List<?> beneficiaries(String cif,String status) { var result=beneficiaries.findByCustomerCifNo(cif); return status==null?result:result.stream().filter(b->status.equals(b.getStatus())).toList(); }
    public List<?> beneficiaryHistory(Long id) { return beneficiaryHistory.findByBeneficiaryIdOrderByChangedAtDesc(id); }
}
