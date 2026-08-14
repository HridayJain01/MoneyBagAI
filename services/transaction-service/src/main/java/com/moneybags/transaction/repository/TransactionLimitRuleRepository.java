package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.TransactionLimitRule;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import java.util.List;
public interface TransactionLimitRuleRepository extends JpaRepository<TransactionLimitRule,String>{
 @Query("select r from TransactionLimitRule r where r.active=true and r.currency=:currency and r.effectiveFrom<=:now and (r.effectiveTo is null or r.effectiveTo>:now) order by r.priority desc")
 List<TransactionLimitRule> findActive(String currency, Instant now);
}
