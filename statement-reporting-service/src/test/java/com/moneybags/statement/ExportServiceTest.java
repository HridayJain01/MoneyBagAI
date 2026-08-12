package com.moneybags.statement;

import com.moneybags.statement.ApiModels.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import static org.assertj.core.api.Assertions.assertThat;

class ExportServiceTest {
    private final ExportService service=new ExportService();
    private final StatementDocument document=new StatementDocument("a1","XXXX1234","Savings","INR",LocalDate.of(2026,8,1),LocalDate.of(2026,8,31),new BigDecimal("750"),new BigDecimal("1250"),List.of(new EntryView("t1","l1","r1",Instant.parse("2026-08-01T10:00:00Z"),"DEPOSIT",Direction.CREDIT,"deposit",Money.of(new BigDecimal("500"),"INR"),Money.of(new BigDecimal("1250"),"INR"),null)));
    @Test void exportsValidContainerSignaturesAndEquivalentRow(){
        ExportedFile pdf=service.statement(document,OutputFormat.PDF),csv=service.statement(document,OutputFormat.CSV),xlsx=service.statement(document,OutputFormat.XLSX);
        assertThat(new String(pdf.bytes(),0,5,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(new String(csv.bytes(),java.nio.charset.StandardCharsets.UTF_8)).contains("r1").contains("500.00");
        Set<String> names=new HashSet<>();try(ZipInputStream z=new ZipInputStream(new java.io.ByteArrayInputStream(xlsx.bytes()))){for(var e=z.getNextEntry();e!=null;e=z.getNextEntry())names.add(e.getName());}catch(Exception e){throw new AssertionError(e);}
        assertThat(names).contains("xl/workbook.xml","xl/worksheets/sheet1.xml");
    }
}
