package com.liveconnect.backend.controller;

import com.liveconnect.backend.dto.request.SendMessageRequest;
import com.liveconnect.backend.dto.request.WebSocketMessageRequest;
import com.liveconnect.backend.dto.response.MessageResponse;
import com.liveconnect.backend.service.MessageService;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;

@Controller
public class WebSocketChatController {

    private final MessageService messageService;

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate) {

        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
public void sendMessage(
        WebSocketMessageRequest request,
        StompHeaderAccessor accessor) {

    Authentication authentication =
            (Authentication) accessor.getUser();

    if (authentication == null) {
        throw new IllegalStateException(
                "WebSocket user is not authenticated"
        );
    }

    String username =
            authentication.getName();

    System.out.println(
            "WebSocket message from user: "
                    + username
    );

    SendMessageRequest messageRequest =
            new SendMessageRequest();

    messageRequest.setConversationId(
            request.getConversationId()
    );

    messageRequest.setContent(
            request.getContent()
    );

    messageRequest.setMessageType(
            request.getMessageType()
    );

    MessageResponse response =
            messageService.sendMessage(
                    username,
                    messageRequest
            );

    String destination =
            "/topic/conversation/"
                    + request.getConversationId();

    messagingTemplate.convertAndSend(
            destination,
            response
    );
}
}