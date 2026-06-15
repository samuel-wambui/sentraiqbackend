package com.senctraiq.conversations;

import lombok.Data;

@Data
public class ConversationReplyRequest extends ConversationActionRequest {
    private String message;
    private String messageId;
}
