package com.moneybags.customer.controller;
import com.moneybags.customer.dto.CustomerOperations.*;
import com.moneybags.customer.service.CustomerOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/customers") @RequiredArgsConstructor
public class CustomerOperationsController {
    private final CustomerOperationsService service;
    @PatchMapping("/{cif}") Object update(@PathVariable String cif,@Valid @RequestBody Update r){return service.update(cif,r);}
    @GetMapping("/{cif}/summary") Object summary(@PathVariable String cif){return service.summary(cif);}
    @GetMapping("/{cif}/completeness") Object completeness(@PathVariable String cif){return service.completeness(cif);}
    @PatchMapping("/{cif}/status") Object status(@PathVariable String cif,@RequestParam com.moneybags.customer.enums.CustomerStatus status){return service.setStatus(cif,status);}
    @PutMapping("/{cif}/relationship-manager/{empId}") Object manager(@PathVariable String cif,@PathVariable Long empId){return service.assignManager(cif,empId);}
    @DeleteMapping("/{cif}/relationship-manager") Object removeManager(@PathVariable String cif){return service.removeManager(cif);}
    @GetMapping("/relationship-manager/{empId}") List<?> managerCustomers(@PathVariable Long empId){return service.byManager(empId);}
    @PostMapping("/{cif}/addresses") ResponseEntity<?> addAddress(@PathVariable String cif,@Valid @RequestBody Address r){return ResponseEntity.status(HttpStatus.CREATED).body(service.addAddress(cif,r));}
    @PutMapping("/{cif}/addresses/{id}") Object updateAddress(@PathVariable String cif,@PathVariable Long id,@Valid @RequestBody Address r){return service.updateAddress(cif,id,r);}
    @DeleteMapping("/{cif}/addresses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteAddress(@PathVariable String cif,@PathVariable Long id){service.removeAddress(cif,id);}
    @GetMapping("/{cif}/addresses") List<?> addresses(@PathVariable String cif){return service.addresses(cif);}
    @PostMapping("/{cif}/kyc-documents") ResponseEntity<?> kyc(@PathVariable String cif,@Valid @RequestBody KycSubmit r){return ResponseEntity.status(HttpStatus.CREATED).body(service.submitKyc(cif,r));}
    @PutMapping("/{cif}/kyc-documents/{id}/assignment/{empId}") Object assignKyc(@PathVariable String cif,@PathVariable Long id,@PathVariable Long empId){return service.assignKyc(cif,id,empId);}
    @PatchMapping("/{cif}/kyc-documents/{id}/decision") Object kycDecision(@PathVariable String cif,@PathVariable Long id,@RequestBody KycDecision r){return service.decideKyc(cif,id,r);}
    @GetMapping("/kyc/pending") List<?> pendingKyc(){return service.pendingKyc();}
    @GetMapping("/kyc/re-kyc-required") List<?> reKycRequired(){return service.reKycRequired();}
    @GetMapping("/{cif}/kyc-rejections") List<?> kycHistory(@PathVariable String cif){return service.kycHistory(cif);}
    @PostMapping("/{cif}/beneficiaries") ResponseEntity<?> beneficiary(@PathVariable String cif,@Valid @RequestBody BeneficiaryRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.addBeneficiary(cif,r));}
    @PutMapping("/{cif}/beneficiaries/{id}") Object updateBeneficiary(@PathVariable String cif,@PathVariable Long id,@Valid @RequestBody BeneficiaryRequest r){return service.updateBeneficiary(cif,id,r);}
    @PostMapping("/{cif}/beneficiaries/{id}/activate") Object activate(@PathVariable String cif,@PathVariable Long id){return service.activateBeneficiary(cif,id);}
    @GetMapping("/{cif}/beneficiaries/{id}/eligibility") Object eligibility(@PathVariable String cif,@PathVariable Long id){return service.beneficiaryEligibility(cif,id);}
    @PatchMapping("/{cif}/beneficiaries/{id}/block") Object block(@PathVariable String cif,@PathVariable Long id,@RequestParam boolean blocked){return service.setBeneficiaryBlocked(cif,id,blocked);}
    @DeleteMapping("/{cif}/beneficiaries/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteBeneficiary(@PathVariable String cif,@PathVariable Long id){service.removeBeneficiary(cif,id);}
    @GetMapping("/{cif}/beneficiaries") List<?> beneficiaries(@PathVariable String cif,@RequestParam(required=false) String status){return service.beneficiaries(cif,status);}
    @GetMapping("/beneficiaries/{id}/history") List<?> history(@PathVariable Long id){return service.beneficiaryHistory(id);}
}
