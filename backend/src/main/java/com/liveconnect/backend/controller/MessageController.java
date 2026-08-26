package com.liveconnect.backend.controller;

import com.liveconnect.backend.dto.request.SendMessageRequest;
import com.liveconnect.backend.dto.response.MessageResponse;
import com.liveconnect.backend.service.MessageService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(
            MessageService messageService) {

        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication,
            @Valid @RequestBody SendMessageRequest request) {

        MessageResponse response =
                messageService.sendMessage(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable String conversationId,
            Pageable pageable) {

        Page<MessageResponse> response =
                messageService.getMessages(
                        authentication.getName(),
                        conversationId,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}