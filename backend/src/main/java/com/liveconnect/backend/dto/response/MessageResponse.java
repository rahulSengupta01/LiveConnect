package com.liveconnect.backend.dto.response;

import com.liveconnect.backend.entity.enums.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {

    private UUID id;

    private UUID conversationId;

    private String senderId;

    private String senderUsername;

    private String content;

    private MessageType messageType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}