package com.senctraiq.notifications;



import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * NotificationService that uses username as the primary user identifier.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationsRepo notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private LinkedHashSet<String> getTargetGroups(NotificationsDto notificationsDto) {
        LinkedHashSet<String> groups = new LinkedHashSet<>();

        if (notificationsDto.getTargetGroups() != null) {
            for (String group : notificationsDto.getTargetGroups()) {
                if (group != null && !group.isBlank()) {
                    groups.add(group.trim());
                }
            }
        }

        if (notificationsDto.getTargetGroup() != null && !notificationsDto.getTargetGroup().isBlank()) {
            for (String group : notificationsDto.getTargetGroup().split(",")) {
                if (group != null && !group.isBlank()) {
                    groups.add(group.trim());
                }
            }
        }

        return groups;
    }

    private NotificationPayload toPayload(Notification notification, boolean read) {
        NotificationPayload payload = new NotificationPayload();
        payload.id = notification.getId();
        payload.message = notification.getMessage();
        payload.createdBy = notification.getCreatedBy();
        payload.createdAt = notification.getCreatedAt();
        payload.targetGroup = notification.getTargetGroup();
        payload.targetUsername = notification.getTargetUsername();
        payload.actionType = notification.getActionType();
        payload.actionRefId = notification.getActionRefId();
        payload.actionStatus = notification.getActionStatus();
        payload.read = read;
        payload.parentId = notification.getParentId();
        payload.editedAt = notification.getEditedAt();
        return payload;
    }

    private void addDelivery(List<UserNotification> deliveries, Notification notification, User user, boolean read) {
        if (user == null) {
            return;
        }

        for (UserNotification delivery : deliveries) {
            if (delivery.getUser() != null && Objects.equals(delivery.getUser().getId(), user.getId())) {
                if (read) {
                    delivery.setIsRead(true);
                    delivery.setReadAt(LocalDateTime.now());
                }
                return;
            }
        }

        UserNotification delivery = new UserNotification();
        delivery.setUser(user);
        delivery.setNotification(notification);
        if (read) {
            delivery.setIsRead(true);
            delivery.setReadAt(LocalDateTime.now());
        }
        deliveries.add(delivery);
    }

    private void addCreatorDelivery(List<UserNotification> deliveries, Notification notification) {
        String createdBy = notification.getCreatedBy();
        if (createdBy == null || createdBy.isBlank()) {
            return;
        }

        userRepository.findByUsernameAndDeleted(createdBy, false)
                .ifPresent(user -> addDelivery(deliveries, notification, user, true));
    }

    private void saveAndSendDeliveries(List<UserNotification> deliveries, Notification notification) {
        if (deliveries.isEmpty()) {
            return;
        }

        userNotificationRepository.saveAll(deliveries);
        for (UserNotification delivery : deliveries) {
            if (delivery.getUser() == null) {
                continue;
            }

            try {
                messagingTemplate.convertAndSendToUser(
                        delivery.getUser().getUsername(),
                        "/queue/notifications",
                        toPayload(notification, Boolean.TRUE.equals(delivery.getIsRead()))
                );
            } catch (Exception ignored) {}
        }
    }

    // CREATE NOTIFICATION (supports group, single username, or list of usernames)
    public Notification createNotification(NotificationsDto notificationsDto) {
        LinkedHashSet<String> targetGroups = getTargetGroups(notificationsDto);
        boolean hasUserList = notificationsDto.getTargetUsernames() != null && !notificationsDto.getTargetUsernames().isEmpty();
        boolean hasSingleUser = notificationsDto.getTargetUsername() != null && !notificationsDto.getTargetUsername().isBlank();

        if (!hasUserList && !hasSingleUser && targetGroups.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one target user or group");
        }

        Notification notification = new Notification();
        notification.setMessage(notificationsDto.getMessage());
        notification.setCreatedBy(notificationsDto.getCreatedBy());
        notification.setTargetGroup(targetGroups.isEmpty() ? notificationsDto.getTargetGroup() : String.join(", ", targetGroups));
        notification.setTargetUsername(notificationsDto.getTargetUsername());
        notification.setActionType(notificationsDto.getActionType());
        notification.setActionRefId(notificationsDto.getActionRefId());
        notification.setActionStatus(notificationsDto.getActionStatus());
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);

        List<UserNotification> userNotifications = new ArrayList<>();

        // 1) multiple explicit usernames provided
        if (notificationsDto.getTargetUsernames() != null && !notificationsDto.getTargetUsernames().isEmpty()) {
            LinkedHashSet<String> uniq = new LinkedHashSet<>(notificationsDto.getTargetUsernames());
            List<String> missing = new ArrayList<>();

            for (String uname : uniq) {
                if (uname == null || uname.isBlank()) continue;
                userRepository.findByUsernameAndDeleted(uname, false).ifPresentOrElse(user -> {
                    addDelivery(userNotifications, saved, user, false);
                }, () -> missing.add(uname));
            }

            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("Some target usernames were not found: " + missing);
            }
            addCreatorDelivery(userNotifications, saved);
            saveAndSendDeliveries(userNotifications, saved);
            return saved;
        }

        // 2) single explicit username
        if (notificationsDto.getTargetUsername() != null && !notificationsDto.getTargetUsername().isBlank()) {
            String username = notificationsDto.getTargetUsername();
            User user = userRepository.findByUsernameAndDeleted(username, false)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

            addDelivery(userNotifications, saved, user, false);
            addCreatorDelivery(userNotifications, saved);
            saveAndSendDeliveries(userNotifications, saved);

            return saved;
        }

        // 3) group by role
        if (!targetGroups.isEmpty()) {
            for (String targetGroup : targetGroups) {
                List<User> users = userRepository.findUsersByRole(targetGroup);
                for (User user : users) {
                    addDelivery(userNotifications, saved, user, false);
                }
            }

            addCreatorDelivery(userNotifications, saved);
            saveAndSendDeliveries(userNotifications, saved);

            for (String targetGroup : targetGroups) {
                try {
                    messagingTemplate.convertAndSend("/topic/notifications/role." + targetGroup, toPayload(saved, false));
                } catch (Exception ignored) {}
            }
        }

        return saved;
    }

    // MARK AS READ by username
    public void markAsRead(String username, Long notificationId) {
        UserNotification un = userNotificationRepository.findByUserUsernameAndNotificationId(username, notificationId)
                .orElseThrow(() -> new IllegalArgumentException("User notification not found for user: " + username));
        un.setIsRead(true);
        un.setReadAt(LocalDateTime.now());
        userNotificationRepository.save(un);

        // notify the user about the read update
        NotificationPayload p = new NotificationPayload();
        p.id = notificationId;
        p.message = un.getNotification().getMessage();
        p.createdBy = un.getNotification().getCreatedBy();
        p.createdAt = un.getNotification().getCreatedAt();
        p.targetGroup = un.getNotification().getTargetGroup();
        p.targetUsername = un.getNotification().getTargetUsername();
        p.actionType = un.getNotification().getActionType();
        p.actionRefId = un.getNotification().getActionRefId();
        p.actionStatus = un.getNotification().getActionStatus();
        p.read = true;
        p.parentId = un.getNotification().getParentId();
        p.editedAt = un.getNotification().getEditedAt();
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/notification-updates", p);
        } catch (Exception ignored) {}
    }

    // SOFT DELETE by username
    public void deleteNotification(String username, Long notificationId) {
        UserNotification un = userNotificationRepository.findByUserUsernameAndNotificationId(username, notificationId)
                .orElseThrow(() -> new IllegalArgumentException("User notification not found for user: " + username));
        un.setIsDeleted(true);
        userNotificationRepository.save(un);
    }

    // GET USER NOTIFICATIONS by username
    public List<UserNotification> getUserNotifications(String username) {
        return userNotificationRepository.findByUserUsernameAndIsDeletedFalse(username);
    }

    // UNREAD COUNT by username
    public Long getUnreadCount(String username) {
        return userNotificationRepository.countUnreadInboxByUsername(username);
    }

    public List<Notification> getSentNotifications(String username) {
        return notificationRepository.findByCreatedByOrderByCreatedAtDesc(username);
    }

    // EDIT (only creator may edit) - username is actor
    public Notification editNotification(String username, Long notificationId, String newMessage) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));

        if (!Objects.equals(notification.getCreatedBy(), username)) {
            throw new SecurityException("You can only edit notifications you created");
        }

        notification.setMessage(newMessage);
        notification.setEditedAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);

        NotificationPayload payload = new NotificationPayload();
        payload.id = saved.getId();
        payload.message = saved.getMessage();
        payload.createdBy = saved.getCreatedBy();
        payload.createdAt = saved.getCreatedAt();
        payload.targetGroup = saved.getTargetGroup();
        payload.targetUsername = saved.getTargetUsername();
        payload.actionType = saved.getActionType();
        payload.actionRefId = saved.getActionRefId();
        payload.actionStatus = saved.getActionStatus();
        payload.read = false;
        payload.parentId = saved.getParentId();
        payload.editedAt = saved.getEditedAt();

        List<UserNotification> recipients = userNotificationRepository.findByNotificationId(notificationId);
        for (UserNotification un : recipients) {
            try {
                messagingTemplate.convertAndSendToUser(un.getUser().getUsername(), "/queue/notifications", payload);
            } catch (Exception ignored) {}
        }

        if (saved.getTargetGroup() != null && !saved.getTargetGroup().isBlank()) {
            try {
                messagingTemplate.convertAndSend("/topic/notifications/role." + saved.getTargetGroup(), payload);
            } catch (Exception ignored) {}
        }

        return saved; }

    public void updateActionStatus(Long notificationId, String actionStatus) {
        if (notificationId == null) {
            return;
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElse(null);
        if (notification == null) {
            return;
        }

        notification.setActionStatus(actionStatus);
        Notification saved = notificationRepository.save(notification);
        List<UserNotification> recipients = userNotificationRepository.findByNotificationId(notificationId);

        for (UserNotification recipient : recipients) {
            if (recipient.getUser() == null) {
                continue;
            }

            try {
                messagingTemplate.convertAndSendToUser(
                        recipient.getUser().getUsername(),
                        "/queue/notifications",
                        toPayload(saved, Boolean.TRUE.equals(recipient.getIsRead()))
                );
            } catch (Exception ignored) {}
        }

        if (saved.getTargetGroup() != null && !saved.getTargetGroup().isBlank()) {
            for (String targetGroup : saved.getTargetGroup().split(",")) {
                String normalized = targetGroup == null ? "" : targetGroup.trim();
                if (normalized.isBlank()) {
                    continue;
                }

                try {
                    messagingTemplate.convertAndSend("/topic/notifications/role." + normalized, toPayload(saved, false));
                } catch (Exception ignored) {}
            }
        }
    }

    // REPLY (creates a new notification linked to parent) - username is actor
    public Notification replyToNotification(String username, Long parentNotificationId, String message) {
        Notification parent = notificationRepository.findById(parentNotificationId)
                .orElseThrow(() -> new IllegalArgumentException("Parent notification not found: " + parentNotificationId));

        Notification reply = new Notification();
        reply.setMessage(message);
        reply.setCreatedBy(username);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setParentId(parent.getId());
        reply.setTargetGroup(parent.getTargetGroup());

        Notification savedReply = notificationRepository.save(reply);

        List<UserNotification> parentRecipients = userNotificationRepository.findByNotificationId(parent.getId());
        List<UserNotification> replyUserNotifications = new ArrayList<>();

        for (UserNotification pun : parentRecipients) {
            User user = pun.getUser();
            if (user != null && !Objects.equals(user.getUsername(), username)) {
                addDelivery(replyUserNotifications, savedReply, user, false);
            }
        }

        String parentCreator = parent.getCreatedBy();
        if (parentCreator != null && !parentCreator.isBlank() && !Objects.equals(parentCreator, username)) {
            userRepository.findByUsernameAndDeleted(parentCreator, false)
                    .ifPresent(user -> addDelivery(replyUserNotifications, savedReply, user, false));
        }

        saveAndSendDeliveries(replyUserNotifications, savedReply);

        return savedReply;
    }
}
