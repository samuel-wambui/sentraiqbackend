package com.senctraiq.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationsRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
