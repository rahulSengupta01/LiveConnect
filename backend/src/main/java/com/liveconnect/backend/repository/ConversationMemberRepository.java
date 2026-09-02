package com.liveconnect.backend.repository;

import com.liveconnect.backend.entity.Conversation;
import com.liveconnect.backend.entity.ConversationMember;
import com.liveconnect.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationMemberRepository
        extends JpaRepository<ConversationMember, UUID> {

    List<ConversationMember> findByConversation(
            Conversation conversation
    );

    List<ConversationMember> findByUser(
            User user
    );
}