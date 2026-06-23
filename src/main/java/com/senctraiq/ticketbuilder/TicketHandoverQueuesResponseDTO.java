package com.senctraiq.ticketbuilder;

import lombok.Data;

import java.util.List;

@Data
public class TicketHandoverQueuesResponseDTO {
    private List<TicketResponseDTO> handedOverByAi;
    private List<TicketResponseDTO> handedOverByBankSupport;
}
