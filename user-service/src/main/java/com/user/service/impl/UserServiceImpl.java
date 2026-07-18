package com.user.service.impl;

import com.user.dto.request.UpdateProfileRequest;
import com.user.dto.response.UserResponse;
import com.user.exception.AuthenticationException;
import com.user.exception.ResourceNotFoundException;
import com.user.mapper.UserMapper;
import com.user.model.User;
import com.user.repository.UserRepository;
import com.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getProfile(Long userId) {
        User user = getUserByIdEntity(userId);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserByIdEntity(userId);

        if (request.getName() != null){
            user.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AuthenticationException("Email already taken");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new AuthenticationException("Phone number already taken");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getProfileImage() != null) {
            user.setProfileImage(request.getProfileImage());
        }

        user = userRepository.save(user);
        log.info("User updated successfully");
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = getUserByIdEntity(userId);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Override
    public void deleteUser(Long userId) {
        User user = getUserByIdEntity(userId);
        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private User getUserByIdEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
