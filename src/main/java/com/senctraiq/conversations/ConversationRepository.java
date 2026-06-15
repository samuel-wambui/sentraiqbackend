package com.senctraiq.conversations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    String REFERENCE_SELECT = """
            with conversation_reference as (
                select
                    c.id as "id",
                    c.source as "source",
                    c.sender_id as "senderId",
                    c.sender_name as "senderName",
                    c.recipient_id as "recipientId",
                    c.status as "status",
                    c.category as "category",
                    c.sentiment as "sentiment",
                    c.confidence as "confidence",
                    c.assigned_agent_id as "assignedAgentId",
                    assigned_user.username as "assignedAgentUsername",
                    c.handed_over_by_agent_id as "handedOverByAgentId",
                    case
                        when handover_user.id is not null then handover_user.username
                        when coalesce(c.handed_over_from_ai_agent, false) = true
                        then coalesce(nullif(c.last_answered_by, ''), 'AI Agent')
                        else null
                    end as "handedOverByUsername",
                    coalesce(c.handed_over_from_ai_agent, false) as "handedOverFromAiAgent",
                    c.last_answered_by as "lastAnsweredBy",
                    c.last_answered_by_agent_id as "lastAnsweredByAgentId",
                    case
                        when last_answer_user.id is not null then last_answer_user.username
                        when coalesce(c.last_answered_by_ai_agent, false) = true
                        then coalesce(nullif(c.last_answered_by, ''), 'AI Agent')
                        else null
                    end as "lastAnsweredByUsername",
                    last_answer_user.email as "lastAnsweredByEmail",
                    last_answer_user.firstname as "lastAnsweredByFirstName",
                    last_answer_user.lastname as "lastAnsweredByLastName",
                    coalesce(c.last_answered_by_ai_agent, false) as "lastAnsweredByAiAgent",
                    c.handover_note as "handoverNote",
                    c.started_at as "startedAt",
                    c.last_message_at as "lastMessageAt",
                    c.assigned_at as "assignedAt",
                    c.handed_over_at as "handedOverAt",
                    c.handed_over_from_ai_agent_at as "handedOverFromAiAgentAt",
                    c.handed_over_from_ai_agent_expires_at as "handedOverFromAiAgentExpiresAt",
                    c.last_answered_at as "lastAnsweredAt",
                    c.status_updated_at as "statusUpdatedAt",
                    c.pending_since as "pendingSince",
                    c.closed_at as "closedAt"
                from conversations c
                left join users assigned_user on assigned_user.id = c.assigned_agent_id
                left join users handover_user on handover_user.id = c.handed_over_by_agent_id
                left join users last_answer_user on last_answer_user.id = c.last_answered_by_agent_id
            )
            select *
            from conversation_reference
            """;

    Optional<Conversation> findTopBySourceAndSenderIdAndRecipientIdAndStatusInOrderByLastMessageAtDesc(
            String source,
            String senderId,
            String recipientId,
            List<ConversationStatus> statuses
    );

    Optional<Conversation> findTopBySourceAndSenderIdAndRecipientIdOrderByLastMessageAtDesc(
            String source,
            String senderId,
            String recipientId
    );

    default List<Conversation> findAllNewestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "lastMessageAt"));
    }

    @Query(value = REFERENCE_SELECT + " order by \"lastMessageAt\" desc", nativeQuery = true)
    List<ConversationReferenceProjection> findAllReferenceRowsNewestFirst();

    @Query(value = REFERENCE_SELECT + " where \"id\" = :id", nativeQuery = true)
    Optional<ConversationReferenceProjection> findReferenceRowById(@Param("id") Long id);

    @Query(value = REFERENCE_SELECT + " where \"id\" in (:ids)", nativeQuery = true)
    List<ConversationReferenceProjection> findReferenceRowsByIds(@Param("ids") Collection<Long> ids);

}
