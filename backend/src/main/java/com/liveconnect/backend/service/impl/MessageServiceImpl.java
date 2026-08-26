package com.liveconnect.backend.service.impl;

import com.liveconnect.backend.dto.request.SendMessageRequest;
import com.liveconnect.backend.dto.response.MessageResponse;
import com.liveconnect.backend.entity.Conversation;
import com.liveconnect.backend.entity.ConversationMember;
import com.liveconnect.backend.entity.Message;
import com.liveconnect.backend.entity.User;
import com.liveconnect.backend.repository.ConversationMemberRepository;
import com.liveconnect.backend.repository.ConversationRepository;
import com.liveconnect.backend.repository.MessageRepository;
import com.liveconnect.backend.repository.UserRepository;
import com.liveconnect.backend.service.MessageService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageServiceImpl implements MessageService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;

    public MessageServiceImpl(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            MessageRepository messageRepository) {

        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(
            String username,
            SendMessageRequest request) {

        // 1. Find logged-in user
        User sender = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 2. Find conversation
        Conversation conversation =
                conversationRepository
                        .findById(request.getConversationId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversation not found"
                                ));

        // 3. Make sure sender belongs to conversation
        boolean isMember =
                conversationMemberRepository
                        .findByConversation(conversation)
                        .stream()
                        .anyMatch(member ->
                                member.getUser()
                                        .getId()
                                        .equals(sender.getId())
                        );

        if (!isMember) {
            throw new RuntimeException(
                    "You are not a member of this conversation"
            );
        }

        // 4. Create message
        LocalDateTime now = LocalDateTime.now();

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Message savedMessage =
                messageRepository.save(message);

        // 5. Update conversation's last activity
        conversation.setUpdatedAt(now);

        conversationRepository.save(conversation);

        // 6. Return response
        return mapToResponse(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(
            String username,
            String conversationId,
            Pageable pageable) {

        // 1. Find logged-in user
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 2. Convert conversation ID
        UUID id;

        try {
            id = UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid conversation ID"
            );
        }

        // 3. Find conversation
        Conversation conversation =
                conversationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversation not found"
                                ));

        // 4. Check membership
        boolean isMember =
                conversationMemberRepository
                        .findByConversation(conversation)
                        .stream()
                        .anyMatch(member ->
                                member.getUser()
                                        .getId()
                                        .equals(user.getId())
                        );

        if (!isMember) {
            throw new RuntimeException(
                    "You are not a member of this conversation"
            );
        }

        // 5. Get messages
        Page<Message> messages =
                messageRepository
                        .findByConversationOrderByCreatedAtDesc(
                                conversation,
                                pageable
                        );

        // 6. Convert entities to DTOs
        return messages.map(this::mapToResponse);
    }

    private MessageResponse mapToResponse(
            Message message) {

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(
                        message.getConversation().getId()
                )
                .senderId(
                        message.getSender().getId()
                )
                .senderUsername(
                        message.getSender().getUsername()
                )
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}