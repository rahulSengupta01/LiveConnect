package com.liveconnect.backend.repository;

import com.liveconnect.backend.entity.FriendRequest;
import com.liveconnect.backend.entity.FriendRequestStatus;
import com.liveconnect.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository
        extends JpaRepository<FriendRequest, UUID> {

    Optional<FriendRequest> findBySenderAndReceiver(
            User sender,
            User receiver
    );

    List<FriendRequest> findByReceiverAndStatus(
            User receiver,
            FriendRequestStatus status
    );

    List<FriendRequest> findBySenderAndStatus(
            User sender,
            FriendRequestStatus status
    );
}