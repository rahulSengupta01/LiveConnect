
package com.liveconnect.backend.repository;

import com.liveconnect.backend.entity.Message;
import com.liveconnect.backend.entity.MessageReceipt;
import com.liveconnect.backend.entity.User;
import com.liveconnect.backend.entity.enums.MessageReceiptStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReceiptRepository
        extends JpaRepository<MessageReceipt, UUID> {

    List<MessageReceipt> findByMessage(Message message);

    Optional<MessageReceipt> findByMessageAndRecipient(
            Message message,
            User recipient
    );

    @Query("""
        SELECT r
        FROM MessageReceipt r
        JOIN FETCH r.message m
        JOIN FETCH m.sender s
        WHERE r.recipient = :recipient
          AND m.conversation.id = :conversationId
          AND s.id <> :recipientId
          AND r.status <> :readStatus
        ORDER BY m.createdAt ASC
    """)
    List<MessageReceipt> findUnreadReceiptsForConversation(
            @Param("recipient") User recipient,
            @Param("conversationId") UUID conversationId,
            @Param("recipientId") String recipientId,
            @Param("readStatus") MessageReceiptStatus readStatus
    );
}
