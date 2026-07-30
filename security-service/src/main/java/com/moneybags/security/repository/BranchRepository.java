package com.moneybags.security.repository;
import com.moneybags.security.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BranchRepository extends JpaRepository<Branch, String> {}
