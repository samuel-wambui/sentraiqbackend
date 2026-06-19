package com.senctraiq.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {

    private Long id;
    private String source;
    private String messageId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String message;
    private String reply;
    private boolean isConversationContinuation;
    private String repliedBy;
    private String category;
    private String sentiment;
    private String ticketNumber;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    private boolean deleted;
}

