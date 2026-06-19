package com.senctraiq.messages;

public class DuplicateMessageException extends RuntimeException {
    public DuplicateMessageException(String messageId) {
        super("Message already exists for messageId: " + messageId);
    }
}
