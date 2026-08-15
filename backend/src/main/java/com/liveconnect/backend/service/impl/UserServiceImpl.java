package com.liveconnect.backend.service.impl;

import com.liveconnect.backend.dto.response.UserResponse;
import com.liveconnect.backend.entity.User;
import com.liveconnect.backend.repository.UserRepository;
import com.liveconnect.backend.service.UserService;

import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getCurrentUser(String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .status(user.getStatus().toString())
                .build();
    }
}