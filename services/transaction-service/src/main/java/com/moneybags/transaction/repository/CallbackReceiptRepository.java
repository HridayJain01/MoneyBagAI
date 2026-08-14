package com.moneybags.transaction.repository;
import com.moneybags.transaction.entity.CallbackReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CallbackReceiptRepository extends JpaRepository<CallbackReceipt,String>{ Optional<CallbackReceipt> findByCallbackTypeAndProviderEventId(String type,String providerEventId); }
