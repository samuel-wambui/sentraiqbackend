package com.senctraiq.messages;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor

public class Messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String messageId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String message;
    private String reply;
    private String repliedBy;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
}
