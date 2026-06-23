package com.senctraiq.ticketbuilder;

import lombok.Data;

@Data
public class TicketReplyRequestDTO {
    private String message;
    private String username;
    private String firstName;
}
