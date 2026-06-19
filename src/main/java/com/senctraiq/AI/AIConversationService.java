package com.senctraiq.AI;

import com.senctraiq.n8n.AiFullConversationDTO;
import com.senctraiq.n8n.AiHandoverToHuman;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AIConversationService {

    @Autowired
    private AIConversationRepository aiConversationRepository;


    public Optional<AIConversationResponseDTO> getAIConversationById(Long id) {
        return aiConversationRepository.findActiveById(id)
                .map(this::convertToResponseDTO);
    }

    /**
     * Get all active AI conversations
     */
    public List<AIConversationResponseDTO> getAllAIConversations() {
        return aiConversationRepository.findAllActive()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get AI conversation by message ID
     */
    public Optional<AIConversationResponseDTO> getAIConversationByMessageId(String messageId) {
        return aiConversationRepository.findActiveByMessageId(messageId)
                .map(this::convertToResponseDTO);
    }

    /**
     * Get all conversations that have been handed over by AI
     */
    public List<AIConversationResponseDTO> getHandedOverConversations() {
        return aiConversationRepository.findActiveHandedOverConversations()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all ongoing AI conversations (not handed over)
     */
    public List<AIConversationResponseDTO> getOngoingConversations() {
        return aiConversationRepository.findActiveOngoingConversations()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update an AI conversation
     */
    public AIConversationResponseDTO updateAIConversation(Long id, AIConversationRequestDTO requestDTO) {
        Optional<AIConversation> existingConversation = aiConversationRepository.findActiveById(id);
        if (existingConversation.isPresent()) {
            AIConversation aiConversation = existingConversation.get();

            if (requestDTO.getMessageId() != null) {
                aiConversation.setMessageId(requestDTO.getMessageId());
            }
            if (requestDTO.getLastRepliedAt() != null) {
                aiConversation.setLastRepliedAt(requestDTO.getLastRepliedAt());
            }
            aiConversation.setHandedOverByAI(requestDTO.isHandedOverByAI());

            AIConversation updatedConversation = aiConversationRepository.save(aiConversation);
            return convertToResponseDTO(updatedConversation);
        }
        return null;
    }

    /**
     * Mark AI conversation as handed over to human
     */
    public AIConversationResponseDTO handoverToHuman(Long id) {
        Optional<AIConversation> existingConversation = aiConversationRepository.findActiveById(id);
        if (existingConversation.isPresent()) {
            AIConversation aiConversation = existingConversation.get();
            aiConversation.setHandedOverByAI(true);
            aiConversation.setLastRepliedAt(LocalDateTime.now());

            AIConversation updatedConversation = aiConversationRepository.save(aiConversation);
            return convertToResponseDTO(updatedConversation);
        }
        return null;
    }

    /**
     * Soft delete an AI conversation (marks as deleted, does not remove from database)
     */
    public boolean deleteAIConversation(Long id) {
        Optional<AIConversation> existingConversation = aiConversationRepository.findActiveById(id);
        if (existingConversation.isPresent()) {
            AIConversation aiConversation = existingConversation.get();
            aiConversation.setDeleted(true);
            aiConversationRepository.save(aiConversation);
            return true;
        }
        return false;
    }

    /**
     * Hard delete (permanently remove) an AI conversation - use with caution
     */
    public boolean hardDeleteAIConversation(Long id) {
        try {
            aiConversationRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert AIConversation entity to AIConversationResponseDTO
     */
    private AIConversationResponseDTO convertToResponseDTO(AIConversation aiConversation) {
        AIConversationResponseDTO dto = new AIConversationResponseDTO();
        dto.setId(aiConversation.getId());
        dto.setMessageId(aiConversation.getMessageId());
        dto.setLastRepliedAt(aiConversation.getLastRepliedAt());
        dto.setHandedOverByAI(aiConversation.isHandedOverByAI());
        return dto;
    }
}

