package com.senctraiq.ticketbuilder;

import lombok.Data;

@Data
public class TicketHandoverRequestDTO {
    private String messageId;
    private String handoverBy;
    private String handoverNote;
}
