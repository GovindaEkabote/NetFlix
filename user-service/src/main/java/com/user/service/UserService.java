package com.user.service;

import com.user.dto.request.UpdateProfileRequest;
import com.user.dto.response.UserResponse;

public interface UserService {

    UserResponse getProfile(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    UserResponse getUserById(Long userId);
    UserResponse getUserByEmail(String email);
    void deleteUser(Long userId);
    boolean existsByEmail(String email);
}
