// package com.liveconnect.backend.repository;

// import com.liveconnect.backend.entity.FriendRequest;
// import com.liveconnect.backend.entity.FriendRequestStatus;
// import com.liveconnect.backend.entity.User;

// import org.springframework.data.jpa.repository.JpaRepository;

// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;

// public interface FriendRequestRepository
//         extends JpaRepository<FriendRequest, UUID> {

//     Optional<FriendRequest> findBySenderAndReceiver(
//             User sender,
//             User receiver
//     );

//     List<FriendRequest> findByReceiverAndStatus(
//             User receiver,
//             FriendRequestStatus status
//     );

//     List<FriendRequest> findBySenderAndStatus(
//             User sender,
//             FriendRequestStatus status
//     );
// }
package com.liveconnect.backend.repository;

import com.liveconnect.backend.entity.FriendRequest;
import com.liveconnect.backend.entity.FriendRequestStatus;
import com.liveconnect.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        SELECT COUNT(fr) > 0
        FROM FriendRequest fr
        WHERE fr.status = :status
          AND (
              (fr.sender = :user1 AND fr.receiver = :user2)
              OR
              (fr.sender = :user2 AND fr.receiver = :user1)
          )
    """)
    boolean areFriends(
            @Param("user1") User user1,
            @Param("user2") User user2,
            @Param("status") FriendRequestStatus status
    );
}