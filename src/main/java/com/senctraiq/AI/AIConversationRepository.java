package com.senctraiq.AI;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {

    @Query("SELECT a FROM AIConversation a WHERE a.deleted = false AND a.id = ?1")
    Optional<AIConversation> findActiveById(Long id);

    @Query("SELECT a FROM AIConversation a WHERE a.deleted = false")
    List<AIConversation> findAllActive();

    @Query("SELECT a FROM AIConversation a WHERE a.deleted = false AND a.messageId = ?1")
    Optional<AIConversation> findActiveByMessageId(String messageId);

    List<AIConversation> findAllByMessageIdInAndDeletedOrderByLastRepliedAtAscIdAsc(List<String> messageIds, boolean deleted);

    @Query("SELECT a FROM AIConversation a WHERE a.deleted = false AND a.handedOverByAI = true")
    List<AIConversation> findActiveHandedOverConversations();

    @Query("SELECT a FROM AIConversation a WHERE a.deleted = false AND a.handedOverByAI = false")
    List<AIConversation> findActiveOngoingConversations();
}
