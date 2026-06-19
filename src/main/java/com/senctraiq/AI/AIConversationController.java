//package com.senctraiq.AI;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/v1/ai-conversations")
//@CrossOrigin(origins = "*", maxAge = 3600)
//public class AIConversationController {
//
//    @Autowired
//    private AIConversationService aiConversationService;
//
//    /**
//     * Create a new AI conversation
//     * POST /api/v1/ai-conversations
//     */
//    @PostMapping
//    public ResponseEntity<AIConversationResponseDTO> createAIConversation(@RequestBody AIConversationRequestDTO requestDTO) {
//        AIConversationResponseDTO response = aiConversationService.createAIConversation(requestDTO);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    /**
//     * Get all active AI conversations
//     * GET /api/v1/ai-conversations
//     */
//    @GetMapping
//    public ResponseEntity<List<AIConversationResponseDTO>> getAllAIConversations() {
//        List<AIConversationResponseDTO> conversations = aiConversationService.getAllAIConversations();
//        return ResponseEntity.ok(conversations);
//    }
//
//    /**
//     * Get AI conversation by ID
//     * GET /api/v1/ai-conversations/{id}
//     */
//    @GetMapping("/{id}")
//    public ResponseEntity<AIConversationResponseDTO> getAIConversationById(@PathVariable Long id) {
//        Optional<AIConversationResponseDTO> conversation = aiConversationService.getAIConversationById(id);
//        return conversation.map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    /**
//     * Get AI conversation by message ID
//     * GET /api/v1/ai-conversations/message/{messageId}
//     */
//    @GetMapping("/message/{messageId}")
//    public ResponseEntity<AIConversationResponseDTO> getAIConversationByMessageId(@PathVariable String messageId) {
//        Optional<AIConversationResponseDTO> conversation = aiConversationService.getAIConversationByMessageId(messageId);
//        return conversation.map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    /**
//     * Get all conversations handed over by AI to human
//     * GET /api/v1/ai-conversations/handed-over
//     */
//    @GetMapping("/handed-over")
//    public ResponseEntity<List<AIConversationResponseDTO>> getHandedOverConversations() {
//        List<AIConversationResponseDTO> conversations = aiConversationService.getHandedOverConversations();
//        return ResponseEntity.ok(conversations);
//    }
//
//    /**
//     * Get all ongoing AI conversations (not handed over)
//     * GET /api/v1/ai-conversations/ongoing
//     */
//    @GetMapping("/ongoing")
//    public ResponseEntity<List<AIConversationResponseDTO>> getOngoingConversations() {
//        List<AIConversationResponseDTO> conversations = aiConversationService.getOngoingConversations();
//        return ResponseEntity.ok(conversations);
//    }
//
//    /**
//     * Update an AI conversation
//     * PUT /api/v1/ai-conversations/{id}
//     */
//    @PutMapping("/{id}")
//    public ResponseEntity<AIConversationResponseDTO> updateAIConversation(
//            @PathVariable Long id,
//            @RequestBody AIConversationRequestDTO requestDTO) {
//        AIConversationResponseDTO updated = aiConversationService.updateAIConversation(id, requestDTO);
//        if (updated != null) {
//            return ResponseEntity.ok(updated);
//        }
//        return ResponseEntity.notFound().build();
//    }
//
//    /**
//     * Mark AI conversation as handed over to human
//     * PUT /api/v1/ai-conversations/{id}/handover
//     */
//    @PutMapping("/{id}/handover")
//    public ResponseEntity<AIConversationResponseDTO> handoverToHuman(@PathVariable Long id) {
//        AIConversationResponseDTO updated = aiConversationService.handoverToHuman(id);
//        if (updated != null) {
//            return ResponseEntity.ok(updated);
//        }
//        return ResponseEntity.notFound().build();
//    }
//
//    /**
//     * Soft delete an AI conversation (marks as deleted, does not remove from database)
//     * DELETE /api/v1/ai-conversations/{id}
//     */
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Map<String, Object>> deleteAIConversation(@PathVariable Long id) {
//        boolean deleted = aiConversationService.deleteAIConversation(id);
//        Map<String, Object> response = new HashMap<>();
//        if (deleted) {
//            response.put("success", true);
//            response.put("message", "AI conversation soft deleted successfully");
//            return ResponseEntity.ok(response);
//        } else {
//            response.put("success", false);
//            response.put("message", "AI conversation not found");
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    /**
//     * Hard delete an AI conversation (permanently remove from database)
//     * WARNING: This action is irreversible
//     * DELETE /api/v1/ai-conversations/{id}/permanent
//     */
//    @DeleteMapping("/{id}/permanent")
//    public ResponseEntity<Map<String, Object>> hardDeleteAIConversation(@PathVariable Long id) {
//        boolean deleted = aiConversationService.hardDeleteAIConversation(id);
//        Map<String, Object> response = new HashMap<>();
//        if (deleted) {
//            response.put("success", true);
//            response.put("message", "AI conversation permanently deleted");
//            return ResponseEntity.ok(response);
//        } else {
//            response.put("success", false);
//            response.put("message", "Failed to delete AI conversation");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }
//}
//
