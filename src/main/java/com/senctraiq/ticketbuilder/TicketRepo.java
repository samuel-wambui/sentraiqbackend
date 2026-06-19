package com.senctraiq.ticketbuilder;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepo extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findFirstByTicketNumberAndTicketDeletedOrderByTicketUpdatedAtDescIdDesc(String ticketNumber, boolean ticketDeleted);

    default Optional<Ticket> findByTicketNumberAndTicketDeleted(String ticketNumber, boolean ticketDeleted) {
        return findFirstByTicketNumberAndTicketDeletedOrderByTicketUpdatedAtDescIdDesc(ticketNumber, ticketDeleted);
    }

    boolean existsByTicketNumber(String ticketNumber);

    Optional<Ticket> findFirstByMessageIdAndTicketDeletedOrderByTicketUpdatedAtDescIdDesc(String customerMessageId, boolean ticketDeleted);

    default Optional<Ticket> findByMessageIdAndTicketDeleted(String customerMessageId, boolean ticketDeleted) {
        return findFirstByMessageIdAndTicketDeletedOrderByTicketUpdatedAtDescIdDesc(customerMessageId, ticketDeleted);
    }

    Optional<Ticket> findFirstBySenderIdAndTicketDeletedOrderByTicketUpdatedAtDescIdDesc(String senderId, boolean ticketDeleted);

    default Optional<Ticket> findBySenderIdAndTicketDeleted(String senderId, boolean ticketDeleted) {
        return findFirstBySenderIdAndTicketDeletedOrderByTicketUpdatedAtDescIdDesc(senderId, ticketDeleted);
    }

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.senderId = :senderId
              AND t.ticketDeleted = false
              AND t.ticketClosed = false
              AND COALESCE(t.ticketUpdatedAt, t.ticketCreatedAt) >= :activeFrom
            ORDER BY COALESCE(t.ticketUpdatedAt, t.ticketCreatedAt) DESC, t.id DESC
            """)
    List<Ticket> findActiveTicketsBySenderId(
            @Param("senderId") String senderId,
            @Param("activeFrom") LocalDateTime activeFrom
    );
}
