package com.senctraiq.conversations;

import lombok.Data;

@Data
public class ConversationActionRequest {
    private Long assignedAgentId;
    private String assignedAgentUsername;
}
