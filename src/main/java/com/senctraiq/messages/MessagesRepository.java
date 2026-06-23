package com.senctraiq.messages;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessagesRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.deleted = false AND m.id = ?1")
    Optional<Message> findActiveById(Long id);

    boolean existsByMessageId(String messageId);

    @Query("SELECT m FROM Message m WHERE m.deleted = false")
    List<Message> findAllActive();

    @Query("SELECT m FROM Message m WHERE m.deleted = false AND m.senderId = ?1")
    List<Message> findActiveBySenderId(String senderId);

    @Query("SELECT m FROM Message m WHERE m.deleted = false AND m.recipientId = ?1")
    List<Message> findActiveByRecipientId(String recipientId);

    @Query("SELECT m FROM Message m WHERE m.deleted = false AND (m.senderId = ?1 OR m.recipientId = ?1)")
    List<Message> findActiveConversationsByUserId(String userId);

    @Query("SELECT m FROM Message m WHERE m.deleted = false AND m.source = ?1")
    List<Message> findActiveBySource(String source);

    @Query("""
    SELECT m FROM Message m
    WHERE m.deleted = false
      AND m.senderId = :senderId
      AND m.createdAt >= :activeFrom
    ORDER BY m.createdAt DESC
""")
    List<Message> findActiveMessagesBySenderId(
            @Param("senderId") String senderId,
            @Param("activeFrom") LocalDateTime activeFrom
    );

    @Query("""
    SELECT m FROM Message m
    WHERE m.deleted = false
      AND (m.senderId = :senderId OR m.recipientId = :senderId)
      AND m.createdAt >= :startAt
    ORDER BY m.createdAt ASC, m.id ASC
""")
    List<Message> findTicketMessagesBySenderIdSince(
            @Param("senderId") String senderId,
            @Param("startAt") LocalDateTime startAt
    );

    @Query("""
    SELECT m FROM Message m
    WHERE m.deleted = false
      AND (m.senderId = :senderId OR m.recipientId = :senderId)
      AND m.createdAt >= :startAt
      AND m.createdAt <= :endAt
    ORDER BY m.createdAt ASC, m.id ASC
""")
    List<Message> findTicketMessagesBySenderIdBetween(
            @Param("senderId") String senderId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    @Query("""
    SELECT m FROM Message m
    WHERE m.deleted = false
      AND m.messageId = :messageId
    ORDER BY m.createdAt ASC, m.id ASC
""")
    List<Message> findActiveByMessageId(@Param("messageId") String messageId);


    @Query("""
    SELECT m FROM Message m
    WHERE m.deleted = false
      AND m.senderId = :senderId
      AND m.createdAt >= :startAt
      AND (m.senderType IS NULL OR UPPER(m.senderType) <> 'AGENT')
    ORDER BY m.createdAt DESC, m.id DESC
""")
    List<Message> findTicketCustomerMessagesBySenderIdSince(
            @Param("senderId") String senderId,
            @Param("startAt") LocalDateTime startAt
    );

    @Query("""
    SELECT m FROM Message m
    WHERE m.deleted = false
      AND m.senderId = :senderId
      AND m.createdAt >= :startAt
      AND m.createdAt <= :endAt
      AND (m.senderType IS NULL OR UPPER(m.senderType) <> 'AGENT')
    ORDER BY m.createdAt DESC, m.id DESC
""")
    List<Message> findTicketCustomerMessagesBySenderIdBetween(
            @Param("senderId") String senderId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

}
