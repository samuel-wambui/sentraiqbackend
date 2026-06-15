package com.senctraiq.conversations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "conversation_continuation_messages")
public class ConversationContinuationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long conversationId;

    @Column(unique = true)
    private String messageId;

    private String source;
    private String senderId;
    private String senderName;
    private String recipientId;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;
}
