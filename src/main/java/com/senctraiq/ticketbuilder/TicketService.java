package com.senctraiq.ticketbuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {
    @Autowired
    private TicketRepo ticketRepo;

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

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
            return ticketRepo.save(ticket);
        }
        return null;
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

}
