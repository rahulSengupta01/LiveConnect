package com.liveconnect.backend.service;

import com.liveconnect.backend.dto.request.SendMessageRequest;
import com.liveconnect.backend.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    MessageResponse sendMessage(
            String username,
            SendMessageRequest request
    );

    Page<MessageResponse> getMessages(
            String username,
            String conversationId,
            Pageable pageable
    );
}