package com.liveconnect.backend.dto.response;

import com.liveconnect.backend.entity.enums.ConversationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ConversationListResponse {

    private UUID conversationId;

    private ConversationType type;

    private String name;

    private String profilePicture;

    private String lastMessage;

    private String lastMessageSender;

    private LocalDateTime lastMessageTime;

    private LocalDateTime updatedAt;
}