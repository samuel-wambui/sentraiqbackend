package com.senctraiq.conversations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationContinuationMessageRepository extends JpaRepository<ConversationContinuationMessage, Long> {
    boolean existsByMessageId(String messageId);
    Optional<ConversationContinuationMessage> findByMessageId(String messageId);
}
