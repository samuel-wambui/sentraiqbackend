package com.senctraiq.conversations;

import lombok.Data;

@Data
public class IncomingMessageRequest {
    private String source;
    private String customerNo;
    private String customerId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String phoneNumberId;
    private String messageId;
    private String message;
    private Boolean handedOverFromAiAgent;
    private String category;
    private String sentiment;
}
