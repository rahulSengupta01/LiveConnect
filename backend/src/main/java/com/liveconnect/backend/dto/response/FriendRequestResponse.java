package com.liveconnect.backend.dto.response;

import com.liveconnect.backend.entity.FriendRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FriendRequestResponse {

    private UUID id;

    private String senderId;
    private String senderUsername;

    private String receiverId;
    private String receiverUsername;

    private FriendRequestStatus status;
}