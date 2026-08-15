package com.liveconnect.backend.service;

import com.liveconnect.backend.dto.response.UserResponse;

public interface UserService {

    UserResponse getCurrentUser(String username);
}