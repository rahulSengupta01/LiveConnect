package com.liveconnect.backend.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class MessageDeliveryRequest {
    private UUID messageId;
}