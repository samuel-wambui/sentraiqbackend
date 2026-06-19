package com.senctraiq.messages;

import com.senctraiq.AI.sentiment.Sentiments;
import com.senctraiq.AI.sentiment.SentimentsRepository;
import com.senctraiq.ApiResponse.ApiResponse;
import com.senctraiq.n8n.AiFullConversationDTO;
import com.senctraiq.n8n.AiHandoverToHuman;
import com.senctraiq.n8n.ConversationContinuationDTO;
import com.senctraiq.ticketbuilder.Ticket;
import com.senctraiq.ticketbuilder.TicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/messages")
@CrossOrigin(originPatterns = "*", maxAge = 3600)
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private TicketRepo ticketRepo;
    @Autowired
    private SentimentsRepository sentimentRepo;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> createMessage(@RequestBody AiFullConversationDTO requestDTO) {
        try {
            Message savedMessage = messageService.createMessage(requestDTO);
            MessageResponseDTO responseDTO = convertToResponseDTO(savedMessage);

            ApiResponse<MessageResponseDTO> apiResponse = new ApiResponse<>();
            apiResponse.setMessage("Message created successfully");
            apiResponse.setStatusCode(201);
            apiResponse.setEntity(responseDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (DuplicateMessageException ex) {
            return duplicateMessageResponse(ex);
        }
    }

    @PostMapping("/conversation-continuation")
    public ResponseEntity<ApiResponse<MessageResponseDTO>> createConversationContinuation(
            @RequestBody ConversationContinuationDTO requestDTO
    ) {
        try {
            ApiResponse<MessageResponseDTO> apiResponse = new ApiResponse<>();
            Message savedMessage = messageService.createConversationCont(requestDTO);
            MessageResponseDTO responseDTO = convertToResponseDTO(savedMessage, requestDTO.getConversationId());

            apiResponse.setMessage("Conversation continuation message created successfully");
            apiResponse.setStatusCode(201);
            apiResponse.setEntity(responseDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (DuplicateMessageException ex) {
            return duplicateMessageResponse(ex);
        }
    }

    private ResponseEntity<ApiResponse<MessageResponseDTO>> duplicateMessageResponse(DuplicateMessageException ex) {
        ApiResponse<MessageResponseDTO> apiResponse = new ApiResponse<>();
        apiResponse.setMessage(ex.getMessage());
        apiResponse.setStatusCode(HttpStatus.CONFLICT.value());
        apiResponse.setEntity(null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
    }

    @PostMapping("/hand-over-from-ai")
    public ResponseEntity<ApiResponse<Message>> handledOverFromAi  (@RequestBody AiHandoverToHuman requestDTO) {
        ApiResponse<Message> apiResponse = new ApiResponse<>();
        try {
            Message savedMessage = messageService.messageHandOverFromAI(requestDTO);
            apiResponse.setMessage("Message handed over from AI successfully");
            apiResponse.setStatusCode(201);
            apiResponse.setEntity(savedMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (DuplicateMessageException ex) {
            apiResponse.setMessage(ex.getMessage());
            apiResponse.setStatusCode(HttpStatus.CONFLICT.value());
            apiResponse.setEntity(null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
        }
    }



    private MessageResponseDTO convertToResponseDTO(Message message) {
        return convertToResponseDTO(message, null);
    }

    private MessageResponseDTO convertToResponseDTO(Message message, String ticketNumberHint) {
        MessageResponseDTO dto = new MessageResponseDTO();
        dto.setId(message.getId());
        dto.setSource(message.getSource());
        dto.setMessageId(message.getMessageId());
        dto.setSenderId(message.getSenderId());
        dto.setSenderName(message.getSenderName());
        dto.setRecipientId(message.getRecipientId());
        dto.setMessage(message.getMessage());
        dto.setReply(message.getReply());
        Optional<Ticket> optionalTicket = findResponseTicket(message, ticketNumberHint);
        optionalTicket.ifPresent(ticket -> dto.setTicketNumber(ticket.getTicketNumber()));
        Optional<Sentiments> optionalSentiment = sentimentRepo.findByMessageId(message.getMessageId());
        optionalSentiment.ifPresent(sentiment -> {
            dto.setCategory(sentiment.getCategory());
            dto.setSentiment(sentiment.getSentiment());
        });
        dto.setConversationContinuation(message.isConversationContinuation());
        dto.setRepliedBy(message.getRepliedBy());
        dto.setRepliedAt(message.getRepliedAt());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setDeleted(message.isDeleted());
        return dto;
    }

    private Optional<Ticket> findResponseTicket(Message message, String ticketNumberHint) {
        Optional<Ticket> senderTicket = messageService.findActiveTicketBySenderId(message.getSenderId());
        if (senderTicket.isPresent()) {
            return senderTicket;
        }
        if (ticketNumberHint != null && !ticketNumberHint.isBlank()) {
            Optional<Ticket> ticket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumberHint, false);
            if (ticket.isPresent()) {
                return ticket;
            }
            ticket = ticketRepo.findByMessageIdAndTicketDeleted(ticketNumberHint, false);
            if (ticket.isPresent()) {
                return ticket;
            }
        }
        return ticketRepo.findByMessageIdAndTicketDeleted(message.getMessageId(), false);
    }
}
