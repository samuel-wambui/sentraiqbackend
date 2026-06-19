package com.senctraiq.AI.sentiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SentimentsService {

    @Autowired
    private SentimentsRepository sentimentsRepository;

    /**
     * Create a new sentiment analysis from request DTO
     */
    public SentimentResponseDTO createSentiment(SentimentRequestDTO requestDTO) {
        Sentiments sentiment = new Sentiments();
        sentiment.setMessageId(requestDTO.getMessageId());
        sentiment.setCategory(requestDTO.getCategory());
        sentiment.setSentiment(requestDTO.getSentiment());


        Sentiments savedSentiment = sentimentsRepository.save(sentiment);
        return convertToResponseDTO(savedSentiment);
    }

    /**
     * Convert Sentiments entity to SentimentResponseDTO
     */
    private SentimentResponseDTO convertToResponseDTO(Sentiments sentiment) {
        SentimentResponseDTO dto = new SentimentResponseDTO();
        dto.setId(sentiment.getId());
        dto.setMessageId(sentiment.getMessageId());
        dto.setCategory(sentiment.getCategory());
        dto.setSentiment(sentiment.getSentiment());

        return dto;
    }
}

