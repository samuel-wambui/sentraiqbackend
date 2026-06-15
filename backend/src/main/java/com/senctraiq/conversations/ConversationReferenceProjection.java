package com.senctraiq.conversations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ConversationReferenceProjection {
    Long getId();
    String getSource();
    String getSenderId();
    String getSenderName();
    String getRecipientId();
    String getStatus();
    String getCategory();
    String getSentiment();
    BigDecimal getConfidence();
    Long getAssignedAgentId();
    String getAssignedAgentUsername();
    Long getHandedOverByAgentId();
    String getHandedOverByUsername();
    Boolean getHandedOverFromAiAgent();
    String getLastAnsweredBy();
    Long getLastAnsweredByAgentId();
    String getLastAnsweredByUsername();
    String getLastAnsweredByEmail();
    String getLastAnsweredByFirstName();
    String getLastAnsweredByLastName();
    Boolean getLastAnsweredByAiAgent();
    String getHandoverNote();
    LocalDateTime getStartedAt();
    LocalDateTime getLastMessageAt();
    LocalDateTime getAssignedAt();
    LocalDateTime getHandedOverAt();
    LocalDateTime getHandedOverFromAiAgentAt();
    LocalDateTime getHandedOverFromAiAgentExpiresAt();
    LocalDateTime getLastAnsweredAt();
    LocalDateTime getStatusUpdatedAt();
    LocalDateTime getPendingSince();
    LocalDateTime getClosedAt();
}
