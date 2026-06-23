package com.senctraiq.ticketbuilder;

import com.senctraiq.AI.AIConversation;
import com.senctraiq.AI.AIConversationRepository;
import com.senctraiq.AI.sentiment.Sentiments;
import com.senctraiq.AI.sentiment.SentimentsRepository;
import com.senctraiq.banksupport.BankSupport;
import com.senctraiq.banksupport.BankSupportRepo;
import com.senctraiq.messages.Message;
import com.senctraiq.messages.MessagesRepository;
import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketService {
    @Autowired
    private TicketRepo ticketRepo;
    @Autowired
    private MessagesRepository messagesRepository;
    @Autowired
    private SentimentsRepository sentimentsRepository;
    @Autowired
    private BankSupportRepo bankSupportRepo;
    @Autowired
    private AIConversationRepository aiConversationRepository;
    @Autowired
    private UserRepository userRepository;
    @Value("${app.tickets.reply-webhook-url:https://n8n.digitalbank365.com/webhook/ReplyMeta}")
    private String replyWebhookUrl;

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long ACTIVE_TICKET_WINDOW_HOURS = 48;

    public Ticket assignTicketToSupport(String ticketNumber, String username) {
        Optional<Ticket> optionalTicket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if(optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();
            ticket.setTicketAssignedTo(username);
            ticket.setTicketStatus("OPEN");
            return ticketRepo.save(ticket);
        }
        return null;
    }

    public Ticket handoverTicket(String ticketNumber, String newAssignee, String handoverBy, String handoverNote) {
        Optional<Ticket> optionalTicket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if(optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();
            ticket.setTicketAssignedTo(newAssignee);
            ticket.setTicketHandoverTime(LocalDateTime.now());
            ticket.setTicketHandoverBy(handoverBy);
            ticket.setHandoverNote(handoverNote);
            ticket.setTicketHandedOver(true);
            return ticketRepo.save(ticket);
        }
        return null;
    }
    public Ticket checkTicketHandOverToHuman(String senderId) {
        if (senderId == null || senderId.isBlank()) {
            return null;
        }
        List<Ticket> activeTickets = ticketRepo.findActiveTicketsBySenderId(senderId, LocalDateTime.now().minusHours(48));
        if (activeTickets.isEmpty()) {
            return null;
        }
        deactivateDuplicateActiveTickets(activeTickets);
        return activeTickets.get(0);
    }
    public Ticket closeTicket(String ticketNumber) {
        Optional<Ticket> optionalTicket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if(optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();
            ticket.setTicketStatus("CLOSED");
            ticket.setTicketClosed(true);
            ticket.setTicketClosedAt(LocalDateTime.now());
            ticket.setTicketUpdatedAt(ticket.getTicketClosedAt());
            return ticketRepo.save(ticket);
        }
        return null;
    }

    @Transactional
    public Optional<TicketResponseDTO> closeTicket(
            String ticketNumber,
            TicketCloseRequestDTO requestDTO,
            String authenticatedUsername
    ) {
        if (ticketNumber == null || ticketNumber.isBlank() || requestDTO == null
                || requestDTO.getClosingRemarks() == null || requestDTO.getClosingRemarks().isBlank()) {
            return Optional.empty();
        }

        Optional<Ticket> optionalTicket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if (optionalTicket.isEmpty()) {
            return Optional.empty();
        }

        Ticket ticket = optionalTicket.get();
        if (ticket.isTicketClosed()) {
            throw new IllegalStateException("Ticket is already closed");
        }

        LocalDateTime now = LocalDateTime.now();
        BankSupport activeSupport = bankSupportRepo.findFirstByTicketNumberAndDeletedAndHandOverAtIsNullOrderByTicketAssignAtDescIdDesc(ticketNumber, false);
        if (activeSupport != null) {
            activeSupport.setHandOverAt(now);
            bankSupportRepo.save(activeSupport);
        }

        ticket.setTicketStatus("CLOSED");
        ticket.setTicketClosed(true);
        ticket.setTicketClosedAt(now);
        ticket.setTicketUpdatedAt(now);
        ticket.setTicketPending(false);
        ticket.setTicketAssignedTo(null);
        ticket.setTicketHandedOver(false);
        ticket.setClosingRemarks(requestDTO.getClosingRemarks().trim());
        ticket.setTicketClosedBy(firstNonBlank(authenticatedUsername, requestDTO.getClosedBy()));
        ticketRepo.save(ticket);

        return Optional.of(buildTicketResponse(ticket));
    }

    public Optional<TicketResponseDTO> getTicketResponseByTicketNumber(String ticketNumber) {
        if (ticketNumber == null || ticketNumber.isBlank()) {
            return Optional.empty();
        }
        return ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false)
                .map(this::buildTicketResponse);
    }

    public List<TicketResponseDTO> getAllTickets() {
        return toTicketResponses(ticketRepo.findAllByTicketDeletedOrderByTicketUpdatedAtDescIdDesc(false));
    }

    public List<TicketResponseDTO> getOpenTickets() {
        return toTicketResponses(ticketRepo.findAllByTicketDeletedAndTicketClosedOrderByTicketUpdatedAtDescIdDesc(false, false));
    }

    public List<TicketResponseDTO> getClosedTickets() {
        return toTicketResponses(ticketRepo.findAllByTicketDeletedAndTicketClosedOrderByTicketUpdatedAtDescIdDesc(false, true));
    }

    public Map<String, List<TicketResponseDTO>> getNewTicketsByChannel() {
        return ticketRepo.findAllByTicketDeletedAndTicketClosedAndTicketHandedOverOrderByTicketUpdatedAtDescIdDesc(false, false, false)
                .stream()
                .filter(ticket -> !hasActiveSupportAssignment(ticket))
                .map(this::buildTicketResponse)
                .collect(Collectors.groupingBy(
                        this::resolveChannel,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public TicketHandoverQueuesResponseDTO getHandedOverTicketQueues() {
        TicketHandoverQueuesResponseDTO dto = new TicketHandoverQueuesResponseDTO();
        dto.setHandedOverByAi(getTicketsHandedOverByAi());
        dto.setHandedOverByBankSupport(getTicketsHandedOverByBankSupport());
        return dto;
    }

    public List<TicketResponseDTO> getTicketsHandedOverByAi() {
        return toTicketResponses(ticketRepo.findAllByTicketDeletedAndTicketClosedAndTicketHandedOverOrderByTicketUpdatedAtDescIdDesc(false, false, true)
                .stream()
                .filter(ticket -> !hasActiveSupportAssignment(ticket))
                .filter(this::isHandedOverByAi)
                .collect(Collectors.toList()));
    }

    public List<TicketResponseDTO> getTicketsHandedOverByBankSupport() {
        return toTicketResponses(ticketRepo.findAllByTicketDeletedAndTicketClosedAndTicketHandedOverOrderByTicketUpdatedAtDescIdDesc(false, false, true)
                .stream()
                .filter(ticket -> !hasActiveSupportAssignment(ticket))
                .filter(this::isHandedOverByBankSupport)
                .collect(Collectors.toList()));
    }

    public List<TicketResponseDTO> getActiveBankSupportTickets() {
        return toTicketResponses(ticketRepo.findAllByTicketDeletedAndTicketClosedOrderByTicketUpdatedAtDescIdDesc(false, false)
                .stream()
                .filter(this::hasActiveSupportAssignment)
                .collect(Collectors.toList()));
    }

    public Optional<TicketResponseDTO> assignTicketToBankSupport(String ticketNumber, TicketAssignmentRequestDTO requestDTO) {
        if (ticketNumber == null || ticketNumber.isBlank() || requestDTO == null
                || requestDTO.getUsername() == null || requestDTO.getUsername().isBlank()) {
            return Optional.empty();
        }

        Optional<Ticket> optionalTicket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if (optionalTicket.isEmpty()) {
            return Optional.empty();
        }

        Ticket ticket = optionalTicket.get();
        if (ticket.isTicketClosed()) {
            throw new IllegalStateException("Cannot assign a closed ticket");
        }
        if (hasActiveSupportAssignment(ticket) && !ticket.isTicketHandedOver()) {
            throw new IllegalStateException("Ticket is already assigned to bank support");
        }

        BankSupport support = new BankSupport();
        support.setUsername(requestDTO.getUsername());
        support.setTicketNumber(ticket.getTicketNumber());
        support.setMessageId(resolveAssignmentMessageId(ticket, requestDTO));
        support.setTicketAssignAt(LocalDateTime.now());
        bankSupportRepo.save(support);

        ticket.setTicketAssignedTo(requestDTO.getUsername());
        ticket.setTicketStatus("OPEN");
        ticket.setTicketPending(false);
        ticket.setTicketHandedOver(false);
        ticket.setTicketUpdatedAt(LocalDateTime.now());
        ticketRepo.save(ticket);

        return Optional.of(buildTicketResponse(ticket));
    }

    public Optional<TicketResponseDTO> handOverTicketByBankSupport(String ticketNumber, TicketHandoverRequestDTO requestDTO) {
        return handOverTicketByBankSupport(ticketNumber, requestDTO, null);
    }

    @Transactional
    public Optional<TicketResponseDTO> handOverTicketByBankSupport(
            String ticketNumber,
            TicketHandoverRequestDTO requestDTO,
            String authenticatedUsername
    ) {
        if (ticketNumber == null || ticketNumber.isBlank()) {
            return Optional.empty();
        }
        Optional<Ticket> optionalTicket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if (optionalTicket.isEmpty()) {
            return Optional.empty();
        }

        Ticket ticket = optionalTicket.get();
        if (ticket.isTicketClosed()) {
            throw new IllegalStateException("Cannot hand over a closed ticket");
        }

        BankSupport support = bankSupportRepo.findFirstByTicketNumberAndDeletedAndHandOverAtIsNullOrderByTicketAssignAtDescIdDesc(ticketNumber, false);
        String handoverBy = firstNonBlank(
                authenticatedUsername,
                support != null ? support.getUsername() : null,
                ticket.getTicketAssignedTo(),
                requestDTO != null ? requestDTO.getHandoverBy() : null
        );

        if (support == null && (ticket.getTicketAssignedTo() == null || ticket.getTicketAssignedTo().isBlank())) {
            throw new IllegalStateException("Only active bank support tickets can be handed over by bank support");
        }
        if (handoverBy == null) {
            throw new IllegalStateException("Bank support username is required for handover");
        }

        LocalDateTime now = LocalDateTime.now();
        if (support != null) {
            if (requestDTO != null && requestDTO.getMessageId() != null && !requestDTO.getMessageId().isBlank()) {
                support.setMessageId(requestDTO.getMessageId());
            }
            support.setUsername(handoverBy);
            support.setHandOverAt(now);
            bankSupportRepo.save(support);
        } else {
            BankSupport handoverSupport = new BankSupport();
            handoverSupport.setUsername(handoverBy);
            handoverSupport.setTicketNumber(ticket.getTicketNumber());
            handoverSupport.setMessageId(requestDTO != null && requestDTO.getMessageId() != null && !requestDTO.getMessageId().isBlank()
                    ? requestDTO.getMessageId()
                    : ticket.getMessageId());
            handoverSupport.setTicketAssignAt(now);
            handoverSupport.setHandOverAt(now);
            bankSupportRepo.save(handoverSupport);
        }

        ticket.setTicketHandedOver(true);
        ticket.setTicketHandoverBy("BANK_SUPPORT");
        ticket.setTicketHandoverTime(now);
        ticket.setHandoverNote(requestDTO != null ? requestDTO.getHandoverNote() : null);
        ticket.setTicketAssignedTo(null);
        ticket.setTicketStatus("HANDED_OVER");
        ticket.setTicketUpdatedAt(now);
        ticketRepo.save(ticket);

        return Optional.of(buildTicketResponse(ticket));
    }

    @Transactional
    public Optional<TicketResponseDTO> replyToTicket(String ticketNumber, TicketReplyRequestDTO requestDTO, String authenticatedUsername) {
        if (ticketNumber == null || ticketNumber.isBlank() || requestDTO == null
                || requestDTO.getMessage() == null || requestDTO.getMessage().isBlank()) {
            return Optional.empty();
        }

        Optional<Ticket> optionalTicket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if (optionalTicket.isEmpty()) {
            return Optional.empty();
        }

        Ticket ticket = optionalTicket.get();
        if (ticket.isTicketClosed()) {
            throw new IllegalStateException("Cannot reply to a closed ticket");
        }

        String username = firstNonBlank(authenticatedUsername, requestDTO.getUsername());
        if (username == null) {
            throw new IllegalStateException("Reply username is required");
        }

        Optional<User> optionalUser = userRepository.findByUsernameAndDeleted(username, false);
        String firstName = optionalUser
                .map(User::getFirstName)
                .filter(value -> value != null && !value.isBlank())
                .orElse(firstNonBlank(requestDTO.getFirstName(), username));

        Message latestCustomerMessage = latestTicketCustomerMessage(ticket).orElse(null);
        String replyText = requestDTO.getMessage().trim();
        LocalDateTime now = LocalDateTime.now();
        String replyMessageId = "reply-" + ticket.getTicketNumber() + "-" + UUID.randomUUID();

        sendReplyWebhook(ticket, latestCustomerMessage, replyMessageId, replyText, username, firstName);

        Message replyMessage = new Message();
        replyMessage.setSource(resolveReplySource(ticket, latestCustomerMessage));
        replyMessage.setMessageId(replyMessageId);
        replyMessage.setSenderId(username);
        replyMessage.setSenderName(firstName);
        replyMessage.setSenderType("AGENT");
        replyMessage.setRecipientId(ticket.getSenderId());
        replyMessage.setMessage(replyText);
        replyMessage.setConversationContinuation(true);
        replyMessage.setRepliedBy(username);
        replyMessage.setRepliedByUsername(username);
        replyMessage.setRepliedByFirstName(firstName);
        replyMessage.setRepliedAt(now);
        replyMessage.setCreatedAt(now);
        messagesRepository.save(replyMessage);

        ticket.setTicketStatus("OPEN");
        ticket.setTicketPending(false);
        ticket.setTicketUpdatedAt(now);
        ticketRepo.save(ticket);

        return Optional.of(buildTicketResponse(ticket));
    }



    private void deactivateDuplicateActiveTickets(List<Ticket> activeTickets) {
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

    private List<TicketResponseDTO> toTicketResponses(List<Ticket> tickets) {
        return tickets.stream()
                .map(this::buildTicketResponse)
                .collect(Collectors.toList());
    }

    private TicketResponseDTO buildTicketResponse(Ticket ticket) {
        List<Message> messages = findTicketMessages(ticket);
        List<String> messageIds = messages.stream()
                .map(Message::getMessageId)
                .filter(messageId -> messageId != null && !messageId.isBlank())
                .collect(Collectors.toList());
        List<Sentiments> sentiments = findTicketSentiments(messageIds);
        List<BankSupport> supports = bankSupportRepo.findAllByTicketNumberAndDeletedOrderByTicketAssignAtAscIdAsc(ticket.getTicketNumber(), false);
        List<AIConversation> aiConversations = findTicketAiConversations(messageIds);
        Message latestMessage = latestMessage(messages);
        Message latestRepliedMessage = latestRepliedMessage(messages);
        Message latestAgentMessage = latestAgentMessage(messages);
        Sentiments latestSentiment = latestSentiment(latestMessage, sentiments);
        BankSupport activeSupport = activeSupport(supports);
        BankSupport lastSupport = latestSupport(supports);
        BankSupport lastHandedOverSupport = latestHandedOverSupport(supports);
        AIConversation latestAiConversation = latestAiConversation(aiConversations);

        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setTicketNumber(ticket.getTicketNumber());
        dto.setMessageId(ticket.getMessageId());
        dto.setSenderId(ticket.getSenderId());
        dto.setTicketStatus(ticket.getTicketStatus());
        dto.setTicketAssignedTo(ticket.getTicketAssignedTo());
        dto.setTicketCreatedAt(ticket.getTicketCreatedAt());
        dto.setTicketUpdatedAt(ticket.getTicketUpdatedAt());
        dto.setTicketClosedAt(ticket.getTicketClosedAt());
        dto.setClosingRemarks(ticket.getClosingRemarks());
        dto.setTicketClosedBy(ticket.getTicketClosedBy());
        dto.setTicketHandoverTime(ticket.getTicketHandoverTime());
        dto.setTicketHandoverBy(ticket.getTicketHandoverBy());
        dto.setHandoverNote(ticket.getHandoverNote());
        dto.setRequestedServices(ticket.getRequestedServices());
        dto.setTicketPending(ticket.isTicketPending());
        dto.setTicketClosed(ticket.isTicketClosed());
        dto.setTicketDeleted(ticket.isTicketDeleted());
        dto.setTicketHandedOver(ticket.isTicketHandedOver());
        dto.setOpen(!ticket.isTicketClosed() && !ticket.isTicketDeleted());
        dto.setClosed(ticket.isTicketClosed());
        dto.setQueue(resolveQueue(ticket, activeSupport, lastHandedOverSupport));
        dto.setChannel(resolveChannel(messages));
        dto.setLatestMessageId(latestMessage != null ? latestMessage.getMessageId() : ticket.getMessageId());
        dto.setLatestMessage(latestMessage != null ? latestMessage.getMessage() : null);
        dto.setLatestSenderId(latestMessage != null ? latestMessage.getSenderId() : ticket.getSenderId());
        dto.setLatestSenderName(latestMessage != null ? latestMessage.getSenderName() : null);
        dto.setRecipientId(latestMessage != null ? latestMessage.getRecipientId() : null);
        dto.setLastMessageAt(latestMessageTime(latestMessage, latestRepliedMessage, latestAiConversation, ticket));
        dto.setCategory(latestSentiment != null ? latestSentiment.getCategory() : null);
        dto.setSentiment(latestSentiment != null ? latestSentiment.getSentiment() : null);
        dto.setActiveSupportAssigned(activeSupport != null);
        dto.setActiveSupportUsername(activeSupport != null ? activeSupport.getUsername() : null);
        dto.setActiveSupportAssignedAt(activeSupport != null ? activeSupport.getTicketAssignAt() : null);
        dto.setLastSupportUsername(lastSupport != null ? lastSupport.getUsername() : null);
        dto.setLastSupportHandOverAt(lastHandedOverSupport != null ? lastHandedOverSupport.getHandOverAt() : null);
        dto.setLastAnsweredBy(resolveLastAnsweredBy(latestRepliedMessage, latestAgentMessage, latestAiConversation));
        dto.setLastAnsweredAt(resolveLastAnsweredAt(latestRepliedMessage, latestAgentMessage, latestAiConversation));
        dto.setLastAnsweredByAiAgent(isLastAnsweredByAiAgent(latestRepliedMessage, latestAgentMessage, latestAiConversation));
        dto.setMessages(messages);
        dto.setSentiments(sentiments);
        dto.setSupports(supports);
        dto.setAiConversations(aiConversations);
        return dto;
    }

    private List<Message> findTicketMessages(Ticket ticket) {
        if (ticket.getSenderId() != null && !ticket.getSenderId().isBlank()) {
            LocalDateTime startAt = resolveTicketThreadStart(ticket);
            if (ticket.getTicketClosedAt() != null) {
                if (ticket.getTicketClosedAt().isBefore(startAt)) {
                    return messagesRepository.findTicketMessagesBySenderIdSince(ticket.getSenderId(), startAt);
                }
                return messagesRepository.findTicketMessagesBySenderIdBetween(
                        ticket.getSenderId(),
                        startAt,
                        ticket.getTicketClosedAt()
                );
            }
            return messagesRepository.findTicketMessagesBySenderIdSince(ticket.getSenderId(), startAt);
        }
        if (ticket.getMessageId() != null && !ticket.getMessageId().isBlank()) {
            return messagesRepository.findActiveByMessageId(ticket.getMessageId());
        }
        return Collections.emptyList();
    }

    private Optional<Message> latestTicketCustomerMessage(Ticket ticket) {
        if (ticket.getSenderId() == null || ticket.getSenderId().isBlank()) {
            return Optional.empty();
        }
        LocalDateTime startAt = resolveTicketThreadStart(ticket);
        List<Message> messages = ticket.getTicketClosedAt() != null
                ? messagesRepository.findTicketCustomerMessagesBySenderIdBetween(
                ticket.getSenderId(),
                startAt,
                ticket.getTicketClosedAt()
        )
                : messagesRepository.findTicketCustomerMessagesBySenderIdSince(
                ticket.getSenderId(),
                startAt
        );
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(messages.get(0));
    }

    private List<Sentiments> findTicketSentiments(List<String> messageIds) {
        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sentimentsRepository.findAllByMessageIdInOrderByIdDesc(messageIds);
    }

    private List<AIConversation> findTicketAiConversations(List<String> messageIds) {
        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        return aiConversationRepository.findAllByMessageIdInAndDeletedOrderByLastRepliedAtAscIdAsc(messageIds, false);
    }

    private boolean hasActiveSupportAssignment(Ticket ticket) {
        return ticket.getTicketNumber() != null
                && bankSupportRepo.existsByTicketNumberAndDeletedAndHandOverAtIsNull(ticket.getTicketNumber(), false);
    }

    private boolean isHandedOverByAi(Ticket ticket) {
        return "AI".equalsIgnoreCase(ticket.getTicketHandoverBy());
    }

    private boolean isHandedOverByBankSupport(Ticket ticket) {
        return "BANK_SUPPORT".equalsIgnoreCase(ticket.getTicketHandoverBy())
                || (ticket.getTicketNumber() != null
                && bankSupportRepo.existsByTicketNumberAndDeletedAndHandOverAtIsNotNull(ticket.getTicketNumber(), false));
    }

    private String resolveChannel(TicketResponseDTO ticketResponseDTO) {
        return resolveChannel(ticketResponseDTO.getMessages());
    }

    private String resolveChannel(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "unknown";
        }
        String source = messages.get(0).getSource();
        if (source == null || source.isBlank()) {
            return "unknown";
        }
        return source;
    }

    private String resolveReplySource(Ticket ticket, Message latestCustomerMessage) {
        if (latestCustomerMessage != null && latestCustomerMessage.getSource() != null && !latestCustomerMessage.getSource().isBlank()) {
            return latestCustomerMessage.getSource();
        }
        return resolveChannel(findTicketMessages(ticket));
    }

    private void sendReplyWebhook(
            Ticket ticket,
            Message latestCustomerMessage,
            String replyMessageId,
            String replyText,
            String username,
            String firstName
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketNumber", ticket.getTicketNumber());
        payload.put("ticketMessageId", ticket.getMessageId());
        payload.put("replyMessageId", replyMessageId);
        payload.put("source", resolveReplySource(ticket, latestCustomerMessage));
        payload.put("channel", resolveReplySource(ticket, latestCustomerMessage));
        payload.put("senderId", ticket.getSenderId());
        payload.put("customerId", ticket.getSenderId());
        payload.put("recipientId", latestCustomerMessage != null ? latestCustomerMessage.getRecipientId() : null);
        payload.put("customerMessageId", latestCustomerMessage != null ? latestCustomerMessage.getMessageId() : ticket.getMessageId());
        payload.put("customerMessage", latestCustomerMessage != null ? latestCustomerMessage.getMessage() : null);
        payload.put("message", replyText);
        payload.put("reply", replyText);
        payload.put("agentUsername", username);
        payload.put("agentFirstName", firstName);
        payload.put("sentByUsername", username);
        payload.put("sentByFirstName", firstName);
        payload.put("sentAt", LocalDateTime.now().toString());

        try {
            ResponseEntity<String> response = replyRestTemplate().postForEntity(
                    replyWebhookUrl,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Reply webhook failed with status " + response.getStatusCode().value());
            }
        } catch (RestClientException error) {
            throw new IllegalStateException("Reply webhook failed: " + error.getMessage(), error);
        }
    }

    private RestTemplate replyRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(20000);
        return new RestTemplate(requestFactory);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Message latestMessage(List<Message> messages) {
        return messages.stream()
                .max(Comparator
                        .comparing((Message message) -> safeTime(message.getCreatedAt()))
                        .thenComparing(Message::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private Message latestRepliedMessage(List<Message> messages) {
        return messages.stream()
                .filter(message -> message.getReply() != null && !message.getReply().isBlank())
                .max(Comparator
                        .comparing((Message message) -> safeTime(message.getRepliedAt() != null ? message.getRepliedAt() : message.getCreatedAt()))
                        .thenComparing(Message::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private Message latestAgentMessage(List<Message> messages) {
        return messages.stream()
                .filter(message -> "AGENT".equalsIgnoreCase(String.valueOf(message.getSenderType())))
                .max(Comparator
                        .comparing((Message message) -> safeTime(answeredTime(message)))
                        .thenComparing(Message::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private Sentiments latestSentiment(Message latestMessage, List<Sentiments> sentiments) {
        if (sentiments == null || sentiments.isEmpty()) {
            return null;
        }
        if (latestMessage != null && latestMessage.getMessageId() != null) {
            Optional<Sentiments> matchingSentiment = sentiments.stream()
                    .filter(sentiment -> latestMessage.getMessageId().equals(sentiment.getMessageId()))
                    .findFirst();
            if (matchingSentiment.isPresent()) {
                return matchingSentiment.get();
            }
        }
        return sentiments.get(0);
    }

    private BankSupport activeSupport(List<BankSupport> supports) {
        return supports.stream()
                .filter(support -> support.getHandOverAt() == null)
                .max(Comparator
                        .comparing((BankSupport support) -> safeTime(support.getTicketAssignAt()))
                        .thenComparing(BankSupport::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private BankSupport latestSupport(List<BankSupport> supports) {
        return supports.stream()
                .max(Comparator
                        .comparing((BankSupport support) -> safeTime(support.getHandOverAt() != null ? support.getHandOverAt() : support.getTicketAssignAt()))
                        .thenComparing(BankSupport::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private BankSupport latestHandedOverSupport(List<BankSupport> supports) {
        return supports.stream()
                .filter(support -> support.getHandOverAt() != null)
                .max(Comparator
                        .comparing((BankSupport support) -> safeTime(support.getHandOverAt()))
                        .thenComparing(BankSupport::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private AIConversation latestAiConversation(List<AIConversation> aiConversations) {
        return aiConversations.stream()
                .max(Comparator
                        .comparing((AIConversation conversation) -> safeTime(conversation.getLastRepliedAt()))
                        .thenComparing(AIConversation::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private LocalDateTime latestMessageTime(
            Message latestMessage,
            Message latestRepliedMessage,
            AIConversation latestAiConversation,
            Ticket ticket
    ) {
        return latestTime(
                latestMessage != null ? latestMessage.getCreatedAt() : null,
                latestRepliedMessage != null ? latestRepliedMessage.getRepliedAt() : null,
                latestAiConversation != null ? latestAiConversation.getLastRepliedAt() : null,
                ticket.getTicketUpdatedAt(),
                ticket.getTicketCreatedAt()
        );
    }

    private LocalDateTime resolveLastAnsweredAt(
            Message latestRepliedMessage,
            Message latestAgentMessage,
            AIConversation latestAiConversation
    ) {
        return latestTime(
                latestRepliedMessage != null ? latestRepliedMessage.getRepliedAt() : null,
                latestAgentMessage != null ? answeredTime(latestAgentMessage) : null,
                latestAiConversation != null ? latestAiConversation.getLastRepliedAt() : null
        );
    }

    private String resolveLastAnsweredBy(
            Message latestRepliedMessage,
            Message latestAgentMessage,
            AIConversation latestAiConversation
    ) {
        LocalDateTime replyTime = latestRepliedMessage != null ? latestRepliedMessage.getRepliedAt() : null;
        LocalDateTime agentTime = latestAgentMessage != null ? answeredTime(latestAgentMessage) : null;
        LocalDateTime aiTime = latestAiConversation != null ? latestAiConversation.getLastRepliedAt() : null;
        if (agentTime != null
                && (replyTime == null || !replyTime.isAfter(agentTime))
                && (aiTime == null || !aiTime.isAfter(agentTime))) {
            return firstNonBlank(
                    latestAgentMessage.getRepliedByUsername(),
                    latestAgentMessage.getRepliedBy(),
                    latestAgentMessage.getSenderId(),
                    latestAgentMessage.getSenderName()
            );
        }
        if (aiTime != null && (replyTime == null || aiTime.isAfter(replyTime))) {
            return "AI";
        }
        return latestRepliedMessage != null ? latestRepliedMessage.getRepliedBy() : null;
    }

    private boolean isLastAnsweredByAiAgent(
            Message latestRepliedMessage,
            Message latestAgentMessage,
            AIConversation latestAiConversation
    ) {
        String answeredBy = resolveLastAnsweredBy(latestRepliedMessage, latestAgentMessage, latestAiConversation);
        return answeredBy != null && answeredBy.equalsIgnoreCase("AI");
    }

    private LocalDateTime answeredTime(Message message) {
        return message != null && message.getRepliedAt() != null ? message.getRepliedAt() : message != null ? message.getCreatedAt() : null;
    }

    private LocalDateTime resolveTicketThreadStart(Ticket ticket) {
        LocalDateTime reference = ticket.getTicketClosedAt() != null ? ticket.getTicketClosedAt() : LocalDateTime.now();
        LocalDateTime createdAt = ticket.getTicketCreatedAt();
        if (createdAt == null || createdAt.isAfter(reference.plusMinutes(5))) {
            return reference.minusHours(ACTIVE_TICKET_WINDOW_HOURS);
        }
        return createdAt;
    }

    private LocalDateTime latestTime(LocalDateTime... values) {
        LocalDateTime latest = null;
        for (LocalDateTime value : values) {
            if (value != null && (latest == null || value.isAfter(latest))) {
                latest = value;
            }
        }
        return latest;
    }

    private LocalDateTime safeTime(LocalDateTime value) {
        return value != null ? value : LocalDateTime.MIN;
    }

    private String resolveQueue(Ticket ticket, BankSupport activeSupport, BankSupport lastHandedOverSupport) {
        if (ticket.isTicketClosed()) {
            return "CLOSED";
        }
        if (activeSupport != null) {
            return "ACTIVE_BANK_SUPPORT";
        }
        if (ticket.isTicketHandedOver()) {
            if (isHandedOverByAi(ticket)) {
                return "HANDED_OVER_AI";
            }
            if (isHandedOverByBankSupport(ticket) || lastHandedOverSupport != null) {
                return "HANDED_OVER_BANK_SUPPORT";
            }
            return "HANDED_OVER";
        }
        return "NEW";
    }

    private String resolveAssignmentMessageId(Ticket ticket, TicketAssignmentRequestDTO requestDTO) {
        if (requestDTO.getMessageId() != null && !requestDTO.getMessageId().isBlank()) {
            return requestDTO.getMessageId();
        }
        return ticket.getMessageId();
    }

}
