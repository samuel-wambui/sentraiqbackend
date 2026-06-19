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


}
