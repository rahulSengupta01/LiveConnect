package com.liveconnect.backend.controller;

import com.liveconnect.backend.dto.response.ConversationResponse;
import com.liveconnect.backend.service.ConversationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(
            ConversationService conversationService) {

        this.conversationService = conversationService;
    }

    @PostMapping("/direct/{userId}")
    public ResponseEntity<ConversationResponse>
    createDirectConversation(
            Authentication authentication,
            @PathVariable String userId) {

        ConversationResponse response =
                conversationService.createDirectConversation(
                        authentication.getName(),
                        userId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}