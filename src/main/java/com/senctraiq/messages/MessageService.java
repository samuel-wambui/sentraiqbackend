package com.senctraiq.messages;

import com.senctraiq.AI.AIConversation;
import com.senctraiq.AI.AIConversationRepository;
import com.senctraiq.AI.sentiment.Sentiments;
import com.senctraiq.AI.sentiment.SentimentsRepository;
import com.senctraiq.n8n.AiFullConversationDTO;
import com.senctraiq.n8n.AiHandoverToHuman;
import com.senctraiq.n8n.ConversationContinuationDTO;
import com.senctraiq.ticketbuilder.Ticket;
import com.senctraiq.ticketbuilder.TicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessagesRepository messagesRepository;
    @Autowired
    private TicketRepo ticketRepo;
    @Autowired
    private SentimentsRepository sentimentRepo;
    @Autowired
    private AIConversationRepository aiConversationRepository;
    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public Message createMessage(AiFullConversationDTO requestDTO) {
        rejectDuplicateMessage(requestDTO.getCustomerMessageId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime activeFrom = now.minusHours(48);
        LocalDateTime receivedAt = parsePayloadTime(requestDTO.getReceivedAt(), now);
        LocalDateTime repliedAt = parsePayloadTime(requestDTO.getRepliedAt(), receivedAt);

        Optional<Ticket> activeTicket = findActiveTicketBySenderId(requestDTO.getSenderId(), activeFrom);
        Message message = new Message();
        message.setSource(requestDTO.getSource());
        message.setMessageId(requestDTO.getCustomerMessageId());
        message.setSenderId(requestDTO.getSenderId());
        message.setSenderName(requestDTO.getSenderName());
        message.setRecipientId(requestDTO.getRecipientId());
        message.setReply(requestDTO.getAgentAnswer());
        message.setRepliedBy("AI");
        createAIConversation(message);

        if (activeTicket.isPresent()) {
            message.setConversationContinuation(true);
            updateTicket(activeTicket.get());
        } else {
            message.setConversationContinuation(false);
            createTicket(message, receivedAt, repliedAt);
        }
        Optional<Sentiments> optionalSentiments = sentimentRepo.findByMessageId(requestDTO.getCustomerMessageId());
        if (optionalSentiments.isEmpty()) {
            createSentiment(requestDTO.getCustomerMessageId(), requestDTO.getCategory(), requestDTO.getSentiment());
        }
        message.setMessage(requestDTO.getCustomerMessage());
        message.setCreatedAt(receivedAt);
        message.setRepliedAt(repliedAt);
        return messagesRepository.save(message);
    }



    @Transactional
    public Message createConversationCont(ConversationContinuationDTO requestDTO) {
        rejectDuplicateMessage(requestDTO.getCustomerMessageId());
        LocalDateTime now = LocalDateTime.now();
        Optional<Ticket> activeTicket = findActiveTicketBySenderId(requestDTO.getSenderId());

        Message message = new Message();
        message.setSource(requestDTO.getSource());
        message.setMessageId(requestDTO.getCustomerMessageId());
        message.setSenderId(requestDTO.getSenderId());
        message.setSenderName(requestDTO.getSenderName());
        message.setRecipientId(requestDTO.getRecipientId());
        message.setMessage(requestDTO.getCustomerMessage());
        message.setConversationContinuation(activeTicket.isPresent());
        message.setCreatedAt(now);

        if (activeTicket.isPresent()) {
            updateTicket(activeTicket.get());
        } else {
            createTicket(message, now, now);
        }
        return messagesRepository.save(message);
    }

    public void createTicket(Message message, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setMessageId(message.getMessageId());
        ticket.setSenderId(message.getSenderId());
        ticket.setTicketCreatedAt(createdAt);
        ticket.setTicketUpdatedAt(updatedAt);
        ticketRepo.save(ticket);
    }


    public void createAIConversation(Message message) {
        AIConversation aiConversation = new AIConversation();
        aiConversation.setMessageId(message.getMessageId());
        aiConversation.setLastRepliedAt(LocalDateTime.now());
        aiConversation.setHandedOverByAI(false);
        aiConversation.setDeleted(false);
        aiConversationRepository.save(aiConversation);
    }

    @Transactional
    public Message messageHandOverFromAI(AiHandoverToHuman requestDTO) {
        rejectDuplicateMessage(requestDTO.getMessageId());
        Message message = new Message();
        LocalDateTime now = LocalDateTime.now();
        Optional<Ticket> activeTicket = findActiveTicketBySenderId(requestDTO.getSenderId(), now.minusHours(48));
        if (activeTicket.isPresent()) {
            message.setSource(requestDTO.getSource());
            message.setMessageId(requestDTO.getMessageId());
            message.setSenderId(requestDTO.getSenderId());
            message.setSenderName(requestDTO.getSenderName());
            message.setRecipientId(requestDTO.getRecipientId());
            message.setMessage(requestDTO.getMessage());
            message.setCreatedAt(requestDTO.getReceivedAt());
            updateTicket(activeTicket.get());
            message.setConversationContinuation(true);
        } else {
            message.setSource(requestDTO.getSource());
            message.setMessageId(requestDTO.getMessageId());
            message.setSenderId(requestDTO.getSenderId());
            message.setSenderName(requestDTO.getSenderName());
            message.setRecipientId(requestDTO.getRecipientId());
            message.setMessage(requestDTO.getMessage());
            message.setCreatedAt(requestDTO.getReceivedAt());
            createTicket(message, requestDTO.getReceivedAt(), now);
            message.setConversationContinuation(false);
        }
        createSentiment(requestDTO.getMessageId(), requestDTO.getCategory(), requestDTO.getSentiment());
        message.setCreatedAt(now);
        return messagesRepository.save(message);
    }

   public void createSentiment(String  messageId, String category, String sentiments) {

        Sentiments sentiment = new Sentiments();
        sentiment.setMessageId(messageId);
        sentiment.setCategory(category);
        sentiment.setSentiment(sentiments);

       sentimentRepo.save(sentiment);
   }


    public String generateTicketNumber() {
        String ticketNumber;
        do {
            ticketNumber = "TICK-" + generateRandomCode();
        } while (ticketRepo.existsByTicketNumber(ticketNumber));
        return ticketNumber;
    }
    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARACTERS.charAt(
                    RANDOM.nextInt(CHARACTERS.length())
            ));
        }
        return sb.toString();
    }


    public Optional<Ticket> findActiveTicketBySenderId(String senderId) {
         return findActiveTicketBySenderId(senderId, LocalDateTime.now().minusHours(48));
    }


    private Optional<Ticket> findActiveTicketBySenderId(String senderId, LocalDateTime activeFrom) {
        if (senderId == null || senderId.isBlank()) {
            return Optional.empty();
        }
        java.util.List<Ticket> activeTickets = ticketRepo.findActiveTicketsBySenderId(senderId, activeFrom);
        if (activeTickets.isEmpty()) {
            return Optional.empty();
        }
        deactivateDuplicateActiveTickets(activeTickets);
        return Optional.of(activeTickets.get(0));
    }


    private void deactivateDuplicateActiveTickets(java.util.List<Ticket> activeTickets) {
        if (activeTickets.size() <= 1) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i < activeTickets.size(); i++) {
            Ticket duplicateTicket = activeTickets.get(i);
            duplicateTicket.setTicketDeleted(true);
            duplicateTicket.setTicketUpdatedAt(now);
        }
        ticketRepo.saveAll(activeTickets.subList(1, activeTickets.size()));
    }

    private void updateTicket(Ticket ticket) {
        ticket.setTicketUpdatedAt(LocalDateTime.now());
        ticketRepo.save(ticket);
    }

    private void rejectDuplicateMessage(String messageId) {
        if (messageId != null && messagesRepository.existsByMessageId(messageId)) {
            throw new DuplicateMessageException(messageId);
        }
    }

    private LocalDateTime parsePayloadTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // Try offset/Z timestamps sent by n8n and browser clients.
        }

        try {
            return OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }
}
