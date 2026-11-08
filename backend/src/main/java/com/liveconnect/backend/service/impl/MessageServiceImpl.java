package com.liveconnect.backend.service.impl;

import com.liveconnect.backend.dto.request.SendMessageRequest;
import com.liveconnect.backend.dto.response.MessageResponse;
import com.liveconnect.backend.dto.response.MessageStatusResponse;
import com.liveconnect.backend.entity.Conversation;
import com.liveconnect.backend.entity.ConversationMember;
import com.liveconnect.backend.entity.Message;
import com.liveconnect.backend.entity.MessageReceipt;
import com.liveconnect.backend.entity.User;

import com.liveconnect.backend.entity.enums.MessageReceiptStatus;

import com.liveconnect.backend.repository.ConversationMemberRepository;
import com.liveconnect.backend.repository.ConversationRepository;
import com.liveconnect.backend.repository.MessageRepository;
import com.liveconnect.backend.repository.MessageReceiptRepository;
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
    private final MessageReceiptRepository messageReceiptRepository;

    public MessageServiceImpl(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            MessageRepository messageRepository,
            MessageReceiptRepository messageReceiptRepository) {

        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.messageReceiptRepository = messageReceiptRepository;
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

        // 3. Get all conversation members
        List<ConversationMember> members =
                conversationMemberRepository
                        .findByConversation(conversation);

        // 4. Make sure sender belongs to conversation
        boolean isMember = members.stream()
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

        // 5. Create message
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

        // 6. Create SENT receipts for all recipients
        List<MessageReceipt> receipts = members.stream()
                .filter(member ->
                        !member.getUser()
                                .getId()
                                .equals(sender.getId())
                )
                .map(member ->
                        MessageReceipt.builder()
                                .message(savedMessage)
                                .recipient(member.getUser())
                                .status(MessageReceiptStatus.SENT)
                                .createdAt(now)
                                .build()
                )
                .toList();

        if (!receipts.isEmpty()) {
            messageReceiptRepository.saveAll(receipts);
        }

        // 7. Update conversation's last activity
        conversation.setUpdatedAt(now);

        conversationRepository.save(conversation);

        // 8. Return response
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
    //send acknowledgment
    @Override
@Transactional
public MessageStatusResponse markMessageDelivered(
        String username,
        UUID messageId) {

    // 1. Find the authenticated recipient
    User recipient = userRepository
            .findByUsername(username)
            .orElseThrow(() ->
                    new RuntimeException("User not found"));

    // 2. Find the message
    Message message = messageRepository
            .findById(messageId)
            .orElseThrow(() ->
                    new RuntimeException("Message not found"));

    // 3. Find this recipient's receipt
    MessageReceipt receipt = messageReceiptRepository
            .findByMessageAndRecipient(message, recipient)
            .orElseThrow(() ->
                    new RuntimeException(
                            "You are not a recipient of this message"
                    ));

    // 4. Update only if it is still SENT
    // Never downgrade a READ receipt.
    if (receipt.getStatus() == MessageReceiptStatus.SENT) {

        LocalDateTime now = LocalDateTime.now();

        receipt.setStatus(MessageReceiptStatus.DELIVERED);
        receipt.setDeliveredAt(now);

        messageReceiptRepository.save(receipt);
    }

    // 5. Return the status event
    return MessageStatusResponse.builder()
            .messageId(message.getId())
            .conversationId(message.getConversation().getId())
            .recipientUsername(recipient.getUsername())
            .senderUsername(message.getSender().getUsername())
            .status(receipt.getStatus())
            .deliveredAt(receipt.getDeliveredAt())
            .build();
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