package com.senctraiq.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    // find by username and notification id (username is the primary identifier now)
    Optional<UserNotification> findByUserUsernameAndNotificationId(String username, Long notificationId);

    // user's notifications (not deleted)
    List<UserNotification> findByUserUsernameAndIsDeletedFalse(String username);

    // unread inbox count for username; sent/self-authored messages are shown under Sent, not Inbox
    @Query("""
            select count(un)
            from UserNotification un
            where un.user.username = :username
              and un.isRead = false
              and un.isDeleted = false
              and (un.notification.createdBy is null or un.notification.createdBy <> :username)
            """)
    Long countUnreadInboxByUsername(@Param("username") String username);

    // find all recipients for a notification (used for edits/replies)
    List<UserNotification> findByNotificationId(Long notificationId);
}
