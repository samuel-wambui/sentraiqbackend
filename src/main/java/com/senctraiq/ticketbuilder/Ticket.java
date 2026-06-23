package com.senctraiq.ticketbuilder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
    @Entity
    @AllArgsConstructor
    @NoArgsConstructor
    public class Ticket {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String ticketNumber;
        private LocalDateTime ticketCreatedAt;
        private String messageId;
        private String senderId;
        private String ticketStatus;
        private String ticketAssignedTo;
        private LocalDateTime ticketHandoverTime;
        private String ticketHandoverBy;
        private String handoverNote;
        private String requestedServices;
        private LocalDateTime ticketUpdatedAt;
        private LocalDateTime ticketClosedAt;
        @Column(columnDefinition = "TEXT")
        private String closingRemarks;
        private String ticketClosedBy;
        private boolean ticketClosed = false;
        private boolean ticketPending = false;
        private boolean ticketDeleted = false;
        private boolean ticketHandedOver =false;
}
