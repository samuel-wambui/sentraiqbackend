package com.senctraiq.ticketbuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketHandoverStatusResponseDTO {
    private String messageId;
    private boolean ticketFound;
    private boolean handedOver;
    private String handoverStatus;
    private String ticketNumber;
    private String ticketStatus;
    private String ticketAssignedTo;
    private LocalDateTime ticketHandoverTime;
    private String ticketHandoverBy;
    private String handoverNote;
    private LocalDateTime ticketCreatedAt;
    private LocalDateTime ticketUpdatedAt;
}
