package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord,String>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from IdempotencyRecord i where i.callerScope=:scope and i.operation=:operation and i.key=:key")
 Optional<IdempotencyRecord> findLocked(String scope,String operation,String key);
}
