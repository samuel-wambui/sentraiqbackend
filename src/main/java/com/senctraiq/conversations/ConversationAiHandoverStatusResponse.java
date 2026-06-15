package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConversationAiHandoverStatusResponse {
    private Long conversationId;
    private boolean handedOverFromAiAgent;
    private LocalDateTime handedOverFromAiAgentAt;
    private LocalDateTime handedOverFromAiAgentExpiresAt;
}
