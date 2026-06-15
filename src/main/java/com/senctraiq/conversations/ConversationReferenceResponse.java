package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Data
@AllArgsConstructor
public class ConversationReferenceResponse {
    private Long id;
    private String source;
    private String senderId;
    private String senderName;
    private String recipientId;
    private ConversationStatus status;
    private String category;
    private String sentiment;
    private BigDecimal confidence;
    private Long assignedAgentId;
    private String assignedAgentUsername;
    private Long handedOverByAgentId;
    private String handedOverByUsername;
    private Boolean handedOverFromAiAgent;
    private String lastAnsweredBy;
    private Long lastAnsweredByAgentId;
    private String lastAnsweredByUsername;
    private String lastAnsweredByEmail;
    private String lastAnsweredByFirstName;
    private String lastAnsweredByLastName;
    private Boolean lastAnsweredByAiAgent;
    private String handoverNote;
    private LocalDateTime startedAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime assignedAt;
    private LocalDateTime handedOverAt;
    private LocalDateTime handedOverFromAiAgentAt;
    private LocalDateTime handedOverFromAiAgentExpiresAt;
    private LocalDateTime lastAnsweredAt;
    private LocalDateTime statusUpdatedAt;
    private LocalDateTime pendingSince;
    private LocalDateTime closedAt;

    public static ConversationReferenceResponse fromProjection(ConversationReferenceProjection conversation) {
        if (conversation == null) return null;
        return new ConversationReferenceResponse(
                conversation.getId(),
                conversation.getSource(),
                conversation.getSenderId(),
                conversation.getSenderName(),
                conversation.getRecipientId(),
                parseStatus(conversation.getStatus()),
                conversation.getCategory(),
                conversation.getSentiment(),
                conversation.getConfidence(),
                conversation.getAssignedAgentId(),
                conversation.getAssignedAgentUsername(),
                conversation.getHandedOverByAgentId(),
                conversation.getHandedOverByUsername(),
                conversation.getHandedOverFromAiAgent(),
                conversation.getLastAnsweredBy(),
                conversation.getLastAnsweredByAgentId(),
                conversation.getLastAnsweredByUsername(),
                conversation.getLastAnsweredByEmail(),
                conversation.getLastAnsweredByFirstName(),
                conversation.getLastAnsweredByLastName(),
                conversation.getLastAnsweredByAiAgent(),
                conversation.getHandoverNote(),
                conversation.getStartedAt(),
                conversation.getLastMessageAt(),
                conversation.getAssignedAt(),
                conversation.getHandedOverAt(),
                conversation.getHandedOverFromAiAgentAt(),
                conversation.getHandedOverFromAiAgentExpiresAt(),
                conversation.getLastAnsweredAt(),
                conversation.getStatusUpdatedAt(),
                conversation.getPendingSince(),
                conversation.getClosedAt()
        );
    }

    public static ConversationReferenceResponse fromEntity(Conversation conversation) {
        if (conversation == null) return null;
        return new ConversationReferenceResponse(
                conversation.getId(),
                conversation.getSource(),
                conversation.getSenderId(),
                conversation.getSenderName(),
                conversation.getRecipientId(),
                conversation.getStatus(),
                conversation.getCategory(),
                conversation.getSentiment(),
                conversation.getConfidence(),
                conversation.getAssignedAgentId(),
                null,
                conversation.getHandedOverByAgentId(),
                null,
                conversation.getHandedOverFromAiAgent(),
                conversation.getLastAnsweredBy(),
                conversation.getLastAnsweredByAgentId(),
                null,
                null,
                null,
                null,
                conversation.getLastAnsweredByAiAgent(),
                conversation.getHandoverNote(),
                conversation.getStartedAt(),
                conversation.getLastMessageAt(),
                conversation.getAssignedAt(),
                conversation.getHandedOverAt(),
                conversation.getHandedOverFromAiAgentAt(),
                conversation.getHandedOverFromAiAgentExpiresAt(),
                conversation.getLastAnsweredAt(),
                conversation.getStatusUpdatedAt(),
                conversation.getPendingSince(),
                conversation.getClosedAt()
        );
    }

    private static ConversationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return ConversationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
