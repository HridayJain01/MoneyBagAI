package org.jeffrypatrick.authservice.repository;

import org.jeffrypatrick.authservice.model.Role;
import org.jeffrypatrick.authservice.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
