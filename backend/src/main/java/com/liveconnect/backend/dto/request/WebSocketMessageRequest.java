package com.liveconnect.backend.dto.request;

import com.liveconnect.backend.entity.enums.MessageType;
import lombok.Data;

import java.util.UUID;

@Data
public class WebSocketMessageRequest {

    private UUID conversationId;

    private String content;

    private MessageType messageType;
}