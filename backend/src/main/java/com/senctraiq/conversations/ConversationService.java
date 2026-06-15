package com.senctraiq.conversations;

import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    private static final long CONVERSATION_WINDOW_HOURS = 48;
    private static final long AI_AGENT_HANDOVER_TTL_HOURS = 48;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    @Autowired
    private ConversationContinuationMessageRepository continuationMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.conversations.reply-webhook-url:https://n8n.digitalbank365.com/webhook/facebook-webhook}")
    private String replyWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional(readOnly = true)
    public List<ConversationReferenceResponse> getConversations(String source, String status, String category, String assigned) {
        ConversationStatus statusFilter = parseStatus(status);

        return conversationRepository.findAllReferenceRowsNewestFirst().stream()
                .map(ConversationReferenceResponse::fromProjection)
                .filter(conversation -> matchesSource(conversation.getSource(), source))
                .filter(conversation -> statusFilter == null || conversation.getStatus() == statusFilter)
                .filter(conversation -> matchesCategory(conversation.getCategory(), category))
                .filter(conversation -> matchesAssigned(conversation, assigned))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConversationReferenceResponse> getActiveConversationsHandledByAgent(String answeredBy, String source) {
        String agentName = normalizeAgentName(answeredBy, "AI Agent");
        LocalDateTime now = LocalDateTime.now();
        Set<Long> handledConversationIds = new LinkedHashSet<>(
                messageRepository.findConversationIdsHandledByAgent(agentName)
        );

        return conversationRepository.findAllReferenceRowsNewestFirst().stream()
                .map(ConversationReferenceResponse::fromProjection)
                .filter(conversation -> conversation.getId() != null)
                .filter(conversation -> matchesSource(conversation.getSource(), source))
                .filter(conversation -> !isTerminalStatus(conversation.getStatus()))
                .filter(conversation -> !isConversationWindowExpired(conversation, now))
                .filter(conversation -> agentNameMatches(conversation.getLastAnsweredBy(), agentName)
                        || handledConversationIds.contains(conversation.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> getMessages(
            Long conversationId,
            String source,
            String status,
            String category
    ) {
        ConversationStatus statusFilter = parseStatus(status);
        String statusValue = statusFilter != null ? statusFilter.name() : null;
        String sourceValue = source == null || source.isBlank() ? null : normalizeChannel(source);

        return messageRepository.findTimelineRowsNewestFirst(conversationId, sourceValue, statusValue).stream()
                .map(ConversationMessageResponse::fromProjection)
                .filter(message -> matchesCategory(message.getCategory(), category))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> getConversationMessages(Long conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }

        return getMessages(conversationId, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<ConversationThreadResponse> getConversationThreads(
            String agentUsername,
            String customerId,
            String source,
            String status,
            String category,
            String from,
            String to
    ) {
        return findConversationThreads(agentUsername, customerId, source, status, category, from, to);
    }

    @Transactional(readOnly = true)
    public List<ConversationChannelSummary> getConversationThreadChannelSummary(
            String agentUsername,
            String customerId,
            String source,
            String status,
            String category,
            String from,
            String to
    ) {
        Map<String, ConversationChannelSummary> summaries = createChannelSummaryMap();

        findConversationThreads(agentUsername, customerId, source, status, category, from, to)
                .forEach(thread -> {
                    ConversationReferenceResponse conversation = thread.getConversation();
                    if (conversation == null) return;

                    String channel = normalizeChannel(conversation.getSource());
                    if (channel.isBlank()) return;

                    ConversationChannelSummary summary = summaries.computeIfAbsent(
                            channel,
                            key -> new ConversationChannelSummary(key, channelLabel(key), 0, 0, new LinkedHashMap<>())
                    );

                    summary.setTotal(summary.getTotal() + 1);
                    if (conversation.getAssignedAgentId() == null) {
                        summary.setUnassigned(summary.getUnassigned() + 1);
                    }

                    String conversationStatus = conversation.getStatus() != null
                            ? conversation.getStatus().name()
                            : "UNKNOWN";
                    summary.getStatuses().put(
                            conversationStatus,
                            summary.getStatuses().getOrDefault(conversationStatus, 0L) + 1
                    );
                });

        return new ArrayList<>(summaries.values());
    }

    private List<ConversationThreadResponse> findConversationThreads(
            String agentUsername,
            String customerId,
            String source,
            String status,
            String category,
            String from,
            String to
    ) {
        ConversationStatus statusFilter = parseStatus(status);
        LocalDateTime fromTime = parseDateTimeBoundary(from, false);
        LocalDateTime toTime = parseDateTimeBoundary(to, true);
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new IllegalArgumentException("from must be before to");
        }

        Map<Long, List<ConversationMessageResponse>> messagesByConversation = messageRepository
                .findTimelineRowsNewestFirst(null, null, null)
                .stream()
                .map(ConversationMessageResponse::fromProjection)
                .filter(message -> message.getConversationId() != null)
                .collect(Collectors.groupingBy(ConversationMessageResponse::getConversationId));

        return conversationRepository.findAllReferenceRowsNewestFirst().stream()
                .map(ConversationReferenceResponse::fromProjection)
                .filter(conversation -> matchesSource(conversation.getSource(), source))
                .filter(conversation -> statusFilter == null || conversation.getStatus() == statusFilter)
                .filter(conversation -> matchesCategory(conversation.getCategory(), category))
                .filter(conversation -> matchesCustomer(conversation, customerId))
                .filter(conversation -> {
                    List<ConversationMessageResponse> messages = messagesByConversation.getOrDefault(
                            conversation.getId(),
                            List.of()
                    );
                    return matchesTimeRange(conversation, messages, fromTime, toTime);
                })
                .map(conversation -> buildThreadResponse(
                        conversation,
                        messagesByConversation.getOrDefault(conversation.getId(), List.of())
                ))
                .filter(thread -> matchesAgent(thread.getAgentUsernames(), agentUsername))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConversationChannelSummary> getChannelSummary() {
        Map<String, ConversationChannelSummary> summaries = createChannelSummaryMap();

        for (Conversation conversation : conversationRepository.findAllNewestFirst()) {
            String channel = normalizeChannel(conversation.getSource());
            ConversationChannelSummary summary = summaries.get(channel);
            if (summary == null) continue;

            if (!isTerminalStatus(conversation.getStatus())) {
                summary.setTotal(summary.getTotal() + 1);
            }

            if (isNewQueueConversation(conversation)) {
                summary.setUnassigned(summary.getUnassigned() + 1);
            }

            String status = conversation.getStatus() != null ? conversation.getStatus().name() : "UNKNOWN";
            summary.getStatuses().put(status, summary.getStatuses().getOrDefault(status, 0L) + 1);
        }

        return new ArrayList<>(summaries.values());
    }

    @Transactional(readOnly = true)
    public ConversationReportsResponse getReports(
            String source,
            String sentiment,
            String category,
            String username,
            String from,
            String to
    ) {
        LocalDateTime fromTime = parseDateTimeBoundary(from, false);
        LocalDateTime toTime = parseDateTimeBoundary(to, true);
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new IllegalArgumentException("from must be before to");
        }

        String usernameFilter = normalizeOptionalText(username);
        List<ConversationReferenceResponse> conversations = conversationRepository
                .findAllReferenceRowsNewestFirst()
                .stream()
                .map(ConversationReferenceResponse::fromProjection)
                .filter(conversation -> matchesSource(conversation.getSource(), source))
                .filter(conversation -> matchesReportSentiment(conversation.getSentiment(), sentiment))
                .filter(conversation -> matchesReportCategory(conversation.getCategory(), category))
                .filter(conversation -> matchesReportRange(conversation, fromTime, toTime))
                .collect(Collectors.toList());

        Set<Long> conversationIds = conversations.stream()
                .map(ConversationReferenceResponse::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        final Set<Long> initialConversationIds = conversationIds;

        Map<Long, ConversationReferenceResponse> conversationById = conversations.stream()
                .filter(conversation -> conversation.getId() != null)
                .collect(Collectors.toMap(
                        ConversationReferenceResponse::getId,
                        conversation -> conversation,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        List<ConversationMessageResponse> agentMessages = messageRepository.findTimelineRowsNewestFirst(null, null, null)
                .stream()
                .map(ConversationMessageResponse::fromProjection)
                .filter(message -> message.getConversationId() != null)
                .filter(message -> initialConversationIds.contains(message.getConversationId()))
                .filter(message -> message.getSenderType() == SenderType.AGENT)
                .collect(Collectors.toList());

        if (usernameFilter != null) {
            Set<Long> handledBySelectedUsername = agentMessages.stream()
                    .filter(message -> matchesReportUsername(message, usernameFilter))
                    .map(ConversationMessageResponse::getConversationId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            conversations = conversations.stream()
                    .filter(conversation -> conversation.getId() != null)
                    .filter(conversation -> handledBySelectedUsername.contains(conversation.getId()))
                    .collect(Collectors.toList());

            conversationIds = conversations.stream()
                    .map(ConversationReferenceResponse::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            conversationById = conversations.stream()
                    .filter(conversation -> conversation.getId() != null)
                    .collect(Collectors.toMap(
                            ConversationReferenceResponse::getId,
                            conversation -> conversation,
                            (first, second) -> first,
                            LinkedHashMap::new
                    ));
        }

        Map<String, Long> positiveByCategory = new LinkedHashMap<>();
        Map<String, Long> negativeByCategory = new LinkedHashMap<>();
        Map<String, ReportCounter> channelCounters = new LinkedHashMap<>();
        Map<ChannelCategoryReportKey, ReportCounter> channelCategoryCounters = new LinkedHashMap<>();

        long positiveCases = 0;
        long negativeCases = 0;
        long neutralCases = 0;

        for (ConversationReferenceResponse conversation : conversations) {
            String reportCategoryValue = reportCategory(conversation.getCategory());
            String channel = reportChannel(conversation.getSource());
            String reportSentimentValue = normalizeReportSentiment(conversation.getSentiment());

            if ("positive".equals(reportSentimentValue)) {
                positiveCases += 1;
                positiveByCategory.put(reportCategoryValue, positiveByCategory.getOrDefault(reportCategoryValue, 0L) + 1);
            } else if ("negative".equals(reportSentimentValue)) {
                negativeCases += 1;
                negativeByCategory.put(reportCategoryValue, negativeByCategory.getOrDefault(reportCategoryValue, 0L) + 1);
            } else {
                neutralCases += 1;
            }

            channelCounters.computeIfAbsent(channel, key -> new ReportCounter()).add(reportSentimentValue);
            channelCategoryCounters
                    .computeIfAbsent(new ChannelCategoryReportKey(channel, reportCategoryValue), key -> new ReportCounter())
                    .add(reportSentimentValue);
        }

        Map<HandledCaseReportKey, Set<Long>> handledConversationIds = new LinkedHashMap<>();
        final Set<Long> reportConversationIds = conversationIds;
        final Map<Long, ConversationReferenceResponse> reportConversationById = conversationById;
        agentMessages.stream()
                .filter(message -> message.getConversationId() != null)
                .filter(message -> reportConversationIds.contains(message.getConversationId()))
                .filter(message -> usernameFilter == null || matchesReportUsername(message, usernameFilter))
                .forEach(message -> {
                    ConversationReferenceResponse conversation = reportConversationById.get(message.getConversationId());
                    if (conversation == null) return;

                    String handlerUsername = firstNonBlank(message.getSenderName(), message.getSenderId());
                    if (handlerUsername == null) return;

                    String channel = reportChannel(conversation.getSource());
                    String handlerCategory = reportCategory(conversation.getCategory());
                    HandledCaseReportKey key = new HandledCaseReportKey(handlerUsername, channel, handlerCategory);
                    handledConversationIds
                            .computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                            .add(message.getConversationId());
                });

        List<ConversationReportsResponse.CategoryCaseReport> positiveRows = categoryRows(positiveByCategory);
        List<ConversationReportsResponse.CategoryCaseReport> negativeRows = categoryRows(negativeByCategory);
        List<ConversationReportsResponse.ChannelCaseReport> channelRows = channelCounters.entrySet().stream()
                .map(entry -> new ConversationReportsResponse.ChannelCaseReport(
                        entry.getKey(),
                        reportChannelLabel(entry.getKey()),
                        entry.getValue().totalCases,
                        entry.getValue().positiveCases,
                        entry.getValue().negativeCases,
                        entry.getValue().neutralCases
                ))
                .sorted(Comparator.comparingLong(ConversationReportsResponse.ChannelCaseReport::getTotalCases).reversed()
                        .thenComparing(ConversationReportsResponse.ChannelCaseReport::getLabel))
                .collect(Collectors.toList());

        List<ConversationReportsResponse.ChannelCategoryCaseReport> channelCategoryRows = channelCategoryCounters.entrySet()
                .stream()
                .map(entry -> new ConversationReportsResponse.ChannelCategoryCaseReport(
                        entry.getKey().channel(),
                        reportChannelLabel(entry.getKey().channel()),
                        entry.getKey().category(),
                        entry.getValue().totalCases,
                        entry.getValue().positiveCases,
                        entry.getValue().negativeCases,
                        entry.getValue().neutralCases
                ))
                .sorted(Comparator.comparingLong(ConversationReportsResponse.ChannelCategoryCaseReport::getTotalCases).reversed()
                        .thenComparing(ConversationReportsResponse.ChannelCategoryCaseReport::getLabel)
                        .thenComparing(ConversationReportsResponse.ChannelCategoryCaseReport::getCategory))
                .collect(Collectors.toList());

        List<ConversationReportsResponse.HandledCaseReport> handledRows = handledConversationIds.entrySet()
                .stream()
                .map(entry -> new ConversationReportsResponse.HandledCaseReport(
                        entry.getKey().username(),
                        entry.getKey().category(),
                        entry.getKey().channel(),
                        reportChannelLabel(entry.getKey().channel()),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparingLong(ConversationReportsResponse.HandledCaseReport::getTotalCases).reversed()
                        .thenComparing(ConversationReportsResponse.HandledCaseReport::getUsername)
                        .thenComparing(ConversationReportsResponse.HandledCaseReport::getLabel)
                        .thenComparing(ConversationReportsResponse.HandledCaseReport::getCategory))
                .collect(Collectors.toList());

        return new ConversationReportsResponse(
                conversations.size(),
                positiveCases,
                negativeCases,
                neutralCases,
                positiveRows,
                negativeRows,
                channelRows,
                channelCategoryRows,
                handledRows
        );
    }

    private Map<String, ConversationChannelSummary> createChannelSummaryMap() {
        Map<String, ConversationChannelSummary> summaries = new LinkedHashMap<>();
        summaries.put("whatsapp", new ConversationChannelSummary("whatsapp", "WhatsApp", 0, 0, new LinkedHashMap<>()));
        summaries.put("facebook", new ConversationChannelSummary("facebook", "Facebook", 0, 0, new LinkedHashMap<>()));
        summaries.put("x", new ConversationChannelSummary("x", "X", 0, 0, new LinkedHashMap<>()));
        summaries.put("instagram", new ConversationChannelSummary("instagram", "Instagram", 0, 0, new LinkedHashMap<>()));
        return summaries;
    }

    @Transactional
    public ConversationReferenceResponse handleIncomingMessage(IncomingMessageRequest request) {
        if (request.getMessageId() == null || request.getMessageId().isBlank()) {
            throw new IllegalArgumentException("Message id is required");
        }

        if (messageExists(request.getMessageId())) {
            throw new IllegalStateException("Duplicate message ignored: " + request.getMessageId());
        }

        String customerId = resolveIncomingCustomerId(request);
        if (customerId == null) {
            throw new IllegalArgumentException("customerNo, customerId, or senderId is required");
        }

        String recipientId = firstNonBlank(request.getRecipientId(), request.getPhoneNumberId());
        LocalDateTime now = LocalDateTime.now();

        Conversation conversation = conversationRepository
                .findTopBySourceAndSenderIdAndRecipientIdOrderByLastMessageAtDesc(
                        request.getSource(),
                        customerId,
                        recipientId
                )
                .filter(existing -> !isTerminalStatus(existing.getStatus()))
                .filter(existing -> !isConversationWindowExpired(existing, now))
                .orElseGet(() -> createConversation(request, customerId, recipientId, now));

        normalizeAiHandoverExpiry(conversation, now);

        if (isAiHandoverContinuationActive(conversation, now)) {
            saveContinuationMessage(
                    conversation,
                    request.getMessageId(),
                    request.getSource(),
                    customerId,
                    request.getSenderName(),
                    recipientId,
                    request.getMessage(),
                    now
            );

            return getConversationReferenceResponse(conversation.getId());
        }

        routeIncomingConversation(conversation, now);

        conversation.setSenderId(customerId);
        conversation.setRecipientId(recipientId);
        applySenderName(conversation, request.getSenderName());
        conversation.setLastMessageAt(now);
        applyConversationClassification(conversation, request.getCategory(), request.getSentiment(), null);

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationMessage conversationMessage = new ConversationMessage();
        conversationMessage.setConversation(savedConversation);
        conversationMessage.setMessageId(request.getMessageId());
        conversationMessage.setSenderType(SenderType.CUSTOMER);
        conversationMessage.setSenderId(customerId);
        conversationMessage.setSenderName(normalizeOptionalText(request.getSenderName()));
        conversationMessage.setMessage(request.getMessage());
        applyMessageClassification(conversationMessage, request.getCategory(), request.getSentiment(), null);
        conversationMessage.setCreatedAt(now);

        messageRepository.save(conversationMessage);

        if (isAiHandoverRequested(request)) {
            AnswerAgentDetails aiAgent = defaultAiAgentDetails();
            LocalDateTime handoverAt = now.plusNanos(1_000_000);
            markConversationAnswered(savedConversation, aiAgent, handoverAt);
            markConversationHandedOverByAi(savedConversation, aiAgent, null, handoverAt);
            savedConversation = conversationRepository.save(savedConversation);
            saveAiHandoverSystemMessage(savedConversation, aiAgent, null, handoverAt);
        }

        return getConversationReferenceResponse(savedConversation.getId());
    }

    @Transactional
    public ConversationMessageResponse recordContinuationMessage(ConversationContinuationMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Continuation payload is required");
        }

        String messageId = firstNonBlank(request.getMessageId(), request.getCustomerMessageId());
        if (messageId == null) {
            throw new IllegalArgumentException("messageId or customerMessageId is required");
        }

        if (messageExists(messageId)) {
            throw new IllegalStateException("Duplicate continuation message ignored: " + messageId);
        }

        String customerId = firstNonBlank(request.getCustomerNo(), request.getCustomerId(), request.getSenderId());
        if (customerId == null) {
            throw new IllegalArgumentException("customerNo, customerId, or senderId is required");
        }

        String messageText = firstNonBlank(request.getMessage(), request.getCustomerMessage());
        if (messageText == null) {
            throw new IllegalArgumentException("message or customerMessage is required");
        }

        String recipientId = firstNonBlank(request.getRecipientId(), request.getPhoneNumberId());
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = resolveContinuationConversation(request, customerId, recipientId, now);

        return saveContinuationMessage(
                conversation,
                messageId,
                request.getSource(),
                customerId,
                request.getSenderName(),
                recipientId,
                messageText,
                now
        );
    }

    private Conversation resolveContinuationConversation(
            ConversationContinuationMessageRequest request,
            String customerId,
            String recipientId,
            LocalDateTime now
    ) {
        Conversation conversation = request.getConversationId() != null
                ? getConversationOrThrow(request.getConversationId())
                : findActiveConversation(request.getSource(), customerId, recipientId, now)
                .orElseThrow(() -> new IllegalArgumentException("No active conversation found for continuation"));

        if (conversation.getSenderId() == null || !conversation.getSenderId().equalsIgnoreCase(customerId)) {
            throw new IllegalArgumentException("Continuation sender does not match the conversation customer");
        }

        if (request.getSource() != null && !request.getSource().isBlank()
                && !matchesSource(conversation.getSource(), request.getSource())) {
            throw new IllegalArgumentException("Continuation source does not match the conversation source");
        }

        if (recipientId != null && conversation.getRecipientId() != null
                && !conversation.getRecipientId().equalsIgnoreCase(recipientId)) {
            throw new IllegalArgumentException("Continuation recipient does not match the conversation recipient");
        }

        normalizeAiHandoverExpiry(conversation, now);
        if (!isAiHandoverContinuationActive(conversation, now)) {
            throw new IllegalArgumentException("Conversation is not in an active AI handover continuation window");
        }

        return conversation;
    }

    private ConversationMessageResponse saveContinuationMessage(
            Conversation conversation,
            String messageId,
            String source,
            String senderId,
            String senderName,
            String recipientId,
            String messageText,
            LocalDateTime now
    ) {
        String normalizedMessage = requireText(messageText, "message");

        ConversationContinuationMessage continuationMessage = new ConversationContinuationMessage();
        continuationMessage.setConversationId(conversation.getId());
        continuationMessage.setMessageId(messageId);
        continuationMessage.setSource(firstNonBlank(source, conversation.getSource()));
        continuationMessage.setSenderId(senderId);
        continuationMessage.setSenderName(normalizeOptionalText(senderName));
        continuationMessage.setRecipientId(firstNonBlank(recipientId, conversation.getRecipientId()));
        continuationMessage.setMessage(normalizedMessage);
        continuationMessage.setCreatedAt(now);

        continuationMessageRepository.saveAndFlush(continuationMessage);

        conversation.setLastMessageAt(now);
        boolean assignedToHumanAgent = conversation.getAssignedAgentId() != null;
        conversation.setStatus(assignedToHumanAgent ? ConversationStatus.OPEN : ConversationStatus.HANDED_OVER);
        conversation.setStatusUpdatedAt(now);
        conversation.setPendingSince(null);
        conversation.setClosedAt(null);
        if (!assignedToHumanAgent && Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())) {
            conversation.setHandedOverFromAiAgentExpiresAt(now.plusHours(AI_AGENT_HANDOVER_TTL_HOURS));
        }
        conversationRepository.saveAndFlush(conversation);

        return messageRepository.findTimelineRowByMessageId(messageId)
                .map(ConversationMessageResponse::fromProjection)
                .orElseThrow(() -> new IllegalStateException("Continuation message was saved but not found in timeline"));
    }

    @Transactional
    public ConversationReferenceResponse setAiAgentHandover(
            Long conversationId,
            ConversationAiHandoverRequest request
    ) {
        Conversation conversation = getConversationOrThrow(conversationId);
        LocalDateTime now = LocalDateTime.now();
        boolean handedOverFromAiAgent = request == null || !Boolean.FALSE.equals(request.getHandedOverFromAiAgent());

        if (handedOverFromAiAgent) {
            if (Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())) {
                Conversation savedConversation = conversationRepository.save(conversation);
                return getConversationReferenceResponse(savedConversation.getId());
            }

            AnswerAgentDetails aiAgent = defaultAiAgentDetails();
            String note = request != null ? request.getHandoverNote() : null;
            markConversationHandedOverByAi(conversation, aiAgent, note, now);
            Conversation savedConversation = conversationRepository.save(conversation);
            saveAiHandoverSystemMessage(savedConversation, aiAgent, note, now);
            return getConversationReferenceResponse(savedConversation.getId());
        } else {
            clearAiHandover(conversation);
        }

        Conversation savedConversation = conversationRepository.save(conversation);
        return getConversationReferenceResponse(savedConversation.getId());
    }

    @Transactional
    public ConversationAiHandoverStatusResponse getAiAgentHandoverStatus(Long conversationId) {
        Conversation conversation = getConversationOrThrow(conversationId);
        normalizeAiHandoverExpiry(conversation, LocalDateTime.now());
        Conversation savedConversation = conversationRepository.save(conversation);

        return new ConversationAiHandoverStatusResponse(
                savedConversation.getId(),
                Boolean.TRUE.equals(savedConversation.getHandedOverFromAiAgent()),
                savedConversation.getHandedOverFromAiAgentAt(),
                savedConversation.getHandedOverFromAiAgentExpiresAt()
        );
    }

    @Transactional
    public ConversationCustomerActiveStatusResponse getCustomerActiveStatus(String customerId, String source) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }

        String normalizedCustomerId = customerId.trim();
        LocalDateTime now = LocalDateTime.now();

        return conversationRepository.findAllNewestFirst().stream()
                .filter(conversation -> conversation.getSenderId() != null)
                .filter(conversation -> conversation.getSenderId().equalsIgnoreCase(normalizedCustomerId))
                .filter(conversation -> matchesSource(conversation.getSource(), source))
                .filter(conversation -> !isTerminalStatus(conversation.getStatus()))
                .filter(conversation -> !isConversationWindowExpired(conversation, now))
                .findFirst()
                .map(conversation -> {
                    normalizeAiHandoverExpiry(conversation, now);
                    Conversation savedConversation = conversationRepository.save(conversation);
                    return buildCustomerActiveStatus(normalizedCustomerId, source, true, savedConversation);
                })
                .orElseGet(() -> buildCustomerActiveStatus(normalizedCustomerId, source, false, null));
    }

    @Transactional
    public ConversationMessageResponse replyToConversation(
            Long conversationId,
            ConversationReplyRequest request,
            String currentUsername
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Reply payload is required");
        }

        String replyText = request.getMessage() == null ? "" : request.getMessage().trim();
        if (replyText.isBlank()) {
            throw new IllegalArgumentException("Reply message is required");
        }

        Conversation conversation = getConversationOrThrow(conversationId);
        if (isTerminalStatus(conversation.getStatus())) {
            throw new IllegalArgumentException("Closed conversations cannot be replied to");
        }

        AgentIdentity agent = resolveAgent(request, currentUsername);
        LocalDateTime now = LocalDateTime.now();

        conversation.setAssignedAgentId(agent.id());
        if (conversation.getAssignedAt() == null) {
            conversation.setAssignedAt(now);
        }
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setStatusUpdatedAt(now);
        conversation.setPendingSince(null);
        conversation.setClosedAt(null);
        conversation.setLastMessageAt(now);
        endAiHandoverContinuationWindow(conversation, now);
        AnswerAgentDetails answerAgent = answerDetailsFromAgent(agent, false);
        markConversationAnswered(conversation, answerAgent, now);

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationMessage reply = new ConversationMessage();
        reply.setConversation(savedConversation);
        reply.setMessageId(normalizeReplyMessageId(request.getMessageId()));
        reply.setSenderType(SenderType.AGENT);
        applyAnswerAgentDetails(reply, answerAgent);
        reply.setMessage(replyText);
        copyConversationClassification(reply, savedConversation);
        reply.setCreatedAt(now);

        Map<String, Object> webhookPayload = buildReplyWebhookPayload(savedConversation, reply, agent);

        ConversationMessage savedReply = messageRepository.save(reply);
        postReplyWebhook(webhookPayload);

        return ConversationMessageResponse.fromEntity(savedReply);
    }

    @Transactional
    public ConversationMessageResponse recordAiAnswer(
            Long conversationId,
            ConversationAiAnswerRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("AI answer payload is required");
        }

        String answerText = request.getMessage() == null ? "" : request.getMessage().trim();
        if (answerText.isBlank()) {
            throw new IllegalArgumentException("AI answer message is required");
        }

        Conversation conversation = getConversationOrThrow(conversationId);
        if (isTerminalStatus(conversation.getStatus())) {
            throw new IllegalArgumentException("Closed conversations cannot be answered");
        }

        LocalDateTime now = LocalDateTime.now();
        AnswerAgentDetails answerAgent = answerDetailsFromRequest(request);
        boolean handoverControlAnswer = isHandoverControlMessage(answerText);
        boolean handoverRequested = isAiHandoverRequested(request) || handoverControlAnswer;
        boolean aiHandoverAlreadyRecorded = Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent());
        boolean newAiHandoverRequested = handoverRequested && !aiHandoverAlreadyRecorded;
        String messageId = normalizeAiAnswerMessageId(request.getMessageId());
        if (messageRepository.existsByMessageId(messageId)) {
            throw new IllegalStateException("Duplicate AI answer ignored: " + messageId);
        }

        normalizeAiHandoverExpiry(conversation, now);
        if (isActiveAiHandover(conversation) && !newAiHandoverRequested) {
            keepConversationInAiHandover(conversation, now);
        } else if (!aiHandoverAlreadyRecorded || !handoverRequested) {
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setStatusUpdatedAt(now);
            conversation.setPendingSince(null);
            conversation.setClosedAt(null);
        }
        conversation.setLastMessageAt(now);
        markConversationAnswered(conversation, answerAgent, now);
        LocalDateTime handoverAt = now.plusNanos(1_000_000);
        if (newAiHandoverRequested) {
            markConversationHandedOverByAi(conversation, answerAgent, request.getHandoverNote(), handoverAt);
        }

        Conversation savedConversation = conversationRepository.save(conversation);

        if (handoverControlAnswer) {
            if (!newAiHandoverRequested) {
                return ConversationMessageResponse.fromEntity(findLatestAiHandoverSystemMessage(savedConversation));
            }
            ConversationMessage savedSystemMessage = saveAiHandoverSystemMessage(
                    savedConversation,
                    answerAgent,
                    request.getHandoverNote(),
                    handoverAt
            );
            return ConversationMessageResponse.fromEntity(savedSystemMessage);
        }

        ConversationMessage answer = new ConversationMessage();
        answer.setConversation(savedConversation);
        answer.setMessageId(messageId);
        answer.setSenderType(SenderType.AGENT);
        applyAnswerAgentDetails(answer, answerAgent);
        answer.setMessage(answerText);
        copyConversationClassification(answer, savedConversation);
        answer.setCreatedAt(now);

        ConversationMessage savedAnswer = messageRepository.save(answer);
        if (newAiHandoverRequested) {
            saveAiHandoverSystemMessage(savedConversation, answerAgent, request.getHandoverNote(), handoverAt);
        }
        return ConversationMessageResponse.fromEntity(savedAnswer);
    }

    @Transactional
    public ConversationAiConversationResponse recordAiConversation(ConversationAiConversationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Conversation payload is required");
        }

        String source = requireText(request.getSource(), "source");
        String customerId = firstNonBlank(request.getCustomerNo(), request.getCustomerId(), request.getSenderId());
        if (customerId == null) {
            throw new IllegalArgumentException("customerNo, customerId, or senderId is required");
        }

        String recipientId = firstNonBlank(request.getRecipientId(), request.getPhoneNumberId());
        String customerMessageId = firstNonBlank(request.getCustomerMessageId(), request.getMessageId());
        if (customerMessageId == null) {
            throw new IllegalArgumentException("customerMessageId or messageId is required");
        }

        String customerMessageText = firstNonBlank(request.getCustomerMessage(), request.getMessage());
        if (customerMessageText == null) {
            throw new IllegalArgumentException("customerMessage or message is required");
        }

        String answerText = firstNonBlank(request.getAgentAnswer(), request.getAnswer());
        if (answerText == null) {
            throw new IllegalArgumentException("agentAnswer or answer is required");
        }

        AnswerAgentDetails answerAgent = answerDetailsFromRequest(request);
        boolean handoverControlAnswer = isHandoverControlMessage(answerText);
        String requestedAnswerMessageId = firstNonBlank(request.getAgentAnswerMessageId(), request.getAnswerMessageId());
        String answerMessageId = requestedAnswerMessageId != null
                ? requestedAnswerMessageId
                : "agent-" + customerMessageId;

        LocalDateTime now = LocalDateTime.now();
        Optional<ConversationMessage> existingCustomerMessage = messageRepository.findByMessageId(customerMessageId);

        Conversation conversation = existingCustomerMessage
                .map(ConversationMessage::getConversation)
                .filter(existing -> existing != null)
                .filter(existing -> matchesAiConversation(existing, source, customerId, recipientId))
                .orElseGet(() -> findOrCreateConversationForAiConversation(
                        source,
                        customerId,
                        recipientId,
                        request,
                        now
                ));

        if (isTerminalStatus(conversation.getStatus())) {
            throw new IllegalArgumentException("Closed conversations cannot be answered");
        }

        normalizeAiHandoverExpiry(conversation, now);
        conversation.setSource(source);
        conversation.setSenderId(customerId);
        applySenderName(conversation, request.getSenderName());
        if (recipientId != null) {
            conversation.setRecipientId(recipientId);
        }
        applyConversationClassification(conversation, request.getCategory(), request.getSentiment(), request.getConfidence());
        boolean handoverRequested = isAiHandoverRequested(request) || handoverControlAnswer;
        boolean aiHandoverAlreadyRecorded = Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent());
        boolean newAiHandoverRequested = handoverRequested && !aiHandoverAlreadyRecorded;
        if (isActiveAiHandover(conversation) && !newAiHandoverRequested) {
            keepConversationInAiHandover(conversation, now);
        } else if (!aiHandoverAlreadyRecorded || !handoverRequested) {
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setStatusUpdatedAt(now);
            conversation.setPendingSince(null);
            conversation.setClosedAt(null);
        }
        conversation.setLastMessageAt(now);
        markConversationAnswered(conversation, answerAgent, now);
        LocalDateTime handoverAt = now.plusNanos(1_000_000);
        if (newAiHandoverRequested) {
            markConversationHandedOverByAi(conversation, answerAgent, request.getHandoverNote(), handoverAt);
        }

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationMessage customerMessage = resolveCustomerMessageForAiConversation(
                existingCustomerMessage,
                savedConversation,
                request,
                customerMessageId,
                customerId,
                customerMessageText,
                now
        );

        ConversationMessage savedCustomerMessage = messageRepository.save(customerMessage);

        ConversationMessage savedAnswerMessage = null;
        if (!handoverControlAnswer) {
            Optional<ConversationMessage> existingAnswerMessage = messageRepository.findByMessageId(answerMessageId);
            ConversationMessage answerMessage = resolveAiAnswerMessageForAiConversation(
                    existingAnswerMessage,
                    savedConversation,
                    request,
                    answerMessageId,
                    answerText,
                    answerAgent,
                    now
            );

            savedAnswerMessage = messageRepository.save(answerMessage);
        }
        if (newAiHandoverRequested) {
            saveAiHandoverSystemMessage(savedConversation, answerAgent, request.getHandoverNote(), handoverAt);
        }

        List<ConversationMessageResponse> messages = messageRepository
                .findTimelineRowsNewestFirst(savedConversation.getId(), null, null)
                .stream()
                .map(ConversationMessageResponse::fromProjection)
                .sorted(Comparator.comparing(
                        ConversationMessageResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());

        return new ConversationAiConversationResponse(
                getConversationReferenceResponse(savedConversation.getId()),
                ConversationMessageResponse.fromEntity(savedCustomerMessage),
                savedAnswerMessage != null ? ConversationMessageResponse.fromEntity(savedAnswerMessage) : null,
                messages,
                request.getMetrics()
        );
    }

    @Transactional
    public ConversationReferenceResponse assignConversation(Long conversationId, ConversationActionRequest request, String currentUsername) {
        Conversation conversation = getConversationOrThrow(conversationId);
        if (isTerminalStatus(conversation.getStatus())) {
            throw new IllegalArgumentException("Closed conversations cannot be assigned");
        }

        AgentIdentity agent = resolveAgent(request, currentUsername);
        LocalDateTime now = LocalDateTime.now();
        boolean pickedFromAiConversation = shouldMarkAiHandoverOnAssignment(conversation);
        AnswerAgentDetails aiAgent = pickedFromAiConversation ? defaultAiAgentDetails() : null;

        if (pickedFromAiConversation) {
            markConversationHandedOverByAi(conversation, aiAgent, null, now);
        }

        conversation.setAssignedAgentId(agent.id());
        conversation.setAssignedAt(now);
        conversation.setStatus(ConversationStatus.OPEN);
        conversation.setStatusUpdatedAt(now);
        conversation.setPendingSince(null);
        conversation.setClosedAt(null);
        endAiHandoverContinuationWindow(conversation, now);

        Conversation savedConversation = conversationRepository.save(conversation);
        if (pickedFromAiConversation) {
            saveAiHandoverSystemMessage(savedConversation, aiAgent, null, now);
        }
        return getConversationReferenceResponse(savedConversation.getId());
    }

    @Transactional
    public ConversationReferenceResponse handoverConversation(
            Long conversationId,
            ConversationHandoverRequest request,
            String currentUsername
    ) {
        Conversation conversation = getConversationOrThrow(conversationId);
        if (isTerminalStatus(conversation.getStatus())) {
            throw new IllegalArgumentException("Closed conversations cannot be handed over");
        }

        AgentIdentity currentAgent = resolveCurrentAgent(currentUsername);
        assertCurrentAssignee(conversation, currentAgent);

        LocalDateTime now = LocalDateTime.now();
        String note = request != null && request.getNote() != null ? request.getNote().trim() : null;

        conversation.setHandedOverByAgentId(currentAgent.id());
        conversation.setHandedOverAt(now);
        conversation.setHandoverNote(note != null && !note.isBlank() ? note : null);
        conversation.setAssignedAgentId(null);
        conversation.setAssignedAt(null);
        conversation.setStatus(ConversationStatus.HANDED_OVER);
        conversation.setStatusUpdatedAt(now);
        conversation.setPendingSince(null);
        conversation.setClosedAt(null);
        conversation.setLastMessageAt(now);

        Conversation savedConversation = conversationRepository.save(conversation);
        saveHandoverSystemMessage(savedConversation, currentAgent, now);

        return getConversationReferenceResponse(savedConversation.getId());
    }

    @Transactional
    public ConversationReferenceResponse markPendingCustomer(Long conversationId, ConversationActionRequest request, String currentUsername) {
        Conversation conversation = getConversationOrThrow(conversationId);
        if (isTerminalStatus(conversation.getStatus())) {
            throw new IllegalArgumentException("Closed conversations cannot be marked pending");
        }

        if (conversation.getAssignedAgentId() == null) {
            AgentIdentity agent = resolveAgent(request, currentUsername);
            conversation.setAssignedAgentId(agent.id());
            conversation.setAssignedAt(LocalDateTime.now());
        }

        LocalDateTime now = LocalDateTime.now();
        conversation.setStatus(ConversationStatus.PENDING_CUSTOMER);
        conversation.setStatusUpdatedAt(now);
        conversation.setPendingSince(now);
        conversation.setClosedAt(null);

        Conversation savedConversation = conversationRepository.save(conversation);
        return getConversationReferenceResponse(savedConversation.getId());
    }

    @Transactional
    public ConversationReferenceResponse closeConversation(
            Long conversationId,
            ConversationCloseRequest request,
            String currentUsername
    ) {
        Conversation conversation = getConversationOrThrow(conversationId);
        String closingRemarks = request != null && request.getClosingRemarks() != null
                ? request.getClosingRemarks().trim()
                : "";
        if (closingRemarks.isBlank()) {
            throw new IllegalArgumentException("Closing remarks are required");
        }

        LocalDateTime now = LocalDateTime.now();
        AgentIdentity currentAgent = resolveCurrentAgent(currentUsername);

        if (conversation.getStatus() == ConversationStatus.PENDING_CUSTOMER) {
            LocalDateTime pendingStart = conversation.getPendingSince() != null
                    ? conversation.getPendingSince()
                    : conversation.getLastMessageAt();

            if (pendingStart == null || pendingStart.isAfter(now.minusHours(24))) {
                throw new IllegalArgumentException("Pending customer conversations can be closed after 24 hours");
            }
        } else {
            if (isTerminalStatus(conversation.getStatus())) {
                throw new IllegalArgumentException("Conversation is already closed");
            }

            assertCurrentAssignee(conversation, currentAgent);
        }

        conversation.setStatus(ConversationStatus.CLOSED);
        conversation.setStatusUpdatedAt(now);
        conversation.setClosedAt(now);
        conversation.setLastMessageAt(now);

        Conversation savedConversation = conversationRepository.save(conversation);
        saveCloseSystemMessage(savedConversation, currentAgent, now, closingRemarks);

        return getConversationReferenceResponse(savedConversation.getId());
    }

    private Conversation createConversation(
            IncomingMessageRequest request,
            String customerId,
            String recipientId,
            LocalDateTime now
    ) {
        Conversation newConversation = new Conversation();
        newConversation.setSource(request.getSource());
        newConversation.setSenderId(customerId);
        newConversation.setSenderName(normalizeOptionalText(request.getSenderName()));
        newConversation.setRecipientId(recipientId);
        newConversation.setStatus(ConversationStatus.NEW);
        applyConversationClassification(newConversation, request.getCategory(), request.getSentiment(), null);
        newConversation.setStartedAt(now);
        newConversation.setLastMessageAt(now);
        newConversation.setStatusUpdatedAt(now);
        initializeNewConversationHandoverState(newConversation);
        return newConversation;
    }

    private Conversation findOrCreateConversationForAiConversation(
            String source,
            String customerId,
            String recipientId,
            ConversationAiConversationRequest request,
            LocalDateTime now
    ) {
        return findActiveConversation(source, customerId, recipientId, now)
                .filter(existing -> !isTerminalStatus(existing.getStatus()))
                .filter(existing -> !isConversationWindowExpired(existing, now))
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setSource(source);
                    conversation.setSenderId(customerId);
                    conversation.setSenderName(normalizeOptionalText(request.getSenderName()));
                    conversation.setRecipientId(recipientId);
                    conversation.setStatus(ConversationStatus.OPEN);
                    applyConversationClassification(conversation, request.getCategory(), request.getSentiment(), request.getConfidence());
                    conversation.setStartedAt(now);
                    conversation.setLastMessageAt(now);
                    conversation.setStatusUpdatedAt(now);
                    initializeNewConversationHandoverState(conversation);
                    return conversation;
                });
    }

    private boolean matchesAiConversation(
            Conversation conversation,
            String source,
            String customerId,
            String recipientId
    ) {
        if (conversation == null) return false;
        if (!matchesSource(conversation.getSource(), source)) return false;
        if (conversation.getSenderId() == null || !conversation.getSenderId().equalsIgnoreCase(customerId)) return false;
        return recipientId == null
                || conversation.getRecipientId() == null
                || conversation.getRecipientId().equalsIgnoreCase(recipientId);
    }

    private Optional<Conversation> findActiveConversation(
            String source,
            String customerId,
            String recipientId,
            LocalDateTime now
    ) {
        return conversationRepository.findAllNewestFirst().stream()
                .filter(conversation -> matchesSource(conversation.getSource(), source))
                .filter(conversation -> conversation.getSenderId() != null)
                .filter(conversation -> conversation.getSenderId().equalsIgnoreCase(customerId))
                .filter(conversation -> recipientId == null
                        || conversation.getRecipientId() == null
                        || conversation.getRecipientId().equalsIgnoreCase(recipientId))
                .filter(conversation -> !isTerminalStatus(conversation.getStatus()))
                .filter(conversation -> !isConversationWindowExpired(conversation, now))
                .findFirst();
    }

    private ConversationMessage createCustomerMessage(
            Conversation conversation,
            String messageId,
            String customerId,
            String messageText,
            ConversationAiConversationRequest request,
            LocalDateTime now
    ) {
        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setMessageId(messageId);
        message.setSenderType(SenderType.CUSTOMER);
        message.setSenderId(customerId);
        message.setSenderName(normalizeOptionalText(request.getSenderName()));
        message.setMessage(messageText);
        applyMessageClassification(message, request.getCategory(), request.getSentiment(), request.getConfidence());
        message.setCreatedAt(now);
        return message;
    }

    private ConversationMessage resolveCustomerMessageForAiConversation(
            Optional<ConversationMessage> existingCustomerMessage,
            Conversation conversation,
            ConversationAiConversationRequest request,
            String messageId,
            String customerId,
            String messageText,
            LocalDateTime now
    ) {
        if (existingCustomerMessage.isEmpty()) {
            return createCustomerMessage(conversation, messageId, customerId, messageText, request, now);
        }

        ConversationMessage existing = existingCustomerMessage.get();
        if (sameMessageText(existing.getMessage(), messageText)) {
            return updateExistingCustomerMessage(existing, conversation, request, messageText, now);
        }

        String variantMessageId = variantMessageId(messageId, messageText);
        return messageRepository.findByMessageId(variantMessageId)
                .map(variant -> updateExistingCustomerMessage(variant, conversation, request, messageText, now))
                .orElseGet(() -> createCustomerMessage(conversation, variantMessageId, customerId, messageText, request, now));
    }

    private ConversationMessage updateExistingCustomerMessage(
            ConversationMessage message,
            Conversation conversation,
            ConversationAiConversationRequest request,
            String messageText,
            LocalDateTime now
    ) {
        message.setConversation(conversation);
        message.setSenderType(SenderType.CUSTOMER);
        if (message.getSenderId() == null || message.getSenderId().isBlank()) {
            message.setSenderId(conversation.getSenderId());
        }
        if (message.getSenderName() == null || message.getSenderName().isBlank()) {
            message.setSenderName(normalizeOptionalText(request.getSenderName()));
        }
        message.setMessage(messageText);
        applyMessageClassification(message, request.getCategory(), request.getSentiment(), request.getConfidence());
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(now);
        }
        return message;
    }

    private ConversationMessage createAiAnswerMessage(
            Conversation conversation,
            String messageId,
            String answerText,
            AnswerAgentDetails answerAgent,
            ConversationAiConversationRequest request,
            LocalDateTime now
    ) {
        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setMessageId(messageId);
        message.setSenderType(SenderType.AGENT);
        applyAnswerAgentDetails(message, answerAgent);
        message.setMessage(answerText);
        applyMessageClassification(message, request.getCategory(), request.getSentiment(), request.getConfidence());
        message.setCreatedAt(now);
        return message;
    }

    private ConversationMessage resolveAiAnswerMessageForAiConversation(
            Optional<ConversationMessage> existingAnswerMessage,
            Conversation conversation,
            ConversationAiConversationRequest request,
            String messageId,
            String answerText,
            AnswerAgentDetails answerAgent,
            LocalDateTime now
    ) {
        if (existingAnswerMessage.isEmpty()) {
            return createAiAnswerMessage(conversation, messageId, answerText, answerAgent, request, now);
        }

        ConversationMessage existing = existingAnswerMessage.get();
        if (sameMessageText(existing.getMessage(), answerText)) {
            return updateExistingAiAnswer(existing, conversation, request, answerText, answerAgent, now);
        }

        String variantMessageId = variantMessageId(messageId, answerText);
        return messageRepository.findByMessageId(variantMessageId)
                .map(variant -> updateExistingAiAnswer(variant, conversation, request, answerText, answerAgent, now))
                .orElseGet(() -> createAiAnswerMessage(conversation, variantMessageId, answerText, answerAgent, request, now));
    }

    private ConversationMessage updateExistingAiAnswer(
            ConversationMessage message,
            Conversation conversation,
            ConversationAiConversationRequest request,
            String answerText,
            AnswerAgentDetails answerAgent,
            LocalDateTime now
    ) {
        message.setConversation(conversation);
        message.setSenderType(SenderType.AGENT);
        applyAnswerAgentDetails(message, answerAgent);
        message.setMessage(answerText);
        applyMessageClassification(message, request.getCategory(), request.getSentiment(), request.getConfidence());
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(now);
        }
        return message;
    }

    private boolean sameMessageText(String currentText, String nextText) {
        return String.valueOf(currentText).equals(String.valueOf(nextText));
    }

    private String variantMessageId(String messageId, String text) {
        return messageId + "-variant-" + Integer.toHexString(String.valueOf(text).hashCode());
    }

    private void applyConversationClassification(
            Conversation conversation,
            String category,
            String sentiment,
            BigDecimal confidence
    ) {
        if (conversation == null) return;

        String normalizedCategory = normalizeOptionalText(category);
        if (normalizedCategory != null) {
            conversation.setCategory(normalizedCategory);
        }

        String normalizedSentiment = normalizeOptionalText(sentiment);
        if (normalizedSentiment != null) {
            conversation.setSentiment(normalizedSentiment);
        }

        if (confidence != null) {
            conversation.setConfidence(confidence);
        }
    }

    private void applyMessageClassification(
            ConversationMessage message,
            String category,
            String sentiment,
            BigDecimal confidence
    ) {
        if (message == null) return;

        String normalizedCategory = normalizeOptionalText(category);
        if (normalizedCategory != null) {
            message.setCategory(normalizedCategory);
        }

        String normalizedSentiment = normalizeOptionalText(sentiment);
        if (normalizedSentiment != null) {
            message.setSentiment(normalizedSentiment);
        }

        if (confidence != null) {
            message.setConfidence(confidence);
        }
    }

    private void copyConversationClassification(ConversationMessage message, Conversation conversation) {
        if (conversation == null) return;
        applyMessageClassification(
                message,
                conversation.getCategory(),
                conversation.getSentiment(),
                conversation.getConfidence()
        );
    }

    private void routeIncomingConversation(Conversation conversation, LocalDateTime now) {
        if (conversation.getId() == null) return;

        if (isActiveAiHandover(conversation, now)) {
            keepConversationInAiHandover(conversation, now);
            return;
        }

        if (conversation.getAssignedAgentId() == null) {
            if (conversation.getStatus() != ConversationStatus.ESCALATED
                    && conversation.getStatus() != ConversationStatus.HANDED_OVER) {
                conversation.setStatus(ConversationStatus.NEW);
                conversation.setStatusUpdatedAt(now);
                conversation.setPendingSince(null);
            }
            return;
        }

        if (conversation.getStatus() == ConversationStatus.NEW
                || conversation.getStatus() == ConversationStatus.ASSIGNED
                || conversation.getStatus() == ConversationStatus.HANDED_OVER
                || conversation.getStatus() == ConversationStatus.PENDING_CUSTOMER) {
            conversation.setStatus(ConversationStatus.OPEN);
            conversation.setStatusUpdatedAt(now);
            conversation.setPendingSince(null);
        }
    }

    private String normalizeReplyMessageId(String messageId) {
        return normalizeGeneratedMessageId(messageId, "agent");
    }

    private boolean messageExists(String messageId) {
        return messageRepository.existsByMessageId(messageId)
                || continuationMessageRepository.existsByMessageId(messageId);
    }

    private String requireText(String value, String fieldName) {
        String normalized = firstNonBlank(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;

        for (String value : values) {
            if (value == null) continue;
            String normalized = value.trim();
            if (!normalized.isBlank()) {
                return normalized;
            }
        }

        return null;
    }

    private String resolveIncomingCustomerId(IncomingMessageRequest request) {
        if (request == null) return null;
        return firstNonBlank(request.getCustomerNo(), request.getCustomerId(), request.getSenderId());
    }

    private void applySenderName(Conversation conversation, String senderName) {
        String normalized = normalizeOptionalText(senderName);
        if (normalized != null) {
            conversation.setSenderName(normalized);
        }
    }

    private String normalizeAiAnswerMessageId(String messageId) {
        return normalizeGeneratedMessageId(messageId, "ai-agent");
    }

    private String normalizeGeneratedMessageId(String messageId, String prefix) {
        String value = messageId == null ? "" : messageId.trim();
        if (!value.isBlank()) return value;
        return prefix + "-" + UUID.randomUUID();
    }

    private String normalizeAiAnswerer(String answeredBy) {
        return normalizeAgentName(answeredBy, "AI Agent");
    }

    private String normalizeAgentName(String agentName, String fallback) {
        String value = agentName == null ? "" : agentName.trim();
        if (!value.isBlank()) return value;

        String fallbackValue = fallback == null ? "" : fallback.trim();
        return fallbackValue.isBlank() ? "Agent" : fallbackValue;
    }

    private boolean agentNameMatches(String candidate, String agentName) {
        if (candidate == null || agentName == null) return false;
        return candidate.trim().equalsIgnoreCase(agentName.trim());
    }

    private boolean isAnsweredByAiAgent(ConversationAiConversationRequest request) {
        return request != null && isAiAgentAnswer(
                request.getAnsweredByAiAgent(),
                request.getAnsweredBy(),
                request.getAnsweredByUsername(),
                request.getAnsweredByAgentId(),
                true
        );
    }

    private boolean isAiAgentAnswer(
            Boolean answeredByAiAgent,
            String answeredBy,
            String username,
            Long agentId,
            boolean defaultWhenUnknown
    ) {
        if (Boolean.TRUE.equals(answeredByAiAgent)) return true;
        if (agentId != null) return false;
        if (isAiAgentName(answeredBy) || isAiAgentName(username)) return true;
        return answeredByAiAgent == null && defaultWhenUnknown;
    }

    private boolean isAiAgentName(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("ai")
                || normalized.equals("ai agent")
                || normalized.equals("ai_agent")
                || normalized.equals("ai-agent");
    }

    private AnswerAgentDetails answerDetailsFromAgent(AgentIdentity agent, boolean answeredByAiAgent) {
        return new AnswerAgentDetails(
                agent.id(),
                agent.username(),
                agent.email(),
                agent.firstName(),
                agent.lastName(),
                normalizeAgentName(agent.username(), "Agent"),
                answeredByAiAgent
        );
    }

    private AnswerAgentDetails answerDetailsFromRequest(ConversationAiAnswerRequest request) {
        String username = firstNonBlank(request.getAnsweredByUsername(), request.getAnsweredBy());
        String displayName = normalizeAgentName(firstNonBlank(request.getAnsweredBy(), username), "AI Agent");

        return new AnswerAgentDetails(
                request.getAnsweredByAgentId(),
                username,
                firstNonBlank(request.getAnsweredByEmail()),
                firstNonBlank(request.getAnsweredByFirstName()),
                firstNonBlank(request.getAnsweredByLastName()),
                displayName,
                isAiAgentAnswer(
                        request.getAnsweredByAiAgent(),
                        request.getAnsweredBy(),
                        request.getAnsweredByUsername(),
                        request.getAnsweredByAgentId(),
                        true
                )
        );
    }

    private AnswerAgentDetails answerDetailsFromRequest(ConversationAiConversationRequest request) {
        String username = firstNonBlank(request.getAnsweredByUsername(), request.getAnsweredBy());
        String displayName = normalizeAgentName(firstNonBlank(request.getAnsweredBy(), username), "AI Agent");

        return new AnswerAgentDetails(
                request.getAnsweredByAgentId(),
                username,
                firstNonBlank(request.getAnsweredByEmail()),
                firstNonBlank(request.getAnsweredByFirstName()),
                firstNonBlank(request.getAnsweredByLastName()),
                displayName,
                isAnsweredByAiAgent(request)
        );
    }

    private void applyAnswerAgentDetails(ConversationMessage message, AnswerAgentDetails answerAgent) {
        message.setSenderId(answerAgent.displayName());
    }

    private void markConversationAnswered(
            Conversation conversation,
            AnswerAgentDetails answerAgent,
            LocalDateTime answeredAt
    ) {
        conversation.setLastAnsweredBy(answerAgent.displayName());
        conversation.setLastAnsweredByAgentId(answerAgent.id());
        conversation.setLastAnsweredByAiAgent(answerAgent.aiAgent());
        conversation.setLastAnsweredAt(answeredAt);
    }

    private boolean isAiHandoverRequested(ConversationAiAnswerRequest request) {
        return request != null
                && (Boolean.TRUE.equals(request.getHandover())
                || Boolean.TRUE.equals(request.getHandedOver())
                || Boolean.TRUE.equals(request.getHandedOverFromAiAgent()));
    }

    private boolean isAiHandoverRequested(ConversationAiConversationRequest request) {
        return request != null
                && (Boolean.TRUE.equals(request.getHandover())
                || Boolean.TRUE.equals(request.getHandedOver())
                || Boolean.TRUE.equals(request.getHandedOverFromAiAgent()));
    }

    private boolean isAiHandoverRequested(IncomingMessageRequest request) {
        return request != null && Boolean.TRUE.equals(request.getHandedOverFromAiAgent());
    }

    private boolean isHandoverControlMessage(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.equals("handover") || normalized.equals("handedover");
    }

    private void keepConversationInAiHandover(Conversation conversation, LocalDateTime now) {
        conversation.setAssignedAgentId(null);
        conversation.setAssignedAt(null);
        conversation.setStatus(ConversationStatus.HANDED_OVER);
        conversation.setStatusUpdatedAt(now);
        conversation.setPendingSince(null);
        conversation.setClosedAt(null);
    }

    private void markConversationHandedOverByAi(
            Conversation conversation,
            AnswerAgentDetails aiAgent,
            String note,
            LocalDateTime handedOverAt
    ) {
        conversation.setHandedOverByAgentId(aiAgent.id());
        conversation.setHandedOverAt(handedOverAt);
        conversation.setHandoverNote(normalizeOptionalText(note));
        conversation.setAssignedAgentId(null);
        conversation.setAssignedAt(null);
        conversation.setHandedOverFromAiAgent(true);
        conversation.setHandedOverFromAiAgentAt(handedOverAt);
        conversation.setHandedOverFromAiAgentExpiresAt(handedOverAt.plusHours(AI_AGENT_HANDOVER_TTL_HOURS));
        conversation.setStatus(ConversationStatus.HANDED_OVER);
        conversation.setStatusUpdatedAt(handedOverAt);
        conversation.setPendingSince(null);
        conversation.setClosedAt(null);
        conversation.setLastMessageAt(handedOverAt);
    }

    private boolean isAiHandoverContinuationActive(Conversation conversation, LocalDateTime now) {
        return isActiveAiHandover(conversation, now)
                || isAssignedAiHandoverConversation(conversation);
    }

    private boolean isAssignedAiHandoverConversation(Conversation conversation) {
        return conversation != null
                && Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())
                && conversation.getAssignedAgentId() != null
                && !isTerminalStatus(conversation.getStatus());
    }

    private boolean shouldMarkAiHandoverOnAssignment(Conversation conversation) {
        return conversation != null
                && !Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())
                && Boolean.TRUE.equals(conversation.getLastAnsweredByAiAgent())
                && conversation.getAssignedAgentId() == null
                && !isTerminalStatus(conversation.getStatus());
    }

    private boolean isActiveAiHandover(Conversation conversation) {
        return isActiveAiHandover(conversation, LocalDateTime.now());
    }

    private boolean isActiveAiHandover(Conversation conversation, LocalDateTime now) {
        if (conversation == null || !Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())) return false;

        LocalDateTime expiresAt = conversation.getHandedOverFromAiAgentExpiresAt();
        return Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())
                && conversation.getAssignedAgentId() == null
                && conversation.getStatus() == ConversationStatus.HANDED_OVER
                && (expiresAt == null || expiresAt.isAfter(now));
    }

    private void clearAiHandover(Conversation conversation) {
        conversation.setHandedOverFromAiAgent(false);
        conversation.setHandedOverFromAiAgentAt(null);
        conversation.setHandedOverFromAiAgentExpiresAt(null);
    }

    private void initializeNewConversationHandoverState(Conversation conversation) {
        conversation.setHandedOverByAgentId(null);
        conversation.setHandedOverAt(null);
        conversation.setHandoverNote(null);
        clearAiHandover(conversation);
    }

    private void endAiHandoverContinuationWindow(Conversation conversation, LocalDateTime now) {
        if (Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())) {
            conversation.setHandedOverFromAiAgentExpiresAt(now);
        }
    }

    private ConversationMessage findLatestAiHandoverSystemMessage(Conversation conversation) {
        return messageRepository.findTopByConversationIdAndSenderTypeAndSenderIdAndMessageOrderByCreatedAtDesc(
                conversation.getId(),
                SenderType.SYSTEM,
                "AI Agent",
                "Ticket handed over by AI Agent"
        ).orElseThrow(() -> new IllegalStateException("AI handover was already recorded without a timeline message"));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private AnswerAgentDetails defaultAiAgentDetails() {
        return new AnswerAgentDetails(
                null,
                "AI Agent",
                null,
                null,
                null,
                "AI Agent",
                true
        );
    }

    private void saveHandoverSystemMessage(
            Conversation conversation,
            AgentIdentity currentAgent,
            LocalDateTime now
    ) {
        String note = conversation.getHandoverNote();
        String messageText = "Ticket handed over by " + currentAgent.username()
                + (note != null && !note.isBlank() ? ": " + note : "");

        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setMessageId("handover-" + UUID.randomUUID());
        message.setSenderType(SenderType.SYSTEM);
        message.setSenderId(currentAgent.username());
        message.setMessage(messageText);
        copyConversationClassification(message, conversation);
        message.setCreatedAt(now);

        messageRepository.save(message);
    }

    private ConversationMessage saveAiHandoverSystemMessage(
            Conversation conversation,
            AnswerAgentDetails aiAgent,
            String note,
            LocalDateTime now
    ) {
        String handoverUsername = normalizeAgentName(
                firstNonBlank(aiAgent.username(), aiAgent.displayName()),
                "AI Agent"
        );
        String normalizedNote = normalizeOptionalText(note);
        String messageText = "Ticket handed over by " + handoverUsername
                + (normalizedNote != null ? ": " + normalizedNote : "");

        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setMessageId("handover-" + UUID.randomUUID());
        message.setSenderType(SenderType.SYSTEM);
        message.setSenderId(handoverUsername);
        message.setMessage(messageText);
        copyConversationClassification(message, conversation);
        message.setCreatedAt(now);

        return messageRepository.save(message);
    }

    private void saveCloseSystemMessage(
            Conversation conversation,
            AgentIdentity currentAgent,
            LocalDateTime now,
            String closingRemarks
    ) {
        String messageText = "Ticket closed by " + currentAgent.username() + ": " + closingRemarks;

        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setMessageId("close-" + UUID.randomUUID());
        message.setSenderType(SenderType.SYSTEM);
        message.setSenderId(currentAgent.username());
        message.setMessage(messageText);
        copyConversationClassification(message, conversation);
        message.setCreatedAt(now);

        messageRepository.save(message);
    }

    private ConversationThreadResponse buildThreadResponse(
            ConversationReferenceResponse conversation,
            List<ConversationMessageResponse> messages
    ) {
        List<ConversationMessageResponse> timeline = messages.stream()
                .sorted(Comparator.comparing(
                        ConversationMessageResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());
        List<String> agentUsernames = agentUsernamesFor(conversation, timeline);

        return new ConversationThreadResponse(
                conversation,
                timeline,
                agentUsernames,
                agentUsernames.size()
        );
    }

    private List<String> agentUsernamesFor(ConversationReferenceResponse conversation, List<ConversationMessageResponse> messages) {
        Set<String> usernames = new LinkedHashSet<>();
        addUsername(usernames, conversation.getAssignedAgentUsername());
        addUsername(usernames, conversation.getHandedOverByUsername());
        addUsername(usernames, conversation.getLastAnsweredByUsername());
        addUsername(usernames, conversation.getLastAnsweredBy());

        messages.forEach(message -> {
            SenderType senderType = message.getSenderType();
            if (senderType == SenderType.AGENT || senderType == SenderType.SYSTEM) {
                addUsername(usernames, message.getSenderId());
            }
        });

        return new ArrayList<>(usernames);
    }

    private Map<String, Object> buildReplyWebhookPayload(
            Conversation conversation,
            ConversationMessage reply,
            AgentIdentity agent
    ) {
        String source = normalizeChannel(conversation.getSource());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "conversation.reply");
        payload.put("conversationId", conversation.getId());
        payload.put("messageId", reply.getMessageId());
        payload.put("source", source);
        payload.put("channel", source);
        payload.put("customerId", conversation.getSenderId());
        payload.put("customerNo", conversation.getSenderId());
        payload.put("senderId", conversation.getSenderId());
        payload.put("senderName", conversation.getSenderName());
        payload.put("recipientId", conversation.getRecipientId());
        payload.put("pageId", conversation.getRecipientId());
        payload.put("message", reply.getMessage());
        payload.put("text", reply.getMessage());
        payload.put("agentId", agent.id());
        payload.put("agentUsername", agent.username());
        payload.put("agentEmail", agent.email());
        payload.put("agentFirstName", agent.firstName());
        payload.put("agentLastName", agent.lastName());
        payload.put("answeredBy", agent.username());
        payload.put("answeredByAgentId", agent.id());
        payload.put("answeredByUsername", agent.username());
        payload.put("answeredByEmail", agent.email());
        payload.put("answeredByFirstName", agent.firstName());
        payload.put("answeredByLastName", agent.lastName());
        payload.put("answeredByAiAgent", false);
        payload.put("senderType", SenderType.AGENT.name());
        payload.put("category", conversation.getCategory());
        payload.put("status", conversation.getStatus() != null ? conversation.getStatus().name() : null);
        payload.put("createdAt", reply.getCreatedAt());
        return payload;
    }

    private void postReplyWebhook(Map<String, Object> payload) {
        if (replyWebhookUrl == null || replyWebhookUrl.isBlank()) {
            throw new IllegalStateException("Conversation reply webhook URL is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> webhookResponse = restTemplate.postForEntity(
                    replyWebhookUrl,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            HttpStatusCode statusCode = webhookResponse.getStatusCode();
            if (!statusCode.is2xxSuccessful()) {
                throw new IllegalStateException("Reply webhook returned " + statusCode.value());
            }
        } catch (RestClientException rce) {
            throw new IllegalStateException("Reply webhook failed: " + rce.getMessage(), rce);
        }
    }

    private ConversationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;

        try {
            return ConversationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported conversation status: " + status);
        }
    }

    private boolean matchesSource(String source, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return normalizeChannel(source).equals(normalizeChannel(filter));
    }

    private boolean matchesCategory(String category, String filter) {
        if (filter == null || filter.isBlank()) return true;

        String normalizedFilter = filter.trim();
        boolean wantsUncategorized = "uncategorized".equalsIgnoreCase(normalizedFilter)
                || "__uncategorized".equalsIgnoreCase(normalizedFilter);

        if (wantsUncategorized) {
            return category == null || category.isBlank();
        }

        return category != null && category.equalsIgnoreCase(normalizedFilter);
    }

    private boolean matchesAssigned(ConversationReferenceResponse conversation, String assigned) {
        if (assigned == null || assigned.isBlank()) return true;

        String normalized = assigned.trim().toLowerCase(Locale.ROOT);
        boolean isAssigned = conversation.getAssignedAgentId() != null;

        return switch (normalized) {
            case "assigned", "true", "yes" -> isAssigned;
            case "unassigned", "false", "no", "none" -> !isAssigned;
            default -> throw new IllegalArgumentException("assigned must be assigned or unassigned");
        };
    }

    private boolean matchesCustomer(ConversationReferenceResponse conversation, String customerId) {
        if (customerId == null || customerId.isBlank()) return true;
        return conversation.getSenderId() != null
                && conversation.getSenderId().equalsIgnoreCase(customerId.trim());
    }

    private boolean matchesAgent(List<String> agentUsernames, String agentUsername) {
        if (agentUsername == null || agentUsername.isBlank()) return true;
        String normalizedAgent = agentUsername.trim();
        return agentUsernames.stream().anyMatch(username -> username.equalsIgnoreCase(normalizedAgent));
    }

    private boolean matchesTimeRange(
            ConversationReferenceResponse conversation,
            List<ConversationMessageResponse> messages,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (from == null && to == null) return true;

        if (messages.stream().anyMatch(message -> isWithinRange(message.getCreatedAt(), from, to))) {
            return true;
        }

        return isWithinRange(conversation.getLastMessageAt(), from, to)
                || isWithinRange(conversation.getStartedAt(), from, to)
                || isWithinRange(conversation.getClosedAt(), from, to);
    }

    private boolean matchesReportRange(
            ConversationReferenceResponse conversation,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (from == null && to == null) return true;

        return isWithinRange(conversation.getLastMessageAt(), from, to)
                || isWithinRange(conversation.getStartedAt(), from, to)
                || isWithinRange(conversation.getClosedAt(), from, to);
    }

    private boolean isWithinRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) return false;
        if (from != null && value.isBefore(from)) return false;
        return to == null || !value.isAfter(to);
    }

    private LocalDateTime parseDateTimeBoundary(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) return null;

        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate date = LocalDate.parse(trimmed);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            } catch (DateTimeParseException nested) {
                throw new IllegalArgumentException("Invalid date/time filter: " + value);
            }
        }
    }

    private void addUsername(Set<String> usernames, String username) {
        if (username == null || username.isBlank()) return;
        usernames.add(username.trim());
    }

    private ConversationReferenceResponse getConversationReferenceResponse(Long conversationId) {
        conversationRepository.flush();
        return conversationRepository.findReferenceRowById(conversationId)
                .map(ConversationReferenceResponse::fromProjection)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
    }

    private Conversation getConversationOrThrow(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
    }

    private boolean isConversationWindowExpired(Conversation conversation, LocalDateTime now) {
        LocalDateTime expiresAt = conversationWindowExpiresAt(conversation);
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private boolean isConversationWindowExpired(ConversationReferenceResponse conversation, LocalDateTime now) {
        LocalDateTime expiresAt = conversationWindowExpiresAt(conversation);
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private LocalDateTime conversationWindowExpiresAt(Conversation conversation) {
        if (conversation == null) return null;

        LocalDateTime anchor = conversation.getLastMessageAt() != null
                ? conversation.getLastMessageAt()
                : conversation.getStartedAt();

        return anchor != null ? anchor.plusHours(CONVERSATION_WINDOW_HOURS) : null;
    }

    private LocalDateTime conversationWindowExpiresAt(ConversationReferenceResponse conversation) {
        if (conversation == null) return null;

        LocalDateTime anchor = conversation.getLastMessageAt() != null
                ? conversation.getLastMessageAt()
                : conversation.getStartedAt();

        return anchor != null ? anchor.plusHours(CONVERSATION_WINDOW_HOURS) : null;
    }

    private ConversationCustomerActiveStatusResponse buildCustomerActiveStatus(
            String customerId,
            String source,
            boolean active,
            Conversation conversation
    ) {
        if (conversation == null) {
            return new ConversationCustomerActiveStatusResponse(
                    customerId,
                    source,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null
            );
        }

        LocalDateTime startedAt = conversation.getStartedAt();
        return new ConversationCustomerActiveStatusResponse(
                customerId,
                conversation.getSource(),
                active,
                conversation.getId(),
                conversation.getStatus(),
                startedAt,
                conversation.getLastMessageAt(),
                conversationWindowExpiresAt(conversation),
                Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent()),
                conversation.getHandedOverFromAiAgentAt(),
                conversation.getHandedOverFromAiAgentExpiresAt()
        );
    }

    private void normalizeAiHandoverExpiry(Conversation conversation, LocalDateTime now) {
        if (!Boolean.TRUE.equals(conversation.getHandedOverFromAiAgent())) return;

        LocalDateTime expiresAt = conversation.getHandedOverFromAiAgentExpiresAt();
        if (expiresAt == null && conversation.getHandedOverFromAiAgentAt() != null) {
            expiresAt = conversation.getHandedOverFromAiAgentAt().plusHours(AI_AGENT_HANDOVER_TTL_HOURS);
        }

        conversation.setHandedOverFromAiAgentExpiresAt(expiresAt);
    }

    private AgentIdentity resolveAgent(ConversationActionRequest request, String currentUsername) {
        if (request != null && request.getAssignedAgentId() != null) {
            User user = userRepository.findById(request.getAssignedAgentId())
                    .orElseThrow(() -> new IllegalArgumentException("Assigned agent not found"));
            return toAgentIdentity(user);
        }

        String username = request != null ? request.getAssignedAgentUsername() : null;
        if (username == null || username.isBlank()) {
            username = currentUsername;
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Assigned agent username is required");
        }

        String normalizedUsername = username.trim();
        User user = userRepository.findByUsernameAndDeleted(normalizedUsername, false)
                .orElseThrow(() -> new IllegalArgumentException("Assigned agent not found: " + normalizedUsername));

        return toAgentIdentity(user);
    }

    private AgentIdentity resolveCurrentAgent(String currentUsername) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new IllegalArgumentException("Current agent username is required");
        }

        String normalizedUsername = currentUsername.trim();
        User user = userRepository.findByUsernameAndDeleted(normalizedUsername, false)
                .orElseThrow(() -> new IllegalArgumentException("Current agent not found: " + normalizedUsername));

        return toAgentIdentity(user);
    }

    private AgentIdentity toAgentIdentity(User user) {
        return new AgentIdentity(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    private void assertCurrentAssignee(Conversation conversation, AgentIdentity currentAgent) {
        Long assignedAgentId = conversation.getAssignedAgentId();

        if (assignedAgentId == null) {
            throw new IllegalArgumentException("Only an assigned ticket can be handed over");
        }

        if (!assignedAgentId.equals(currentAgent.id())) {
            throw new IllegalArgumentException("Only the current assignee can hand over this ticket");
        }
    }

    private boolean isTerminalStatus(ConversationStatus status) {
        return status == ConversationStatus.CLOSED || status == ConversationStatus.RESOLVED;
    }

    private boolean isNewQueueConversation(Conversation conversation) {
        return conversation.getStatus() == ConversationStatus.NEW
                && conversation.getAssignedAgentId() == null;
    }

    private List<ConversationReportsResponse.CategoryCaseReport> categoryRows(Map<String, Long> counts) {
        return counts.entrySet()
                .stream()
                .map(entry -> new ConversationReportsResponse.CategoryCaseReport(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(ConversationReportsResponse.CategoryCaseReport::getTotalCases).reversed()
                        .thenComparing(ConversationReportsResponse.CategoryCaseReport::getCategory))
                .collect(Collectors.toList());
    }

    private String reportCategory(String category) {
        String normalized = normalizeOptionalText(category);
        return normalized != null ? normalized : "Uncategorized";
    }

    private boolean matchesReportCategory(String category, String filter) {
        String normalizedFilter = normalizeOptionalText(filter);
        if (normalizedFilter == null) return true;

        return reportCategory(category).equalsIgnoreCase(reportCategory(normalizedFilter));
    }

    private boolean matchesReportSentiment(String sentiment, String filter) {
        String normalizedFilter = normalizeOptionalText(filter);
        if (normalizedFilter == null) return true;

        return normalizeReportSentiment(sentiment).equals(normalizeReportSentiment(normalizedFilter));
    }

    private String normalizeReportSentiment(String sentiment) {
        String normalized = sentiment == null ? "" : sentiment.trim().toLowerCase(Locale.ROOT);

        if (normalized.contains("positive")) return "positive";
        if (normalized.contains("negative")) return "negative";
        return "neutral";
    }

    private boolean matchesReportUsername(ConversationMessageResponse message, String usernameFilter) {
        String username = firstNonBlank(message.getSenderName(), message.getSenderId());
        return username != null && username.equalsIgnoreCase(usernameFilter);
    }

    private String reportChannel(String source) {
        String channel = normalizeChannel(source);
        return channel.isBlank() ? "unknown" : channel;
    }

    private String reportChannelLabel(String channel) {
        return "unknown".equals(channel) ? "Unknown" : channelLabel(channel);
    }

    private String normalizeChannel(String source) {
        String normalized = source == null
                ? ""
                : source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");

        if (normalized.contains("whatsapp")) return "whatsapp";
        if (normalized.contains("facebook") || normalized.equals("fb") || normalized.contains("messenger")) {
            return "facebook";
        }
        if (normalized.equals("x") || normalized.equals("xcom") || normalized.contains("twitter")) return "x";
        if (normalized.contains("instagram") || normalized.equals("ig")) return "instagram";

        return normalized;
    }

    private String channelLabel(String channel) {
        return switch (channel) {
            case "whatsapp" -> "WhatsApp";
            case "facebook" -> "Facebook";
            case "x" -> "X";
            case "instagram" -> "Instagram";
            default -> channel;
        };
    }

    private static class ReportCounter {
        private long totalCases;
        private long positiveCases;
        private long negativeCases;
        private long neutralCases;

        private void add(String sentiment) {
            totalCases += 1;

            if ("positive".equals(sentiment)) {
                positiveCases += 1;
            } else if ("negative".equals(sentiment)) {
                negativeCases += 1;
            } else {
                neutralCases += 1;
            }
        }
    }

    private record ChannelCategoryReportKey(String channel, String category) {}

    private record HandledCaseReportKey(String username, String channel, String category) {}

    private record AgentIdentity(Long id, String username, String email, String firstName, String lastName) {}

    private record AnswerAgentDetails(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName,
            String displayName,
            boolean aiAgent
    ) {}
}
