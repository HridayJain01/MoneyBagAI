package com.moneybags.security.service.impl;

import com.moneybags.security.dto.*;
import com.moneybags.security.exception.*;
import com.moneybags.security.mapper.UserMapper;
import com.moneybags.security.repository.UserRepository;
import com.moneybags.security.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final UserMapper mapper;

    @Override @Transactional
    public UserResponse create(UserRequest request) {
        if (repository.findByUsername(request.username()).isPresent() || repository.existsByEmail(request.email())) {
            throw new ConflictException("Username or email already exists");
        }
        // TODO hash raw passwords in the authentication use case; this scaffold accepts an already-hashed value.
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    @Override
    public UserResponse findById(Long userId) {
        return mapper.toResponse(repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId)));
    }

    @Override
    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }
}
