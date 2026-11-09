
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

    // ==========================================
    // SEND MESSAGE
    // ==========================================

    @Override
    @Transactional
    public MessageResponse sendMessage(
            String username,
            SendMessageRequest request) {

        User sender = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Conversation conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() ->
                        new RuntimeException("Conversation not found"));

        List<ConversationMember> members =
                conversationMemberRepository
                        .findByConversation(conversation);

        boolean isMember = members.stream()
                .anyMatch(member ->
                        member.getUser().getId().equals(sender.getId()));

        if (!isMember) {
            throw new RuntimeException(
                    "You are not a member of this conversation");
        }

        LocalDateTime now = LocalDateTime.now();

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Message savedMessage = messageRepository.save(message);

        // Create SENT receipts for all recipients except sender.
        List<MessageReceipt> receipts = members.stream()
                .filter(member ->
                        !member.getUser().getId().equals(sender.getId()))
                .map(member ->
                        MessageReceipt.builder()
                                .message(savedMessage)
                                .recipient(member.getUser())
                                .status(MessageReceiptStatus.SENT)
                                .createdAt(now)
                                .build())
                .toList();

        if (!receipts.isEmpty()) {
            messageReceiptRepository.saveAll(receipts);
        }

        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        return mapToResponse(savedMessage);
    }

    // ==========================================
    // GET CONVERSATION MESSAGES
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(
            String username,
            String conversationId,
            Pageable pageable) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        UUID id;

        try {
            id = UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid conversation ID");
        }

        Conversation conversation = conversationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Conversation not found"));

        boolean isMember = conversationMemberRepository
                .findByConversation(conversation)
                .stream()
                .anyMatch(member ->
                        member.getUser().getId().equals(user.getId()));

        if (!isMember) {
            throw new RuntimeException(
                    "You are not a member of this conversation");
        }

        Page<Message> messages = messageRepository
                .findByConversationOrderByCreatedAtDesc(
                        conversation,
                        pageable
                );

        return messages.map(this::mapToResponse);
    }

    // ==========================================
    // MARK MESSAGE AS DELIVERED
    // ==========================================

    @Override
    @Transactional
    public MessageStatusResponse markMessageDelivered(
            String username,
            UUID messageId) {

        User recipient = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Message message = messageRepository
                .findById(messageId)
                .orElseThrow(() ->
                        new RuntimeException("Message not found"));

        MessageReceipt receipt = messageReceiptRepository
                .findByMessageAndRecipient(message, recipient)
                .orElseThrow(() ->
                        new RuntimeException(
                                "You are not a recipient of this message"));

        // Update only SENT receipts.
        // Never downgrade DELIVERED or READ.
        if (receipt.getStatus() == MessageReceiptStatus.SENT) {

            LocalDateTime now = LocalDateTime.now();

            receipt.setStatus(MessageReceiptStatus.DELIVERED);
            receipt.setDeliveredAt(now);

            messageReceiptRepository.save(receipt);
        }

        return MessageStatusResponse.builder()
                .messageId(message.getId())
                .conversationId(message.getConversation().getId())
                .recipientUsername(recipient.getUsername())
                .senderUsername(message.getSender().getUsername())
                .status(receipt.getStatus())
                .deliveredAt(receipt.getDeliveredAt())
                .readAt(receipt.getReadAt())
                .build();
    }

    // ==========================================
    // MARK CONVERSATION MESSAGES AS READ
    // ==========================================

    @Override
    @Transactional
    public List<MessageStatusResponse> markConversationAsRead(
            String username,
            UUID conversationId) {

        // Find authenticated user.
        User recipient = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Find conversation.
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new RuntimeException("Conversation not found"));

        // Verify conversation membership.
        boolean isMember = conversationMemberRepository
                .findByConversation(conversation)
                .stream()
                .anyMatch(member ->
                        member.getUser().getId().equals(recipient.getId()));

        if (!isMember) {
            throw new RuntimeException(
                    "You are not a member of this conversation");
        }

        // Fetch unread receipts belonging to this recipient.
        List<MessageReceipt> unreadReceipts =
                messageReceiptRepository.findUnreadReceiptsForConversation(
                        recipient,
                        conversationId,
                        recipient.getId(),
                        MessageReceiptStatus.READ
                );

        LocalDateTime now = LocalDateTime.now();

        // Update each receipt.
        for (MessageReceipt receipt : unreadReceipts) {

            receipt.setStatus(MessageReceiptStatus.READ);

            // A READ message must also be DELIVERED.
            if (receipt.getDeliveredAt() == null) {
                receipt.setDeliveredAt(now);
            }

            receipt.setReadAt(now);
        }

        messageReceiptRepository.saveAll(unreadReceipts);

        // Create one notification per message for its original sender.
        return unreadReceipts.stream()
                .map(receipt -> {

                    Message message = receipt.getMessage();

                    return MessageStatusResponse.builder()
                            .messageId(message.getId())
                            .conversationId(conversationId)
                            .recipientUsername(recipient.getUsername())
                            .senderUsername(
                                    message.getSender().getUsername())
                            .status(MessageReceiptStatus.READ)
                            .deliveredAt(receipt.getDeliveredAt())
                            .readAt(receipt.getReadAt())
                            .build();
                })
                .toList();
    }

    // ==========================================
    // MAP MESSAGE ENTITY TO RESPONSE DTO
    // ==========================================

    private MessageResponse mapToResponse(Message message) {

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(
                        message.getConversation().getId())
                .senderId(
                        message.getSender().getId())
                .senderUsername(
                        message.getSender().getUsername())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
