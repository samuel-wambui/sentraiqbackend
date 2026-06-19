package com.senctraiq.n8n;

import lombok.Data;

@Data
public class AiHandoverMessageDTO {
    private String source;
    private String receivedAt;
    private String repliedAt;

    private String recipientId;
    private String senderId;
    private String senderName;

    private String customerMessageId;
    private String customerMessage;

    private String agentAnswerMessageId;
    private String agentAnswer;
    private String answeredBy;

    private String category;
    private String sentiment;
    //private Double confidence;
}
