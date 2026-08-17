package org.jeffrypatrick.authservice.service;

import org.jeffrypatrick.authservice.dto.UserInfoResponse;
import org.jeffrypatrick.authservice.model.Permission;
import org.jeffrypatrick.authservice.model.User;
import org.jeffrypatrick.authservice.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUserInfo(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserInfoResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getDob(),
                user.getGender(),
                user.getMobile(),
                user.getRole() != null ? user.getRole().getName().name() : null,
                user.getRole() != null
                        ? user.getRole().getPermissions()
                                        .stream()
                                        .map(Permission::getName)
                                        .collect(java.util.stream.Collectors.toSet())
                        : java.util.Set.of(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}