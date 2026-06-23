package com.senctraiq.AI.sentiment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SentimentsRepository extends JpaRepository<Sentiments, Long> {
    List<Sentiments> findAllByMessageIdOrderByIdDesc(String messageId);

    default Optional<Sentiments> findByMessageId(String messageId) {
        return findAllByMessageIdOrderByIdDesc(messageId).stream().findFirst();
    }

    List<Sentiments> findAllByMessageIdInOrderByIdDesc(List<String> messageIds);

    List<Sentiments> findByCategory(String category);
}
