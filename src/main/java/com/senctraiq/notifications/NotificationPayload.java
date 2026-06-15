package com.senctraiq.notifications;

import java.time.LocalDateTime;

public class NotificationPayload {
    public Long id;
    public String message;
    public String createdBy;
    public LocalDateTime createdAt;
    public String targetGroup;
    public String targetUsername;
    public String actionType;
    public String actionRefId;
    public String actionStatus;
    public boolean read;
    public Long parentId;           // NEW
    public LocalDateTime editedAt;  // NEW

    public NotificationPayload() {}

    public NotificationPayload(Long id, String message, String createdBy, LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.message = message;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.read = read;
    }
}
