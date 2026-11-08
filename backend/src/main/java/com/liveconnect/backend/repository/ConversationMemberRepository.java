
package com.liveconnect.backend.repository;

import com.liveconnect.backend.entity.Conversation;
import com.liveconnect.backend.entity.ConversationMember;
import com.liveconnect.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // NEW METHOD: Get all usernames in a conversation
    @Query("""
        SELECT cm.user.username
        FROM ConversationMember cm
        WHERE cm.conversation.id = :conversationId
    """)
    List<String> findUsernamesByConversationId(
            @Param("conversationId") UUID conversationId
    );
}
