package com.senctraiq.conversations;

import lombok.Data;

@Data
public class ConversationAiAnswerRequest {
    private String message;
    private String messageId;
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
}
