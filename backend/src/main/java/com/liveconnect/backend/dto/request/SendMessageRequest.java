package com.liveconnect.backend.dto.request;

import com.liveconnect.backend.entity.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SendMessageRequest {

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotBlank(message = "Message content cannot be empty")
    private String content;

    @NotNull(message = "Message type is required")
    private MessageType messageType;
}