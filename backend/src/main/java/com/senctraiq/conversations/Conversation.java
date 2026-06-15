package com.senctraiq.conversations;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String senderId;
    private String senderName;
    private String recipientId;

    @Enumerated(EnumType.STRING)
    private ConversationStatus status;

    private String category;
    private String sentiment;
    private BigDecimal confidence;

    private Long assignedAgentId;
    private Long handedOverByAgentId;
    private Boolean handedOverFromAiAgent = false;
    private String lastAnsweredBy;
    private Long lastAnsweredByAgentId;
    private Boolean lastAnsweredByAiAgent = false;

    @Column(columnDefinition = "TEXT")
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

    public Boolean getHandedOverFromAiAgent() {
        return Boolean.TRUE.equals(handedOverFromAiAgent);
    }
}
