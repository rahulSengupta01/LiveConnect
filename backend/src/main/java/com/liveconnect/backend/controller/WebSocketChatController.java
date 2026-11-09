
package com.liveconnect.backend.controller;

import com.liveconnect.backend.dto.request.MessageDeliveryRequest;
import com.liveconnect.backend.dto.request.MessageReadRequest;
import com.liveconnect.backend.dto.request.SendMessageRequest;
import com.liveconnect.backend.dto.request.WebSocketMessageRequest;
import com.liveconnect.backend.dto.response.MessageResponse;
import com.liveconnect.backend.dto.response.MessageStatusResponse;
import com.liveconnect.backend.repository.ConversationMemberRepository;
import com.liveconnect.backend.service.MessageService;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class WebSocketChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationMemberRepository conversationMemberRepository;

    public WebSocketChatController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate,
            ConversationMemberRepository conversationMemberRepository) {

        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.conversationMemberRepository = conversationMemberRepository;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(
            WebSocketMessageRequest request,
            StompHeaderAccessor accessor) {

        // 1. Get authenticated user
        Authentication authentication =
                (Authentication) accessor.getUser();

        if (authentication == null) {
            throw new IllegalStateException(
                    "WebSocket user is not authenticated"
            );
        }

        String username = authentication.getName();

        UUID conversationId = request.getConversationId();

        System.out.println(
                "WebSocket message from user: " + username
        );

        // 2. Prepare message request
        SendMessageRequest messageRequest =
                new SendMessageRequest();

        messageRequest.setConversationId(conversationId);
        messageRequest.setContent(request.getContent());
        messageRequest.setMessageType(request.getMessageType());

        // 3. Save message through existing service
        MessageResponse response =
                messageService.sendMessage(
                        username,
                        messageRequest
                );

        // 4. Send to users currently viewing this conversation
        String conversationDestination =
                "/topic/conversation/" + conversationId;

        messagingTemplate.convertAndSend(
                conversationDestination,
                response
        );

        // 5. Find every participant in this conversation
        List<String> participants =
                conversationMemberRepository
                        .findUsernamesByConversationId(conversationId);

        // 6. Send a personal sidebar update to every participant
        for (String participantUsername : participants) {

            messagingTemplate.convertAndSendToUser(
                    participantUsername,
                    "/queue/conversation-updates",
                    response
            );

            System.out.println(
                    "Conversation update sent to: "
                            + participantUsername
            );
        }
    }
        @MessageMapping("/chat.delivered")
        public void markMessageDelivered(
                MessageDeliveryRequest request,
                StompHeaderAccessor accessor) {

        // 1. Get authenticated recipient
        Authentication authentication =
                (Authentication) accessor.getUser();

        if (authentication == null) {
                throw new IllegalStateException(
                        "WebSocket user is not authenticated"
                );
        }

        String username = authentication.getName();

        // 2. Validate request
        if (request.getMessageId() == null) {
                throw new IllegalArgumentException(
                        "Message ID is required"
                );
        }

        // 3. Update receipt through service
        MessageStatusResponse response =
                messageService.markMessageDelivered(
                        username,
                        request.getMessageId()
                );

        // 4. Notify original sender privately
        messagingTemplate.convertAndSendToUser(
                response.getSenderUsername(),
                "/queue/message-status",
                response
        );

        System.out.println(
                "Message " + response.getMessageId()
                        + " delivered to " + username
        );
        }
        @MessageMapping("/chat.read")
public void markConversationAsRead(
        MessageReadRequest request,
        StompHeaderAccessor accessor) {

    // 1. Get authenticated user
    Authentication authentication =
            (Authentication) accessor.getUser();

    if (authentication == null) {
        throw new IllegalStateException(
                "WebSocket user is not authenticated");
    }

    String username = authentication.getName();

    // 2. Validate conversation ID
    if (request == null || request.getConversationId() == null) {
        throw new IllegalArgumentException(
                "Conversation ID is required");
    }

    // 3. Mark messages as READ
    List<MessageStatusResponse> responses =
            messageService.markConversationAsRead(
                    username,
                    request.getConversationId()
            );

    // 4. Notify each original sender
    for (MessageStatusResponse response : responses) {

        messagingTemplate.convertAndSendToUser(
                response.getSenderUsername(),
                "/queue/message-status",
                response
        );

        System.out.println(
                "Message " + response.getMessageId()
                        + " read by " + username
        );
    }
}
}
