package com.senctraiq.ticketbuilder;

import lombok.Data;

@Data
public class TicketCloseRequestDTO {
    private String closingRemarks;
    private String closedBy;
}
