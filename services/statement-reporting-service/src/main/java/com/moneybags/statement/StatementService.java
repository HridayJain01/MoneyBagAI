package com.moneybags.statement;

import com.moneybags.statement.ApiModels.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
class StatementService {
    private final AccountReadRepository accounts; private final TransactionReadRepository transactions; private final StatementRequestRepository requests;
    private final GeneratedFileRepository files; private final DownloadHistoryRepository downloads;
    private final FileStorageService storage; private final ExportService exporter; private final StatementProperties properties;

    @Transactional(readOnly=true) MiniStatementView mini(String accountId,int size,Actor actor){
        actor.require("STATEMENT_VIEW");AccountReadModel a=account(accountId,actor);List<TransactionReadModel> tx=transactions.findTop100ByAccountIdOrderByPostedAtDesc(accountId);
        List<EntryView> entries=expand(tx.stream().limit(Math.min(Math.max(size,1),100)).sorted(Comparator.comparing(x->x.postedAt)).toList(),null,a.currency);Collections.reverse(entries);
        return new MiniStatementView(accountId,a.maskedAccountNumber,a.accountName,Money.of(a.currentBalance,a.currency),entries.stream().limit(size).toList());
    }

    @Transactional RequestView create(String accountId,StatementRequestBody body,String key,Actor actor){
        actor.require("STATEMENT_VIEW");if(key==null||key.isBlank())throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,"IDEMPOTENCY_KEY_REQUIRED","Idempotency-Key is required");validateDates(body.fromDate(),body.toDate());account(accountId,actor);
        String hash=sha(accountId+"|"+body.fromDate()+"|"+body.toDate()+"|"+body.outputFormat()+"|"+body.statementKind());String ref="ST-"+sha(actor.userId()+"|"+key);
        Optional<StatementRequestEntity> old=requests.findByRequestRef(ref);if(old.isPresent()){if(!old.get().requestHash.equals(hash))throw ApiException.conflict("IDEMPOTENCY_CONFLICT","Idempotency-Key was reused for a different request");return view(old.get());}
        StatementKind kind=body.statementKind()==null?StatementKind.DATE_RANGE:body.statementKind();StatementRequestEntity e=StatementRequestEntity.builder().requestRef(ref).requestHash(hash).accountId(accountId)
                .requestedByUserId(actor.userId()).requestedByCif(actor.cif()).requesterBranchId(actor.branchId()).fromDate(body.fromDate()).toDate(body.toDate())
                .outputFormat(body.outputFormat()).statementKind(kind).status(RequestStatus.PENDING).build();return view(requests.save(e));
    }
    @Transactional(readOnly=true) PageView<RequestView> list(Pageable page,Actor actor){Page<StatementRequestEntity> p=actor.staff()&&actor.branchId()!=null?requests.findByRequesterBranchId(actor.branchId(),page):requests.findByRequestedByUserId(actor.userId(),page);return page(p.map(this::view));}
    @Transactional(readOnly=true) RequestView get(String id,Actor actor){return view(owned(id,actor));}
    @Transactional RequestView cancel(String id,Actor actor){StatementRequestEntity e=owned(id,actor);if(e.status!=RequestStatus.PENDING)throw ApiException.conflict("INVALID_STATEMENT_STATE","Only PENDING requests can be cancelled");e.status=RequestStatus.CANCELLED;return view(e);}

    @Scheduled(fixedDelayString="${moneybags.statement.worker-delay-ms:2000}")
    @Transactional public void generateNext(){if(!properties.isWorkerEnabled())return;requests.findFirstByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).ifPresent(this::generate);}
    @Transactional void generateNow(String id){StatementRequestEntity e=requests.findById(id).orElseThrow(()->ApiException.notFound("STATEMENT_NOT_FOUND","Statement request not found"));if(e.status!=RequestStatus.PENDING)throw ApiException.conflict("INVALID_STATEMENT_STATE","Request is not pending");generate(e);}
    private void generate(StatementRequestEntity e){
        e.status=RequestStatus.GENERATING;e.sourceSnapshotAt=Instant.now();requests.saveAndFlush(e);
        try{Actor sourceActor=new Actor(e.requestedByUserId,e.requestedByCif,e.requesterBranchId==null?null:"scheduled-staff",e.requesterBranchId,Set.of("STATEMENT_VIEW"),UUID.randomUUID().toString());AccountReadModel a=account(e.accountId,sourceActor);
            Instant from=e.fromDate.atStartOfDay(ZoneOffset.UTC).toInstant(),end=e.toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            List<TransactionReadModel> range=transactions.findByAccountIdAndPostedAtGreaterThanEqualAndPostedAtLessThanAndPostedAtLessThanEqualOrderByPostedAtAsc(e.accountId,from,end,e.sourceSnapshotAt);
            List<TransactionReadModel> allSince=transactions.findByAccountIdAndPostedAtGreaterThanEqualAndPostedAtLessThanAndPostedAtLessThanEqualOrderByPostedAtAsc(e.accountId,from,e.sourceSnapshotAt.plusMillis(1),e.sourceSnapshotAt);
            BigDecimal opening=a.currentBalance.subtract(net(allSince));List<EntryView> entries=expand(range,opening,a.currency);BigDecimal closing=opening.add(net(range));
            if(!range.isEmpty()&&range.get(range.size()-1).balanceAfter!=null&&range.get(range.size()-1).balanceAfter.compareTo(closing)!=0)
                throw ApiException.unavailable("SOURCE_BALANCE_MISMATCH","Calculated closing balance does not match the source balance-as-of");
            StatementDocument doc=new StatementDocument(e.accountId,a.maskedAccountNumber,a.accountName,a.currency,e.fromDate,e.toDate,opening,closing,entries);ExportedFile out=exporter.statement(doc,e.outputFormat);
            String key="statements/"+e.id+"/statement."+out.extension();FileStorageService.Stored stored=storage.store(key,out.bytes());GeneratedFileEntity f=GeneratedFileEntity.builder().request(e).storageKey(key).contentType(out.contentType()).fileName("statement-"+e.accountId+"-"+e.fromDate+"-"+e.toDate+"."+out.extension()).fileSizeBytes(stored.size()).checksumSha256(stored.checksum()).expiresAt(Instant.now().plus(Duration.ofDays(properties.getFileRetentionDays()))).build();files.save(f);e.status=RequestStatus.READY;
        }catch(Exception x){e.status=RequestStatus.FAILED;e.safeErrorCode=x instanceof ApiException a?a.code:"GENERATION_FAILED";e.safeErrorMessage=x instanceof ApiException?x.getMessage():"Statement generation failed safely";}
    }

    @Transactional(readOnly=true) DownloadLink link(String requestId,Actor actor){StatementRequestEntity r=owned(requestId,actor);if(r.status!=RequestStatus.READY)throw ApiException.conflict("STATEMENT_NOT_READY","Statement is not ready");GeneratedFileEntity f=files.findByRequestId(requestId).orElseThrow(()->ApiException.notFound("FILE_NOT_FOUND","Generated file not found"));Instant expiry=Instant.now().plus(Duration.ofMinutes(properties.getDownloadLinkMinutes()));String raw=f.id+":"+actor.userId()+":"+expiry.getEpochSecond();return new DownloadLink("/api/v1/statements/files/"+f.id+"/download?token="+Base64.getUrlEncoder().withoutPadding().encodeToString((raw+":"+hmac(raw)).getBytes(StandardCharsets.UTF_8)),expiry);}
    @Transactional DownloadPayload download(String fileId,String token,Actor actor,String ip){GeneratedFileEntity f=files.findById(fileId).orElseThrow(()->ApiException.notFound("FILE_NOT_FOUND","Generated file not found"));StatementRequestEntity r=f.request;authorize(r,actor);String reason=null;try{String decoded=new String(Base64.getUrlDecoder().decode(token),StandardCharsets.UTF_8);String[] p=decoded.split(":",4);String raw=p[0]+":"+p[1]+":"+p[2];if(p.length!=4||!fileId.equals(p[0])||!actor.userId().equals(p[1])||!MessageDigest.isEqual(hmac(raw).getBytes(StandardCharsets.UTF_8),p[3].getBytes(StandardCharsets.UTF_8)))reason="INVALID_TOKEN";else if(Instant.now().isAfter(Instant.ofEpochSecond(Long.parseLong(p[2]))))reason="TOKEN_EXPIRED";else if(Instant.now().isAfter(f.expiresAt))reason="FILE_EXPIRED";}catch(Exception x){reason="INVALID_TOKEN";}
        if(reason!=null){downloads.save(history(r,f,actor,ip,reason.contains("EXPIRED")?"EXPIRED":"DENIED",reason));throw ApiException.forbidden(reason,"Download token is invalid or expired");}byte[] bytes=storage.read(f.storageKey);downloads.save(history(r,f,actor,ip,"SUCCESS",null));return new DownloadPayload(bytes,f.contentType,f.fileName);
    }
    @Transactional(readOnly=true) PageView<DownloadView> history(Pageable page,Actor actor){Page<DownloadHistoryEntity> p=downloads.findByDownloadedByUserId(actor.userId(),page);return page(p.map(d->new DownloadView(d.id,d.request.id,d.file.id,d.downloadedByUserId,d.outcome,d.reasonCode,d.downloadedAt)));}
    record DownloadPayload(byte[] bytes,String contentType,String fileName) {}

    private AccountReadModel account(String id,Actor actor){AccountReadModel a=accounts.findById(id).orElseThrow(()->ApiException.unavailable("ACCOUNT_PROJECTION_NOT_FOUND","Account projection is not available yet"));if(actor.cif()!=null&&!actor.cif().equals(a.customerId))throw ApiException.forbidden("ACCOUNT_SCOPE_DENIED","Account is outside customer scope");if(actor.staff()&&actor.branchId()!=null&&!actor.branchId().equals(a.branchId))throw ApiException.forbidden("BRANCH_SCOPE_DENIED","Account is outside branch scope");return a;}
    private StatementRequestEntity owned(String id,Actor a){StatementRequestEntity r=requests.findById(id).orElseThrow(()->ApiException.notFound("STATEMENT_NOT_FOUND","Statement request not found"));authorize(r,a);return r;}
    private void authorize(StatementRequestEntity r,Actor a){if(!r.requestedByUserId.equals(a.userId())&&!(a.staff()&&a.branchId()!=null&&a.branchId().equals(r.requesterBranchId)))throw ApiException.forbidden("STATEMENT_SCOPE_DENIED","Statement is outside caller scope");}
    private RequestView view(StatementRequestEntity e){GeneratedFileEntity f=files.findByRequestId(e.id).orElse(null);FileView fv=f==null?null:new FileView(f.id,f.fileName,f.contentType,f.fileSizeBytes,f.checksumSha256,f.expiresAt);return new RequestView(e.id,e.requestRef,e.accountId,e.fromDate,e.toDate,e.outputFormat,e.statementKind,e.status,e.sourceSnapshotAt,e.safeErrorCode,e.safeErrorMessage,fv,e.createdAt,e.updatedAt);}
    private List<EntryView> expand(List<TransactionReadModel> rows,BigDecimal opening,String currency){List<EntryView> out=new ArrayList<>();BigDecimal running=opening;for(TransactionReadModel t:rows){if(running!=null)running=running.add(t.direction==Direction.CREDIT?t.amount:t.amount.negate());out.add(entry(t,t.transactionReference,t.transactionType,t.amount,running));if(t.feeAmount!=null&&t.feeAmount.signum()>0){running=running==null?null:running.subtract(t.feeAmount);out.add(new EntryView(t.transactionId,t.ledgerEntryId,t.transactionReference+"-FEE",t.postedAt,"FEE",Direction.DEBIT,"Transaction fee",Money.of(t.feeAmount,currency),running==null?null:Money.of(running,currency),t.reversalOfTransactionId));}}return out;}
    private EntryView entry(TransactionReadModel t,String ref,String type,BigDecimal amount,BigDecimal balance){return new EntryView(t.transactionId,t.ledgerEntryId,ref,t.postedAt,type,t.direction,t.narration,Money.of(amount,t.currency),balance==null?(t.balanceAfter==null?null:Money.of(t.balanceAfter,t.currency)):Money.of(balance,t.currency),t.reversalOfTransactionId);}
    private BigDecimal net(List<TransactionReadModel> rows){return rows.stream().map(t->{BigDecimal signed=t.direction==Direction.CREDIT?t.amount:t.amount.negate();if(t.feeAmount!=null&&t.feeAmount.signum()>0)signed=signed.subtract(t.feeAmount);return signed;}).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private DownloadHistoryEntity history(StatementRequestEntity r,GeneratedFileEntity f,Actor a,String ip,String outcome,String reason){return DownloadHistoryEntity.builder().request(r).file(f).downloadedByUserId(a.userId()).sourceIp(ip).outcome(outcome).reasonCode(reason).build();}
    private void validateDates(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||from.plusYears(1).plusDays(5).isBefore(to))throw ApiException.invalid("INVALID_DATE_RANGE","Date range must be ordered and at most one year");}
    private String sha(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String hmac(String v){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(properties.getDownloadSecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(m.doFinal(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private <T> PageView<T> page(Page<T> p){return new PageView<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
}
