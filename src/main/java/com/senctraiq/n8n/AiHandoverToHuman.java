package com.senctraiq.n8n;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiHandoverToHuman {
    private String source;
    private String senderName;
    private String senderId;
    private String recipientId;
    private String messageId;
    private String message;
    private Boolean handedOverFromAiAgent;
    private String category;
    private String sentiment;
    private String receivedAt;

}
