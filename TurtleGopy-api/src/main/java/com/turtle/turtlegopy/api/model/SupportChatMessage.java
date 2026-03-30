package com.turtle.turtlegopy.api.model;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportChatMessage {

    private UUID id;
    private UUID ticketId;
    private UUID senderUUID;
    private String senderName;
    private String message;
    private long timestamp;
    private boolean staff;
}
