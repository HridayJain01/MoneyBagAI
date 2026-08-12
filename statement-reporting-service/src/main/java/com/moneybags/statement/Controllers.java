package com.moneybags.statement;

import com.moneybags.statement.ApiModels.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Set;

@RestController @RequiredArgsConstructor
class StatementController {
    private final StatementService service; private final ActorResolver actors;
    @GetMapping("/api/v1/statements/accounts/{accountId}/mini") MiniStatementView mini(@PathVariable String accountId,@RequestParam(defaultValue="10")int size,HttpServletRequest r){return service.mini(accountId,size,actors.resolve(r));}
    @PostMapping("/api/v1/statements/accounts/{accountId}") ResponseEntity<RequestView> create(@PathVariable String accountId,@Valid @RequestBody StatementRequestBody body,@RequestHeader("Idempotency-Key")String key,HttpServletRequest r){return ResponseEntity.accepted().body(service.create(accountId,body,key,actors.resolve(r)));}
    @GetMapping("/api/v1/statements/requests") PageView<RequestView> list(@PageableDefault(size=25,sort="createdAt",direction=Sort.Direction.DESC)Pageable p,HttpServletRequest r){return service.list(p,actors.resolve(r));}
    @GetMapping("/api/v1/statements/requests/{id}") RequestView get(@PathVariable String id,HttpServletRequest r){return service.get(id,actors.resolve(r));}
    @PostMapping("/api/v1/statements/requests/{id}/cancel") RequestView cancel(@PathVariable String id,HttpServletRequest r){return service.cancel(id,actors.resolve(r));}
    @PostMapping("/api/v1/statements/requests/{id}/download-link") DownloadLink link(@PathVariable String id,HttpServletRequest r){return service.link(id,actors.resolve(r));}
    @GetMapping("/api/v1/statements/files/{fileId}/download") ResponseEntity<byte[]> download(@PathVariable String fileId,@RequestParam String token,HttpServletRequest r){StatementService.DownloadPayload p=service.download(fileId,token,actors.resolve(r),r.getRemoteAddr());return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+p.fileName().replace("\"","")+"\"").body(p.bytes());}
    @GetMapping("/api/v1/statements/download-history") PageView<DownloadView> history(@PageableDefault(size=25,sort="downloadedAt",direction=Sort.Direction.DESC)Pageable p,HttpServletRequest r){return service.history(p,actors.resolve(r));}
}

@RestController @RequestMapping("/api/v1/reports") @RequiredArgsConstructor
class ReportController {
    private final ReportingService reports; private final ActorResolver actors;
    @GetMapping("/daily-transactions") DailyReport daily(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate date,HttpServletRequest r){return reports.daily(date,actors.resolve(r));}
    @GetMapping("/branches/{branchId}/transactions") DailyReport branch(@PathVariable String branchId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate date,HttpServletRequest r){return reports.branch(branchId,date,actors.resolve(r));}
    @GetMapping("/dormant-accounts") PageView<DormantAccount> dormant(@PageableDefault(size=25)Pageable p,HttpServletRequest r){return reports.dormant(p,actors.resolve(r));}
    @GetMapping("/interest-accruals") java.util.List<InterestRow> interest(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,HttpServletRequest r){return reports.interest(from,to,actors.resolve(r));}
    @GetMapping("/reconciliation") ReconciliationReport reconciliation(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate date,HttpServletRequest r){return reports.reconciliation(date,actors.resolve(r));}
}

@RestController @RequiredArgsConstructor
class CertificateController {
    private final ReportingService reports; private final ActorResolver actors;
    @GetMapping("/api/v1/statements/accounts/{accountId}/interest-certificate") ResponseEntity<byte[]> interest(@PathVariable String accountId,@RequestParam int fiscalYear,HttpServletRequest r){return file(reports.certificate(accountId,fiscalYear,false,actors.resolve(r)));}
    @GetMapping("/api/v1/statements/accounts/{accountId}/tds-certificate") ResponseEntity<byte[]> tds(@PathVariable String accountId,@RequestParam int fiscalYear,HttpServletRequest r){return file(reports.certificate(accountId,fiscalYear,true,actors.resolve(r)));}
    private ResponseEntity<byte[]> file(ReportingService.CertificateFile f){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+f.name()+"\"").body(f.bytes());}
}

@RestController @RequestMapping("/api/v1/report-schedules") @RequiredArgsConstructor
class ScheduleController {
    private final ScheduleService schedules; private final ActorResolver actors;
    @PostMapping ResponseEntity<ScheduleView> create(@Valid @RequestBody ScheduleBody b,HttpServletRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(schedules.create(b,actors.resolve(r)));}
    @GetMapping PageView<ScheduleView> list(@PageableDefault(size=25,sort="createdAt",direction=Sort.Direction.DESC)Pageable p,HttpServletRequest r){return schedules.list(p,actors.resolve(r));}
    @GetMapping("/{id}") ScheduleView get(@PathVariable String id,HttpServletRequest r){return schedules.get(id,actors.resolve(r));}
    @PatchMapping("/{id}") ScheduleView patch(@PathVariable String id,@RequestBody SchedulePatch b,HttpServletRequest r){return schedules.patch(id,b,actors.resolve(r));}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable String id,HttpServletRequest r){schedules.delete(id,actors.resolve(r));}
}

@RestController @RequestMapping("/internal/v1/statement-read-model") @RequiredArgsConstructor
class ProjectionController {
    private final ProjectionService projections;
    @PostMapping("/accounts") IngestResult account(@RequestHeader("X-Service-Name")String service,@Valid @RequestBody AccountEvent e){require(service,Set.of("account-service"));return projections.account(e);}
    @PostMapping("/transactions") IngestResult transaction(@RequestHeader("X-Service-Name")String service,@Valid @RequestBody TransactionEvent e){require(service,Set.of("transaction-service","ledger-service"));return projections.transaction(e);}
    private void require(String actual,Set<String> allowed){if(!allowed.contains(actual))throw ApiException.forbidden("SERVICE_AUTH_DENIED","Caller is not authorised for this event type");}
}
