package com.senctraiq.AI.sentiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sentiments")
@CrossOrigin(originPatterns = "*", maxAge = 3600)
public class SentimentsController {

    @Autowired
    private SentimentsService sentimentsService;

    /**
     * Create a new sentiment analysis
     * POST /api/v1/sentiments
     */
    @PostMapping
    public ResponseEntity<SentimentResponseDTO> createSentiment(@RequestBody SentimentRequestDTO requestDTO) {
        SentimentResponseDTO response = sentimentsService.createSentiment(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
