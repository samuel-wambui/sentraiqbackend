package com.senctraiq.ticketbuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.print.attribute.standard.DateTimeAtCompleted;
import java.time.LocalDateTime;


@Data
    @Entity
    @AllArgsConstructor
    @NoArgsConstructor
    public class BuildTicket {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String ticketNumber;
        private String ticketStatus;
        private boolean ticketHandedOver;
        private String ticketAssignedTo;
        private LocalDateTime ticketHandoverTime;
        private String TicketHandoverBy;
        private String handoverNote;
        private String requestedServices;
        private LocalDateTime ticketCreatedAt;
        private LocalDateTime ticketUpdatedAt;
}
