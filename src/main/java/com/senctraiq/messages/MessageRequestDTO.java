package com.senctraiq.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequestDTO {

    private String source;
    private String messageId;
    private String senderId;
    private String senderName;
    private String recipientId;
    private String message;
}

