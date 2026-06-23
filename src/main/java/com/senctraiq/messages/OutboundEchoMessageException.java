package com.senctraiq.messages;

public class OutboundEchoMessageException extends RuntimeException {
    public OutboundEchoMessageException(String senderId, String recipientId) {
        super("Outbound channel echo ignored for sender " + senderId + " and recipient " + recipientId);
    }
}
