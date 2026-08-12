package com.moneybags.statement;

import com.moneybags.statement.ApiModels.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service @RequiredArgsConstructor
class ScheduleService {
    private final ReportScheduleRepository schedules; private final StatementService statements;
    @Transactional ScheduleView create(ScheduleBody b,Actor a){a.require("STATEMENT_VIEW");if(b.reportType().equalsIgnoreCase("STATEMENT")&&(b.accountId()==null||b.accountId().isBlank()))throw ApiException.invalid("ACCOUNT_REQUIRED","Scheduled statements require accountId");return view(schedules.save(ReportScheduleEntity.builder().ownerUserId(a.userId()).ownerCif(a.cif()).branchId(a.branchId()).accountId(b.accountId()).reportType(b.reportType().toUpperCase()).outputFormat(b.outputFormat()).frequency(b.frequency()).nextRunAt(b.nextRunAt()).active(true).build()));}
    @Transactional(readOnly=true) PageView<ScheduleView> list(Pageable page,Actor a){Page<ReportScheduleEntity> p=a.permissions().contains("REPORT_ADMIN")?schedules.findAll(page):schedules.findByOwnerUserId(a.userId(),page);return new PageView<>(p.map(this::view).getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());}
    @Transactional(readOnly=true) ScheduleView get(String id,Actor a){return view(owned(id,a));}
    @Transactional ScheduleView patch(String id,SchedulePatch b,Actor a){ReportScheduleEntity e=owned(id,a);if(b.outputFormat()!=null)e.outputFormat=b.outputFormat();if(b.frequency()!=null)e.frequency=b.frequency();if(b.nextRunAt()!=null)e.nextRunAt=b.nextRunAt();if(b.active()!=null)e.active=b.active();return view(e);}
    @Transactional void delete(String id,Actor a){owned(id,a).active=false;}
    @Scheduled(fixedDelayString="${moneybags.statement.worker-delay-ms:2000}") @Transactional public void runDue(){for(ReportScheduleEntity e:schedules.findTop25ByActiveTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(Instant.now())){if("STATEMENT".equals(e.reportType)){LocalDate to=LocalDate.now(ZoneOffset.UTC),from=switch(e.frequency){case DAILY->to;case WEEKLY->to.minusDays(6);case MONTHLY->to.withDayOfMonth(1);case YEARLY->to.withDayOfYear(1);};Actor a=new Actor(e.ownerUserId,e.ownerCif,e.branchId==null?null:"scheduled-staff",e.branchId,java.util.Set.of("STATEMENT_VIEW"),java.util.UUID.randomUUID().toString());statements.create(e.accountId,new StatementRequestBody(from,to,e.outputFormat,StatementKind.DATE_RANGE),"schedule:"+e.id+":"+e.nextRunAt,a);}e.lastRunAt=Instant.now();e.nextRunAt=next(e.nextRunAt,e.frequency);}}
    private Instant next(Instant i,Frequency f){return switch(f){case DAILY->i.plus(1,java.time.temporal.ChronoUnit.DAYS);case WEEKLY->i.plus(7,java.time.temporal.ChronoUnit.DAYS);case MONTHLY->i.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();case YEARLY->i.atZone(ZoneOffset.UTC).plusYears(1).toInstant();};}
    private ReportScheduleEntity owned(String id,Actor a){ReportScheduleEntity e=schedules.findById(id).orElseThrow(()->ApiException.notFound("SCHEDULE_NOT_FOUND","Report schedule not found"));if(!e.ownerUserId.equals(a.userId())&&!a.permissions().contains("REPORT_ADMIN"))throw ApiException.forbidden("SCHEDULE_SCOPE_DENIED","Schedule is outside caller scope");return e;}
    private ScheduleView view(ReportScheduleEntity e){return new ScheduleView(e.id,e.ownerUserId,e.accountId,e.reportType,e.outputFormat,e.frequency,e.nextRunAt,e.active,e.lastRunAt,e.createdAt,e.updatedAt);}
}
