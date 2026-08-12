package com.moneybags.statement;

import com.fasterxml.jackson.databind.*;
import com.moneybags.statement.ApiModels.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Component
class SourceFallbackClient {
    private final RestClient accountClient; private final RestClient transactionClient;
    SourceFallbackClient(StatementProperties p){
        SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(p.getSourceConnectTimeoutMs());f.setReadTimeout(p.getSourceReadTimeoutMs());
        accountClient=RestClient.builder().baseUrl(p.getAccountServiceUrl()).requestFactory(f).build();transactionClient=RestClient.builder().baseUrl(p.getTransactionServiceUrl()).requestFactory(f).build();
    }
    AccountReadModel account(String accountId,Actor a){
        try{JsonNode n=accountClient.get().uri("/internal/v1/accounts/{id}/statement-context",accountId).header("X-Correlation-Id",a.correlationId()).header("X-User-Id",a.userId()).retrieve().body(JsonNode.class);
            return AccountReadModel.builder().accountId(text(n,"accountId",accountId)).customerId(text(n,"customerId",a.cif())).branchId(text(n,"branchId",null))
                    .maskedAccountNumber(text(n,"maskedAccountNumber","****")).accountName(text(n,"accountName","Account")).status(text(n,"status","ACTIVE"))
                    .currency(text(n,"currency","INR")).currentBalance(decimal(n,"currentBalance")).sourceUpdatedAt(Instant.now()).build();
        }catch(RestClientException e){throw ApiException.unavailable("ACCOUNT_SOURCE_UNAVAILABLE","Account source is unavailable");}
    }
    List<TransactionReadModel> transactions(String accountId,LocalDate from,LocalDate to,Actor a){
        try{JsonNode n=transactionClient.get().uri(b->b.path("/api/v1/accounts/{id}/transactions").queryParam("from",from.atStartOfDay(ZoneOffset.UTC).toInstant()).queryParam("to",to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()).queryParam("size",1000).build(accountId))
                    .header("X-Correlation-Id",a.correlationId()).header("X-User-Id",a.userId()).header("X-Customer-Id",Optional.ofNullable(a.cif()).orElse("")).retrieve().body(JsonNode.class);
            JsonNode items=n.has("items")?n.get("items"):n.has("content")?n.get("content"):n;
            List<TransactionReadModel> out=new ArrayList<>();if(items!=null&&items.isArray())for(JsonNode x:items){String source=text(x,"sourceAccountId",null);Direction d=accountId.equals(source)?Direction.DEBIT:Direction.CREDIT;
                out.add(TransactionReadModel.builder().transactionId(text(x,"transactionId",UUID.randomUUID().toString())).transactionReference(text(x,"transactionReference","UNKNOWN"))
                        .accountId(accountId).customerId(text(x,"customerId",a.cif())).branchId(text(x,"branchCode",a.branchId())).direction(d).amount(decimal(x,"amount"))
                        .feeAmount(decimal(x,"feeAmount")).currency(text(x,"currency","INR")).transactionType(text(x,"type","TRANSACTION")).status(text(x,"status","SUCCESS"))
                        .narration(text(x,"narration",null)).reversalOfTransactionId(text(x,"reversalOfTransactionId",null)).postedAt(Instant.parse(text(x,"completedAt",text(x,"createdAt",Instant.now().toString())))).sourceUpdatedAt(Instant.now()).build());}
            return out;
        }catch(Exception e){throw ApiException.unavailable("TRANSACTION_SOURCE_UNAVAILABLE","Transaction and ledger source is unavailable");}
    }
    private static String text(JsonNode n,String f,String d){return n!=null&&n.hasNonNull(f)?n.get(f).asText():d;}
    private static BigDecimal decimal(JsonNode n,String f){return new BigDecimal(text(n,f,"0"));}
}
