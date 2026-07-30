package com.moneybags.account.service;
import com.moneybags.account.dto.*;
import java.util.List;
public interface AccountService {
    AccountResponse create(AccountRequest request);
    AccountResponse findByNumber(String accountNo);
    List<AccountResponse> findByCif(String cifNo);
}
