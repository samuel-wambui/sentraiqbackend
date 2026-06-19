package com.senctraiq.AI.sentiment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentResponseDTO {

    private Long id;
    private String messageId;
    private String category;
    private String sentiment;
    private BigDecimal confidence;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
}

