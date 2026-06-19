package com.senctraiq.AI.sentiment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentRequestDTO {

    private String messageId;
    private String category;
    private String sentiment;

}

