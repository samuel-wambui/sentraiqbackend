package com.senctraiq.conversations;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class ConversationAiConversationRequest {
    private String source;
    private String customerNo;
    private String senderId;
    private String senderName;
    private String customerId;
    private String recipientId;
    private String customerMessageId;
    private String messageId;
    private String customerMessage;
    private String message;
    private String agentAnswerMessageId;
    private String answerMessageId;
    private String agentAnswer;
    private String answer;
    private String answeredBy;
    private Long answeredByAgentId;
    private String answeredByUsername;
    private String answeredByEmail;
    private String answeredByFirstName;
    private String answeredByLastName;
    private Boolean answeredByAiAgent;
    private Boolean handover;
    private Boolean handedOver;
    private Boolean handedOverFromAiAgent;
    private String handoverNote;
    private String timestamp;
    private String replyTo;
    private String phoneNumberId;
    private String category;
    private String sentiment;
    private BigDecimal confidence;
    private Map<String, Object> metrics;
}
