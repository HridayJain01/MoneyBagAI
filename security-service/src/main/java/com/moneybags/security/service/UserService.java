package com.moneybags.security.service;

import com.moneybags.security.dto.UserRequest;
import com.moneybags.security.dto.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse create(UserRequest request);
    UserResponse findById(Long userId);
    List<UserResponse> findAll();
}
