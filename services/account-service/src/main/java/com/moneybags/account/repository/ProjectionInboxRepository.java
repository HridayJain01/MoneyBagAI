package com.moneybags.account.repository;

import com.moneybags.account.entity.ProjectionInbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectionInboxRepository extends JpaRepository<ProjectionInbox, String> {
    Optional<ProjectionInbox> findByDedupKey(String dedupKey);
}
