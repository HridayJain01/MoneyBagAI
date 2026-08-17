package org.jeffrypatrick.authservice.service;

import org.jeffrypatrick.authservice.dto.AdminCreateUserRequest;
import org.jeffrypatrick.authservice.dto.UpdateUserRoleRequest;
import org.jeffrypatrick.authservice.dto.UserInfoResponse;
import org.jeffrypatrick.authservice.model.Permission;
import org.jeffrypatrick.authservice.model.Role;
import org.jeffrypatrick.authservice.model.RoleName;
import org.jeffrypatrick.authservice.model.Status;
import org.jeffrypatrick.authservice.model.User;
import org.jeffrypatrick.authservice.repository.RoleRepository;
import org.jeffrypatrick.authservice.repository.UserRepository;
import org.jeffrypatrick.authservice.utility.UserUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserInfoResponse createUser(AdminCreateUserRequest request) {
        String normalizedEmail = request.email().toLowerCase();

        if (request.role() == RoleName.ADMIN) {
            throw new IllegalArgumentException("ADMIN cannot be assigned through this endpoint");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.role()));

//        User user = new User();
//        user.setFirstName(request.firstName());
//        user.setLastName(request.lastName());
//        user.setEmail(normalizedEmail);
//        user.setPasswordHash(passwordEncoder.encode(request.password()));
//        user.setDob(request.dob());
//        user.setGender(request.gender());
//        user.setMobile(request.mobile());
//        user.setStatus(Status.ACTIVE);
//        user.setRole(role);


        User saved = userRepository.save(UserUtil.commonCreateUser(
                request.firstName(),
                request.lastName(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.dob(),
                request.gender(),
                request.mobile(),
                Status.ACTIVE,
                role
        ));
        return toUserInfoResponse(saved);
    }

    @Transactional
    public UserInfoResponse changeRole(Long userId, UpdateUserRoleRequest request) {
        if (request.role() == RoleName.ADMIN) {
            throw new IllegalArgumentException("ADMIN cannot be assigned through this endpoint");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.role()));

        user.setRole(role);

        User saved = userRepository.save(user);
        return toUserInfoResponse(saved);
    }

    private UserInfoResponse toUserInfoResponse(User user) {
        Set<String> permissions = user.getRole() != null && user.getRole().getPermissions() != null
                ? user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet())
                : Set.of();

        return new UserInfoResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getDob(),
                user.getGender(),
                user.getMobile(),
                user.getRole() != null ? user.getRole().getName().name() : null,
                permissions,
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}