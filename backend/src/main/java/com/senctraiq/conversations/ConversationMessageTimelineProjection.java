package com.senctraiq.conversations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ConversationMessageTimelineProjection {
    Long getId();
    String getMessageId();
    Long getConversationId();
    String getSenderType();
    String getSenderId();
    String getSenderName();
    String getMessage();
    String getCategory();
    String getSentiment();
    BigDecimal getConfidence();
    LocalDateTime getCreatedAt();
}
