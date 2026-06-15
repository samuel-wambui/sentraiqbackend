package com.senctraiq.conversations;

import lombok.Data;

@Data
public class ConversationAiHandoverRequest {
    private Boolean handedOverFromAiAgent;
    private String handoverNote;
}
