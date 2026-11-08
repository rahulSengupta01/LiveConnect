package com.liveconnect.backend.repository;

import com.liveconnect.backend.entity.Message;
import com.liveconnect.backend.entity.MessageReceipt;
import com.liveconnect.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

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
}