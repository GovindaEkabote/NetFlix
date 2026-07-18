package com.user.service.impl;

import com.user.dto.request.LoginRequest;
import com.user.dto.request.RegisterRequest;
import com.user.dto.response.AuthResponse;
import com.user.exception.AuthenticationException;
import com.user.exception.ResourceNotFoundException;
import com.user.mapper.UserMapper;
import com.user.model.RefreshToken;
import com.user.model.User;
import com.user.repository.RefreshTokenRepository;
import com.user.repository.UserRepository;
import com.user.service.AuthService;
import com.user.security.jwt.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request);

        if (userRepository.existsByEmail(request.getEmail())){
            throw new AuthenticationException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())){
            throw new AuthenticationException("Phone number already exists");
        }
        User user= User.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        log.info("User registered successfully: {}", user);
        return generateAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Logging in user: {}", request);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!user.isEnabled()) {
                throw new AuthenticationException("Account is disabled");
            }

            log.info("User logged in successfully: {}", user.getEmail());
            return generateAuthResponse(user);
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getEmail(), e);
            throw new AuthenticationException("Invalid email or password");
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");
        RefreshToken  token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if(token.isExpired()){
            refreshTokenRepository.delete(token);
            throw new AuthenticationException("Refresh token expired");
        }

        User user = token.getUser();
        String newAccessToken  = jwtService.generateToken(user);
        String newRefreshToken  = jwtService.generateRefreshToken(user);

        refreshTokenRepository.delete(token);

        // Save new refresh token
        RefreshToken newToken = RefreshToken.builder()
                .token(newRefreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(15))
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(newToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        log.info("Logging out user");

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElse(null);

        if(token != null) {
            refreshTokenRepository.delete(token);
            log.info("Refresh token deleted for user: {}", token.getUser().getEmail());
        }

        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }


   private AuthResponse generateAuthResponse(User user){
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

       RefreshToken token = RefreshToken.builder()
               .token(refreshToken)
               .user(user)
               .expiryDate(LocalDateTime.now().plusMinutes(15))
               .createdAt(LocalDateTime.now())
               .build();
        refreshTokenRepository.save(token);
        log.info("Generated auth response for user: {}", user);
       return AuthResponse.builder()
               .accessToken(accessToken)
               .refreshToken(refreshToken)
               .tokenType("Bearer")
               .expiresIn(86400000L)
               .user(userMapper.toResponse(user))
               .build();
    }
}
