package org.jeffrypatrick.authservice.service;

import lombok.Data;
import org.jeffrypatrick.authservice.model.Permission;
import org.jeffrypatrick.authservice.model.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;
    private final String status;
    private final Set<String> permissions;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.role = user.getRole() != null ? user.getRole().getName().name() : null;
        this.status = user.getStatus() != null ? user.getStatus().name() : null;
        this.permissions = user.getRole() != null
                ? user.getRole().getPermissions().stream().map(Permission::getName).collect(Collectors.toSet())
                : Set.of();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equalsIgnoreCase(status);
    }

    @Override
    public boolean isEnabled() {
        return !"DISABLED".equalsIgnoreCase(status);
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return Set.of();
        return Set.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @NullMarked
    public String getUsername() {
        return email;
    }
}
