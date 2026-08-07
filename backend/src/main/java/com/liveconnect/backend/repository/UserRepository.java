package com.liveconnect.backend.repository;

import com.liveconnect.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

}