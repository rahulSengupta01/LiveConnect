package com.liveconnect.backend.controller;

import com.liveconnect.backend.dto.response.FriendRequestResponse;
import com.liveconnect.backend.service.FriendService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/request")
    public ResponseEntity<FriendRequestResponse>
    sendFriendRequest(
            Authentication authentication,
            @RequestParam String username) {

        FriendRequestResponse response =
                friendService.sendFriendRequest(
                        authentication.getName(),
                        username
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequestResponse>>
    getPendingRequests(
            Authentication authentication) {

        return ResponseEntity.ok(
                friendService.getPendingRequests(
                        authentication.getName()
                )
        );
    }

    @PutMapping("/request/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse>
    acceptFriendRequest(
            Authentication authentication,
            @PathVariable UUID requestId) {

        return ResponseEntity.ok(
                friendService.acceptFriendRequest(
                        authentication.getName(),
                        requestId
                )
        );
    }

    @PutMapping("/request/{requestId}/reject")
    public ResponseEntity<FriendRequestResponse>
    rejectFriendRequest(
            Authentication authentication,
            @PathVariable UUID requestId) {

        return ResponseEntity.ok(
                friendService.rejectFriendRequest(
                        authentication.getName(),
                        requestId
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<FriendRequestResponse>>
    getFriends(
            Authentication authentication) {

        return ResponseEntity.ok(
                friendService.getFriends(
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(
            Authentication authentication,
            @PathVariable String friendId) {

        friendService.removeFriend(
                authentication.getName(),
                friendId
        );

        return ResponseEntity.noContent().build();
    }
}