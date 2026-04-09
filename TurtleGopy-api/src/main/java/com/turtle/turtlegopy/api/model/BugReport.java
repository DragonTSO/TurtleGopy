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
public class BugReport {

    private UUID id;
    private UUID playerUUID;
    private String playerName;
    private String content;
    private BugReportStatus status;
    private long createdAt;
    private String adminNote;
    private boolean rewardGiven;
    private boolean rewardPending;
}
