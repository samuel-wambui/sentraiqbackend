package com.senctraiq.banksupport;


import com.senctraiq.ticketbuilder.Ticket;
import com.senctraiq.ticketbuilder.TicketRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SupportService {

    private final TicketRepo ticketRepo;
    private final BankSupportRepo bankSupportRepo;

    public SupportService(TicketRepo ticketRepo, BankSupportRepo bankSupportRepo) {
        this.ticketRepo = ticketRepo;
        this.bankSupportRepo = bankSupportRepo;
    }

   public BankSupport assignTicketToAgent(String username, String ticketNumber, String messageId) {
        BankSupport supportMessage = new BankSupport();
        supportMessage.setUsername(username);
        supportMessage.setTicketNumber(ticketNumber);
        supportMessage.setMessageId(messageId);
        supportMessage.setTicketAssignAt(LocalDateTime.now());

        Optional<Ticket> ticket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if (ticket.isPresent()) {
            Ticket ticket1 = ticket.get();
            ticket1.setTicketAssignedTo(username);
            ticket1.setTicketStatus("OPEN");
            ticket1.setTicketHandedOver(false);
            ticket1.setTicketUpdatedAt(LocalDateTime.now());
            ticketRepo.save(ticket1);
        }

        return bankSupportRepo.save(supportMessage);
    }

    public BankSupport handOverTicket(String TicketNumber, String messageId) {
        BankSupport supportMessages = bankSupportRepo.findByMessageIdAndDeleted(messageId , false);
        if (supportMessages != null) {
            supportMessages.setHandOverAt(LocalDateTime.now());
            bankSupportRepo.save(supportMessages);
        } else {
            throw new RuntimeException("Support message not found or already deleted");
        }
        Optional<Ticket> ticket = ticketRepo.findByTicketNumberAndTicketDeleted(TicketNumber, false);
        if (ticket.isPresent()) {
            Ticket ticket1 = ticket.get();
            ticket1.setTicketHandedOver(true);
            ticket1.setTicketHandoverBy("BANK_SUPPORT");
            ticket1.setTicketHandoverTime(LocalDateTime.now());
            ticket1.setTicketAssignedTo(null);
            ticket1.setTicketUpdatedAt(LocalDateTime.now());
            ticketRepo.save(ticket1);
        }
        else {
            throw new RuntimeException("Ticket not found or already deleted");
        }
        return null;
    }

    public BankSupport assignToHandedOverTicket(String username, String ticketNumber, String messageId) {
        Optional<Ticket> ticket = ticketRepo.findByTicketNumberAndTicketDeleted(ticketNumber, false);
        if (ticket.isPresent()) {
            Ticket ticket1 = ticket.get();
            if (ticket1.isTicketHandedOver()) {
                ticket1.setTicketHandedOver(false);
                ticketRepo.save(ticket1);
                BankSupport supportMessage = new BankSupport();
                supportMessage.setUsername(username);
                supportMessage.setTicketNumber(ticketNumber);
                supportMessage.setMessageId(messageId);
                supportMessage.setTicketAssignAt(LocalDateTime.now());
                ticket1.setTicketAssignedTo(username);
                ticket1.setTicketStatus("OPEN");
                ticket1.setTicketUpdatedAt(LocalDateTime.now());
                ticketRepo.save(ticket1);
                return bankSupportRepo.save(supportMessage);
            } else {
                throw new RuntimeException("Ticket has not been handed over yet");
            }
        } else {
            throw new RuntimeException("Ticket not found or already deleted");
        }
    }
}
