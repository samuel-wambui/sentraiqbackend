package com.senctraiq.n8n;


import lombok.Data;

@Data
public class ConversationContinuationDTO {
    private String conversationId;

    private String source;

    private String recipientId;

    private String senderId;

    private String senderName;

    private String customerMessageId;

    private String customerMessage;

}
