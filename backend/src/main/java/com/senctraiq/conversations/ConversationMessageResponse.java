package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ConversationMessageResponse {
    private Long id;
    private String messageId;
    private Long conversationId;
    private SenderType senderType;
    private String senderId;
    private String senderName;
    private String message;
    private String category;
    private String sentiment;
    private BigDecimal confidence;
    private LocalDateTime createdAt;

    public static ConversationMessageResponse fromEntity(ConversationMessage message) {
        Conversation conversation = message.getConversation();
        return new ConversationMessageResponse(
                message.getId(),
                message.getMessageId(),
                conversation != null ? conversation.getId() : null,
                message.getSenderType(),
                message.getSenderId(),
                message.getSenderName(),
                message.getMessage(),
                message.getCategory(),
                message.getSentiment(),
                message.getConfidence(),
                message.getCreatedAt()
        );
    }

    public static ConversationMessageResponse fromProjection(ConversationMessageTimelineProjection message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getMessageId(),
                message.getConversationId(),
                parseSenderType(message.getSenderType()),
                message.getSenderId(),
                message.getSenderName(),
                message.getMessage(),
                message.getCategory(),
                message.getSentiment(),
                message.getConfidence(),
                message.getCreatedAt()
        );
    }

    private static SenderType parseSenderType(String value) {
        if (value == null || value.isBlank()) return null;
        return SenderType.valueOf(value.trim().toUpperCase());
    }
}
