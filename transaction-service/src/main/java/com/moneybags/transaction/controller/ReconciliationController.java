package com.moneybags.transaction.controller;

import com.moneybags.transaction.api.TransactionModels.*;
import com.moneybags.transaction.domain.FinancialEnums.ReconciliationStatus;
import com.moneybags.transaction.entity.ReconciliationException;
import com.moneybags.transaction.security.RequestActorResolver;
import com.moneybags.transaction.service.ReconciliationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor
public class ReconciliationController {
    private final ReconciliationService service; private final RequestActorResolver actors;
    @GetMapping("/api/v1/reconciliation/exceptions") public Page<ReconciliationException> list(@RequestParam(required=false)ReconciliationStatus status,@PageableDefault(size=20,sort="detectedAt",direction=Sort.Direction.DESC)Pageable page,HttpServletRequest request){staff(request);return service.list(status,page);}
    @GetMapping("/api/v1/reconciliation/exceptions/{id}") public ReconciliationException get(@PathVariable String id,HttpServletRequest request){staff(request);return service.get(id);}
    @PostMapping("/api/v1/reconciliation/exceptions/{id}/assign") public ReconciliationException assign(@PathVariable String id,@Valid @RequestBody AssignRequest body,HttpServletRequest request){staff(request);return service.assign(id,body.investigatorId());}
    @PostMapping("/api/v1/reconciliation/exceptions/{id}/resolve") public ReconciliationException resolve(@PathVariable String id,@Valid @RequestBody ResolveRequest body,HttpServletRequest request){staff(request);return service.resolve(id,body.resolution(),body.notes());}
    @PostMapping("/internal/v1/reconciliation/runs") public Map<String,Integer> run(){return Map.of("exceptionsCreated",service.run());}
    private void staff(HttpServletRequest request){if(actors.resolve(request).employeeId()==null)throw com.moneybags.transaction.exception.DomainException.forbidden("STAFF_SCOPE_REQUIRED","Staff scope is required");}
}
