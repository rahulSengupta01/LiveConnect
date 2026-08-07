package com.liveconnect.backend.service;

import com.liveconnect.backend.dto.request.RegisterRequest;
import com.liveconnect.backend.dto.response.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

}