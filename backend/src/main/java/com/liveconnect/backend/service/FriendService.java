package com.liveconnect.backend.service;

import com.liveconnect.backend.dto.response.FriendRequestResponse;

import java.util.List;
import java.util.UUID;

public interface FriendService {

    FriendRequestResponse sendFriendRequest(
            String senderUsername,
            String receiverUsername
    );

    List<FriendRequestResponse> getPendingRequests(
            String username
    );

    FriendRequestResponse acceptFriendRequest(
            String username,
            UUID requestId
    );

    FriendRequestResponse rejectFriendRequest(
            String username,
            UUID requestId
    );

    List<FriendRequestResponse> getFriends(
            String username
    );

    void removeFriend(
        String username,
        String friendId
    );
}