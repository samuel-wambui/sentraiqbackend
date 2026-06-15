package com.senctraiq.conversations;

import com.senctraiq.ApiResponse.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "http://localhost:8080")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping({"", "/", "/getConversation"})
    public ResponseEntity<ApiResponse<List<ConversationReferenceResponse>>> getConversations(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String assigned
    ) {
        ApiResponse<List<ConversationReferenceResponse>> response = new ApiResponse<>();

        try {
            List<ConversationReferenceResponse> conversations = conversationService.getConversations(source, status, category, assigned);
            response.setEntity(conversations);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversations retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve conversations: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<ConversationChannelSummary>>> getChannelSummary() {
        ApiResponse<List<ConversationChannelSummary>> response = new ApiResponse<>();

        try {
            List<ConversationChannelSummary> summary = conversationService.getChannelSummary();
            response.setEntity(summary);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation channel summary retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve conversation channel summary: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<ConversationReportsResponse>> getReports(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        ApiResponse<ConversationReportsResponse> response = new ApiResponse<>();

        try {
            ConversationReportsResponse reports = conversationService.getReports(source, sentiment, category, username, from, to);
            response.setEntity(reports);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation reports retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve conversation reports: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/ai-agent/active")
    public ResponseEntity<ApiResponse<List<ConversationReferenceResponse>>> getActiveAiAgentConversations(
            @RequestParam(required = false) String answeredBy,
            @RequestParam(required = false) String source
    ) {
        ApiResponse<List<ConversationReferenceResponse>> response = new ApiResponse<>();

        try {
            List<ConversationReferenceResponse> conversations = conversationService.getActiveConversationsHandledByAgent(
                    answeredBy,
                    source
            );
            response.setEntity(conversations);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Active conversations handled by AI agent retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve active AI-agent conversations: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ConversationMessageResponse>>> getMessages(
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        ApiResponse<List<ConversationMessageResponse>> response = new ApiResponse<>();

        try {
            List<ConversationMessageResponse> messages = conversationService.getMessages(
                    conversationId,
                    source,
                    status,
                    category
            );
            response.setEntity(messages);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation messages retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve conversation messages: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/threads")
    public ResponseEntity<ApiResponse<List<ConversationThreadResponse>>> getConversationThreads(
            @RequestParam(required = false) String agentUsername,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        ApiResponse<List<ConversationThreadResponse>> response = new ApiResponse<>();

        try {
            List<ConversationThreadResponse> threads = conversationService.getConversationThreads(
                    agentUsername != null ? agentUsername : username,
                    customerId,
                    source,
                    status,
                    category,
                    from,
                    to
            );
            response.setEntity(threads);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation threads retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve conversation threads: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/threads/summary")
    public ResponseEntity<ApiResponse<List<ConversationChannelSummary>>> getConversationThreadChannelSummary(
            @RequestParam(required = false) String agentUsername,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        ApiResponse<List<ConversationChannelSummary>> response = new ApiResponse<>();

        try {
            List<ConversationChannelSummary> summary = conversationService.getConversationThreadChannelSummary(
                    agentUsername != null ? agentUsername : username,
                    customerId,
                    source,
                    status,
                    category,
                    from,
                    to
            );
            response.setEntity(summary);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation thread summary retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve conversation thread summary: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/agents/{username}/threads")
    public ResponseEntity<ApiResponse<List<ConversationThreadResponse>>> getAgentConversationThreads(
            @PathVariable String username,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return getConversationThreads(username, null, customerId, source, status, category, from, to);
    }

    @GetMapping("/customers/{customerId}/threads")
    public ResponseEntity<ApiResponse<List<ConversationThreadResponse>>> getCustomerConversationThreads(
            @PathVariable String customerId,
            @RequestParam(required = false) String agentUsername,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return getConversationThreads(agentUsername, username, customerId, source, status, category, from, to);
    }

    @GetMapping("/customers/{customerId}/active-status")
    public ResponseEntity<ApiResponse<ConversationCustomerActiveStatusResponse>> getCustomerActiveStatus(
            @PathVariable String customerId,
            @RequestParam(required = false) String source
    ) {
        ApiResponse<ConversationCustomerActiveStatusResponse> response = new ApiResponse<>();

        try {
            ConversationCustomerActiveStatusResponse status = conversationService.getCustomerActiveStatus(customerId, source);
            response.setEntity(status);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Customer active conversation status retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve customer active conversation status: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<ConversationMessageResponse>>> getConversationMessages(
            @PathVariable Long conversationId
    ) {
        ApiResponse<List<ConversationMessageResponse>> response = new ApiResponse<>();

        try {
            List<ConversationMessageResponse> messages = conversationService.getConversationMessages(conversationId);
            response.setEntity(messages);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation messages retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve conversation messages: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{conversationId}/reply")
    public ResponseEntity<ApiResponse<ConversationMessageResponse>> replyToConversation(
            @PathVariable Long conversationId,
            @RequestBody ConversationReplyRequest request
    ) {
        ApiResponse<ConversationMessageResponse> response = new ApiResponse<>();

        try {
            ConversationMessageResponse message = conversationService.replyToConversation(
                    conversationId,
                    request,
                    getCurrentUsername()
            );
            response.setEntity(message);
            response.setStatusCode(HttpStatus.CREATED.value());
            response.setMessage("Conversation reply sent successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException ise) {
            response.setStatusCode(HttpStatus.BAD_GATEWAY.value());
            response.setMessage(ise.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to send conversation reply: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping({"/{conversationId}/agent-answer", "/{conversationId}/ai-answer"})
    public ResponseEntity<ApiResponse<ConversationMessageResponse>> recordAiAnswer(
            @PathVariable Long conversationId,
            @RequestBody ConversationAiAnswerRequest request
    ) {
        ApiResponse<ConversationMessageResponse> response = new ApiResponse<>();

        try {
            ConversationMessageResponse message = conversationService.recordAiAnswer(conversationId, request);
            response.setEntity(message);
            response.setStatusCode(HttpStatus.CREATED.value());
            response.setMessage("Agent answer recorded successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException ise) {
            response.setStatusCode(HttpStatus.CONFLICT.value());
            response.setMessage(ise.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to record agent answer: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping({"/full", "/ai-agent/conversation"})
    public ResponseEntity<ApiResponse<ConversationAiConversationResponse>> recordAiConversation(
            @RequestBody ConversationAiConversationRequest request
    ) {
        ApiResponse<ConversationAiConversationResponse> response = new ApiResponse<>();

        try {
            ConversationAiConversationResponse conversation = conversationService.recordAiConversation(request);
            response.setEntity(conversation);
            response.setStatusCode(HttpStatus.CREATED.value());
            response.setMessage("AI conversation recorded successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException ise) {
            response.setStatusCode(HttpStatus.CONFLICT.value());
            response.setMessage(ise.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to record AI conversation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping({"/continuation", "/continuations"})
    public ResponseEntity<ApiResponse<ConversationMessageResponse>> recordContinuationMessage(
            @RequestBody ConversationContinuationMessageRequest request
    ) {
        ApiResponse<ConversationMessageResponse> response = new ApiResponse<>();

        try {
            ConversationMessageResponse message = conversationService.recordContinuationMessage(request);
            response.setEntity(message);
            response.setStatusCode(HttpStatus.CREATED.value());
            response.setMessage("Conversation continuation message recorded successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException ise) {
            response.setStatusCode(HttpStatus.CONFLICT.value());
            response.setMessage(ise.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to record conversation continuation message: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/incoming")
    public ResponseEntity<ApiResponse<ConversationReferenceResponse>> receiveMessage(@RequestBody IncomingMessageRequest request) {
        ApiResponse<ConversationReferenceResponse> response = new ApiResponse<>();

        try {
            ConversationReferenceResponse conversation = conversationService.handleIncomingMessage(request);

            response.setEntity(conversation);
            response.setStatusCode(HttpStatus.CREATED.value());
            response.setMessage("Conversation message received successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException ise) {
            response.setStatusCode(HttpStatus.CONFLICT.value());
            response.setMessage(ise.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to receive conversation message: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{conversationId}/assign")
    public ResponseEntity<ApiResponse<ConversationReferenceResponse>> assignConversation(
            @PathVariable Long conversationId,
            @RequestBody(required = false) ConversationActionRequest request
    ) {
        ApiResponse<ConversationReferenceResponse> response = new ApiResponse<>();

        try {
            ConversationReferenceResponse conversation = conversationService.assignConversation(
                    conversationId,
                    request,
                    getCurrentUsername()
            );
            response.setEntity(conversation);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation assigned successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to assign conversation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{conversationId}/handover")
    public ResponseEntity<ApiResponse<ConversationReferenceResponse>> handoverConversation(
            @PathVariable Long conversationId,
            @RequestBody(required = false) ConversationHandoverRequest request
    ) {
        ApiResponse<ConversationReferenceResponse> response = new ApiResponse<>();

        try {
            ConversationReferenceResponse conversation = conversationService.handoverConversation(
                    conversationId,
                    request,
                    getCurrentUsername()
            );
            response.setEntity(conversation);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation handed over successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to hand over conversation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{conversationId}/ai-agent-handover")
    public ResponseEntity<ApiResponse<ConversationReferenceResponse>> setAiAgentHandover(
            @PathVariable Long conversationId,
            @RequestBody(required = false) ConversationAiHandoverRequest request
    ) {
        ApiResponse<ConversationReferenceResponse> response = new ApiResponse<>();

        try {
            ConversationReferenceResponse conversation = conversationService.setAiAgentHandover(conversationId, request);
            response.setEntity(conversation);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation AI-agent handover flag updated successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update AI-agent handover flag: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{conversationId}/ai-agent-handover/status")
    public ResponseEntity<ApiResponse<ConversationAiHandoverStatusResponse>> getAiAgentHandoverStatus(
            @PathVariable Long conversationId
    ) {
        ApiResponse<ConversationAiHandoverStatusResponse> response = new ApiResponse<>();

        try {
            ConversationAiHandoverStatusResponse status = conversationService.getAiAgentHandoverStatus(conversationId);
            response.setEntity(status);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation AI-agent handover status retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve AI-agent handover status: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{conversationId}/pending-customer")
    public ResponseEntity<ApiResponse<ConversationReferenceResponse>> markPendingCustomer(
            @PathVariable Long conversationId,
            @RequestBody(required = false) ConversationActionRequest request
    ) {
        ApiResponse<ConversationReferenceResponse> response = new ApiResponse<>();

        try {
            ConversationReferenceResponse conversation = conversationService.markPendingCustomer(
                    conversationId,
                    request,
                    getCurrentUsername()
            );
            response.setEntity(conversation);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation marked pending customer");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to mark conversation pending customer: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{conversationId}/close")
    public ResponseEntity<ApiResponse<ConversationReferenceResponse>> closeConversation(
            @PathVariable Long conversationId,
            @RequestBody(required = false) ConversationCloseRequest request
    ) {
        ApiResponse<ConversationReferenceResponse> response = new ApiResponse<>();

        try {
            ConversationReferenceResponse conversation = conversationService.closeConversation(
                    conversationId,
                    request,
                    getCurrentUsername()
            );
            response.setEntity(conversation);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Conversation closed successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to close conversation: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;

        String name = authentication.getName();
        if (name == null || "anonymousUser".equalsIgnoreCase(name)) return null;
        return name;
    }
}
