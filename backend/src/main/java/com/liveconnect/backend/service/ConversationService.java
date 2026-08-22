package com.liveconnect.backend.service;

import com.liveconnect.backend.dto.response.ConversationResponse;

public interface ConversationService {

    ConversationResponse createDirectConversation(
            String username,
            String otherUserId
    );
}