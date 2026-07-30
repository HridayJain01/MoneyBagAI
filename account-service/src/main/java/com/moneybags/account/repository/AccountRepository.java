package com.moneybags.account.repository;
import com.moneybags.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByCifNo(String cifNo);
}
