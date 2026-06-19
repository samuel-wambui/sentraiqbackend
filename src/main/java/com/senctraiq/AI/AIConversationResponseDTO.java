package com.senctraiq.AI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIConversationResponseDTO {

    private Long id;
    private String messageId;
    private LocalDateTime lastRepliedAt;
    private boolean handedOverByAI;
}

