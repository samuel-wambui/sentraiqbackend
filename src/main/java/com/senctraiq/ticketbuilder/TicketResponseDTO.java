package com.senctraiq.ticketbuilder;

import com.senctraiq.AI.AIConversation;
import com.senctraiq.AI.sentiment.Sentiments;
import com.senctraiq.banksupport.BankSupport;
import com.senctraiq.messages.Message;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketResponseDTO {
    private Long id;
    private String ticketNumber;
    private String messageId;
    private String senderId;
    private String ticketStatus;
    private String ticketAssignedTo;
    private LocalDateTime ticketCreatedAt;
    private LocalDateTime ticketUpdatedAt;
    private LocalDateTime ticketClosedAt;
    private String closingRemarks;
    private String ticketClosedBy;
    private LocalDateTime ticketHandoverTime;
    private String ticketHandoverBy;
    private String handoverNote;
    private String requestedServices;
    private boolean ticketPending;
    private boolean ticketClosed;
    private boolean ticketDeleted;
    private boolean ticketHandedOver;
    private boolean open;
    private boolean closed;
    private String queue;
    private String channel;
    private String latestMessageId;
    private String latestMessage;
    private String latestSenderId;
    private String latestSenderName;
    private String recipientId;
    private LocalDateTime lastMessageAt;
    private String category;
    private String sentiment;
    private boolean activeSupportAssigned;
    private String activeSupportUsername;
    private LocalDateTime activeSupportAssignedAt;
    private String lastSupportUsername;
    private LocalDateTime lastSupportHandOverAt;
    private String lastAnsweredBy;
    private LocalDateTime lastAnsweredAt;
    private boolean lastAnsweredByAiAgent;
    private List<Message> messages;
    private List<Sentiments> sentiments;
    private List<BankSupport> supports;
    private List<AIConversation> aiConversations;
}
