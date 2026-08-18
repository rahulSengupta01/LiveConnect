package com.liveconnect.backend.service.impl;

import com.liveconnect.backend.dto.response.FriendRequestResponse;
import com.liveconnect.backend.entity.FriendRequest;
import com.liveconnect.backend.entity.FriendRequestStatus;
import com.liveconnect.backend.entity.User;
import com.liveconnect.backend.repository.FriendRequestRepository;
import com.liveconnect.backend.repository.UserRepository;
import com.liveconnect.backend.service.FriendService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FriendServiceImpl implements FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;

    public FriendServiceImpl(
            FriendRequestRepository friendRequestRepository,
            UserRepository userRepository) {

        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public FriendRequestResponse sendFriendRequest(
            String senderUsername,
            String receiverUsername) {

        User sender = userRepository
                .findByUsername(senderUsername)
                .orElseThrow(() ->
                        new RuntimeException("Sender not found"));

        User receiver = userRepository
                .findByUsername(receiverUsername)
                .orElseThrow(() ->
                        new RuntimeException("Receiver not found"));

        // Cannot send request to yourself
        if (sender.getId().equals(receiver.getId())) {
            throw new RuntimeException(
                    "You cannot send a friend request to yourself"
            );
        }

        // Check existing request
        friendRequestRepository
                .findBySenderAndReceiver(sender, receiver)
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "Friend request already exists"
                    );
                });

        // Check reverse request
        friendRequestRepository
                .findBySenderAndReceiver(receiver, sender)
                .ifPresent(existing -> {
                    if (existing.getStatus()
                            == FriendRequestStatus.PENDING) {

                        throw new RuntimeException(
                                "This user has already sent you a friend request"
                        );
                    }
                });

        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        FriendRequest saved =
                friendRequestRepository.save(request);

        return mapToResponse(saved);
    }

    @Override
    public List<FriendRequestResponse> getPendingRequests(
            String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        List<FriendRequest> requests =
                friendRequestRepository
                        .findByReceiverAndStatus(
                                user,
                                FriendRequestStatus.PENDING
                        );

        return requests.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public FriendRequestResponse acceptFriendRequest(
            String username,
            UUID requestId) {

        User currentUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        FriendRequest request =
                friendRequestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Friend request not found"
                                ));

        // Only receiver can accept
        if (!request.getReceiver()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You are not allowed to accept this request"
            );
        }

        if (request.getStatus()
                != FriendRequestStatus.PENDING) {

            throw new RuntimeException(
                    "Friend request is not pending"
            );
        }

        request.setStatus(
                FriendRequestStatus.ACCEPTED
        );

        request.setUpdatedAt(
                java.time.LocalDateTime.now()
        );

        FriendRequest saved =
                friendRequestRepository.save(request);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public FriendRequestResponse rejectFriendRequest(
            String username,
            UUID requestId) {

        User currentUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        FriendRequest request =
                friendRequestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Friend request not found"
                                ));

        // Only receiver can reject
        if (!request.getReceiver()
                .getId()
                .equals(currentUser.getId())) {

            throw new RuntimeException(
                    "You are not allowed to reject this request"
            );
        }

        if (request.getStatus()
                != FriendRequestStatus.PENDING) {

            throw new RuntimeException(
                    "Friend request is not pending"
            );
        }

        request.setStatus(
                FriendRequestStatus.REJECTED
        );

        request.setUpdatedAt(
                java.time.LocalDateTime.now()
        );

        FriendRequest saved =
                friendRequestRepository.save(request);

        return mapToResponse(saved);
    }

    @Override
    public List<FriendRequestResponse> getFriends(
            String username) {

        User currentUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        List<FriendRequestResponse> friends =
                new ArrayList<>();

        // Requests where current user is sender
        List<FriendRequest> sent =
                friendRequestRepository
                        .findBySenderAndStatus(
                                currentUser,
                                FriendRequestStatus.ACCEPTED
                        );

        for (FriendRequest request : sent) {
            friends.add(mapToResponse(request));
        }

        // Requests where current user is receiver
        List<FriendRequest> received =
                friendRequestRepository
                        .findByReceiverAndStatus(
                                currentUser,
                                FriendRequestStatus.ACCEPTED
                        );

        for (FriendRequest request : received) {
            friends.add(mapToResponse(request));
        }

        return friends;
    }

    @Override
    @Transactional
    public void removeFriend(
        String username,
        String friendId) {

        User currentUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        User friend = userRepository
                .findById(friendId)
                .orElseThrow(() ->
                        new RuntimeException("Friend not found"));

        // Check current user -> friend
        friendRequestRepository
                .findBySenderAndReceiver(
                        currentUser,
                        friend
                )
                .ifPresent(request -> {
                    if (request.getStatus()
                            == FriendRequestStatus.ACCEPTED) {

                        friendRequestRepository.delete(request);
                    }
                });

        // Check friend -> current user
        friendRequestRepository
                .findBySenderAndReceiver(
                        friend,
                        currentUser
                )
                .ifPresent(request -> {
                    if (request.getStatus()
                            == FriendRequestStatus.ACCEPTED) {

                        friendRequestRepository.delete(request);
                    }
                });
    }

    private FriendRequestResponse mapToResponse(
            FriendRequest request) {

        return FriendRequestResponse.builder()
                .id(request.getId())

                .senderId(
                        request.getSender().getId()
                )

                .senderUsername(
                        request.getSender().getUsername()
                )

                .receiverId(
                        request.getReceiver().getId()
                )

                .receiverUsername(
                        request.getReceiver().getUsername()
                )

                .status(request.getStatus())

                .build();
    }
}