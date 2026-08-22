package com.liveconnect.backend.dto.response;

import com.liveconnect.backend.entity.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private UUID id;

    private ConversationType type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}