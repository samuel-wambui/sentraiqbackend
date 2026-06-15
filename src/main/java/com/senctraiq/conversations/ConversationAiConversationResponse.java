package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ConversationAiConversationResponse {
    private ConversationReferenceResponse conversation;
    private ConversationMessageResponse customerMessage;
    private ConversationMessageResponse agentAnswer;
    private List<ConversationMessageResponse> messages;
    private Map<String, Object> metrics;
}
