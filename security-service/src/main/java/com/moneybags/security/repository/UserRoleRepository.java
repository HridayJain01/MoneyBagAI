package com.moneybags.security.repository;
import com.moneybags.security.entity.UserRole;
import com.moneybags.security.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {}
