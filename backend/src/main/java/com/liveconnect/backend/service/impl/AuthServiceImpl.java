package com.liveconnect.backend.service.impl;

import com.liveconnect.backend.dto.request.LoginRequest;
import com.liveconnect.backend.dto.request.RegisterRequest;
import com.liveconnect.backend.dto.response.LoginResponse;
import com.liveconnect.backend.dto.response.RegisterResponse;
import com.liveconnect.backend.entity.User;
import com.liveconnect.backend.entity.enums.UserStatus;
import com.liveconnect.backend.exception.EmailAlreadyExistsException;
import com.liveconnect.backend.exception.UsernameAlreadyExistsException;
import com.liveconnect.backend.repository.UserRepository;
import com.liveconnect.backend.security.JwtService;
import com.liveconnect.backend.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
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
    @Override
    public LoginResponse login(LoginRequest request) {
        

        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password")
                );

        // 2. Check password
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        // 3. Invalid password
        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getUsername());
        // 4. Return login response
        return LoginResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .accessToken(token)
        .tokenType("Bearer")
        .message("Login successful")
        .build();
    }
}