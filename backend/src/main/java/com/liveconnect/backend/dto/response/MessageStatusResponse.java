package com.liveconnect.backend.dto.response;

import com.liveconnect.backend.entity.enums.MessageReceiptStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageStatusResponse {

    private UUID messageId;
    private UUID conversationId;

    private String recipientUsername;
    private String senderUsername;

    private MessageReceiptStatus status;

    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
}