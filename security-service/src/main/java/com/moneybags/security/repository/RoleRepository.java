package com.moneybags.security.repository;
import com.moneybags.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoleRepository extends JpaRepository<Role, Long> {}
