package com.liveconnect.backend.service.impl;

import com.liveconnect.backend.dto.response.ConversationResponse;
import com.liveconnect.backend.entity.Conversation;
import com.liveconnect.backend.entity.ConversationMember;
import com.liveconnect.backend.entity.FriendRequestStatus;
import com.liveconnect.backend.entity.User;
import com.liveconnect.backend.entity.enums.ConversationType;
import com.liveconnect.backend.repository.ConversationMemberRepository;
import com.liveconnect.backend.repository.ConversationRepository;
import com.liveconnect.backend.repository.FriendRequestRepository;
import com.liveconnect.backend.repository.UserRepository;
import com.liveconnect.backend.service.ConversationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.liveconnect.backend.repository.FriendRequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final FriendRequestRepository friendRequestRepository;

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    public ConversationServiceImpl(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            FriendRequestRepository friendRequestRepository) {

        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    @Override
    @Transactional
    public ConversationResponse createDirectConversation(
            String username,
            String otherUserId) {

        // 1. Find current logged-in user
        User currentUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Current user not found"));

        // 2. Find the other user
        User otherUser = userRepository
                .findById(otherUserId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 3. Prevent chatting with yourself
        if (currentUser.getId().equals(otherUser.getId())) {
            throw new RuntimeException(
                    "You cannot create a conversation with yourself"
            );
        }

        boolean areFriends =
        friendRequestRepository.areFriends(
                currentUser,
                otherUser,
                FriendRequestStatus.ACCEPTED
        );

        if (!areFriends) {
            throw new RuntimeException(
                    "You can only start a conversation with a friend"
            );
        }
        // We will connect this with FriendRequestRepository
        // in the next small step.

        // 5. Check if conversation already exists
        Conversation existingConversation =
                findExistingDirectConversation(
                        currentUser,
                        otherUser
                );

        if (existingConversation != null) {
            return mapToResponse(existingConversation);
        }

        // 6. Create conversation
        LocalDateTime now = LocalDateTime.now();

        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Conversation savedConversation =
                conversationRepository.save(conversation);

        // 7. Add current user
        ConversationMember currentMember =
                ConversationMember.builder()
                        .conversation(savedConversation)
                        .user(currentUser)
                        .joinedAt(now)
                        .build();

        // 8. Add other user
        ConversationMember otherMember =
                ConversationMember.builder()
                        .conversation(savedConversation)
                        .user(otherUser)
                        .joinedAt(now)
                        .build();

        conversationMemberRepository.save(currentMember);
        conversationMemberRepository.save(otherMember);

        return mapToResponse(savedConversation);
    }

    private Conversation findExistingDirectConversation(
            User user1,
            User user2) {

        List<ConversationMember> user1Conversations =
                conversationMemberRepository.findByUser(user1);

        for (ConversationMember member :
                user1Conversations) {

            Conversation conversation =
                    member.getConversation();

            if (conversation.getType()
                    != ConversationType.DIRECT) {
                continue;
            }

            List<ConversationMember> members =
                    conversationMemberRepository
                            .findByConversation(conversation);

            if (members.size() != 2) {
                continue;
            }

            boolean containsUser2 =
                    members.stream()
                            .anyMatch(m ->
                                    m.getUser()
                                            .getId()
                                            .equals(user2.getId()));

            if (containsUser2) {
                return conversation;
            }
        }

        return null;
    }

    private ConversationResponse mapToResponse(
            Conversation conversation) {

        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}