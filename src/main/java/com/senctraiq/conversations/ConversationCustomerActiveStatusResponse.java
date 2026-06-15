package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConversationCustomerActiveStatusResponse {
    private String customerId;
    private String source;
    private boolean active;
    private Long conversationId;
    private ConversationStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime conversationExpiresAt;
    private boolean handedOverFromAiAgent;
    private LocalDateTime handedOverFromAiAgentAt;
    private LocalDateTime handedOverFromAiAgentExpiresAt;
}
