package com.moneybags.account.repository;

import com.moneybags.account.entity.LinkedCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LinkedCardRepository extends JpaRepository<LinkedCard, String> {
    List<LinkedCard> findByAccountId(String accountId);
}
