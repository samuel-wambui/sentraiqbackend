package com.senctraiq.conversations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    String TIMELINE_ROWS = """
            select
                m.id as "id",
                m.message_id as "messageId",
                m.conversation_id as "conversationId",
                m.sender_type as "senderType",
                m.sender_id as "senderId",
                m.sender_name as "senderName",
                m.message as "message",
                m.category as "category",
                m.sentiment as "sentiment",
                m.confidence as "confidence",
                m.created_at as "createdAt"
            from conversation_messages m
            join conversations c on c.id = m.conversation_id
            where (:conversationId is null or m.conversation_id = :conversationId)
              and (:source is null or lower(c.source) = lower(:source))
              and (:status is null or c.status = :status)
            union all
            select
                -cm.id as "id",
                cm.message_id as "messageId",
                cm.conversation_id as "conversationId",
                'CUSTOMER' as "senderType",
                cm.sender_id as "senderId",
                cm.sender_name as "senderName",
                cm.message as "message",
                c.category as "category",
                c.sentiment as "sentiment",
                c.confidence as "confidence",
                cm.created_at as "createdAt"
            from conversation_continuation_messages cm
            join conversations c on c.id = cm.conversation_id
            where (:conversationId is null or cm.conversation_id = :conversationId)
              and (:source is null or lower(c.source) = lower(:source))
              and (:status is null or c.status = :status)
            """;

    boolean existsByMessageId(String messageId);

    Optional<ConversationMessage> findByMessageId(String messageId);

    Optional<ConversationMessage> findTopByConversationIdAndSenderTypeAndSenderIdAndMessageOrderByCreatedAtDesc(
            Long conversationId,
            SenderType senderType,
            String senderId,
            String message
    );

    @Query(value = """
            select *
            from (
            """ + TIMELINE_ROWS + """
            ) timeline
            order by "createdAt" desc, "id" desc
            """, nativeQuery = true)
    List<ConversationMessageTimelineProjection> findTimelineRowsNewestFirst(
            @Param("conversationId") Long conversationId,
            @Param("source") String source,
            @Param("status") String status
    );

    @Query(value = """
            select
                m.id as "id",
                m.message_id as "messageId",
                m.conversation_id as "conversationId",
                m.sender_type as "senderType",
                m.sender_id as "senderId",
                m.sender_name as "senderName",
                m.message as "message",
                m.category as "category",
                m.sentiment as "sentiment",
                m.confidence as "confidence",
                m.created_at as "createdAt"
            from conversation_messages m
            where m.message_id = :messageId
            union all
            select
                -cm.id as "id",
                cm.message_id as "messageId",
                cm.conversation_id as "conversationId",
                'CUSTOMER' as "senderType",
                cm.sender_id as "senderId",
                cm.sender_name as "senderName",
                cm.message as "message",
                c.category as "category",
                c.sentiment as "sentiment",
                c.confidence as "confidence",
                cm.created_at as "createdAt"
            from conversation_continuation_messages cm
            join conversations c on c.id = cm.conversation_id
            where cm.message_id = :messageId
            """, nativeQuery = true)
    Optional<ConversationMessageTimelineProjection> findTimelineRowByMessageId(@Param("messageId") String messageId);

    @Query("""
            select distinct m.conversation.id
             from ConversationMessage m
             where m.conversation is not null
               and lower(m.senderId) = lower(:agentName)
            """)
    List<Long> findConversationIdsHandledByAgent(@Param("agentName") String agentName);

    List<ConversationMessage> findByConversationId(Long conversationId, Sort sort);

    default List<ConversationMessage> findAllNewestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    default List<ConversationMessage> findByConversationIdNewestFirst(Long conversationId) {
        return findByConversationId(conversationId, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
