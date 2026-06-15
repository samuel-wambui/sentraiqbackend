package com.senctraiq.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ConversationThreadResponse {
    private ConversationReferenceResponse conversation;
    private List<ConversationMessageResponse> messages;
    private List<String> agentUsernames;
    private int agentCount;
}
