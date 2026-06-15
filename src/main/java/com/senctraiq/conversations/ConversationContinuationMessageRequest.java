package com.senctraiq.conversations;

import lombok.Data;

@Data
public class ConversationContinuationMessageRequest {
    private Long conversationId;
    private String source;
    private String customerNo;
    private String customerId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String phoneNumberId;
    private String messageId;
    private String customerMessageId;
    private String message;
    private String customerMessage;
}
