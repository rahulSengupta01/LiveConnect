package com.liveconnect.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private String id;
    private String username;
    private String email;

    private String firstName;
    private String lastName;

    private String profilePicture;
    private String bio;

    private String status;
}