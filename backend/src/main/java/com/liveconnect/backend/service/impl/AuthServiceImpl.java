package com.liveconnect.backend.service.impl;

import com.liveconnect.backend.dto.request.RegisterRequest;
import com.liveconnect.backend.dto.response.RegisterResponse;
import com.liveconnect.backend.entity.User;
import com.liveconnect.backend.entity.enums.UserStatus;
import com.liveconnect.backend.repository.UserRepository;
import com.liveconnect.backend.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Encrypt Password
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Create User Entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.OFFLINE)
                .emailVerified(false)
                .build();

        // Save User
        User savedUser = userRepository.save(user);

        // Return Response
        return RegisterResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .message("User registered successfully")
                .build();
    }
}