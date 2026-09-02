package com.liveconnect.backend.entity;

import com.liveconnect.backend.entity.enums.ConversationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String name;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}