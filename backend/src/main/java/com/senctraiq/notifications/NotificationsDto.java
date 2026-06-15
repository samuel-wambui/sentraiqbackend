package com.senctraiq.notifications;

import lombok.Data;

import java.util.List;

@Data
public class NotificationsDto {
    public String message;
    public String createdBy;
    public String targetGroup;              // e.g. "ADMIN"
    public List<String> targetGroups;       // e.g. ["ADMIN", "ANALYST"]
    public String targetUsername;           // single username (optional, backwards-compatible)
    public List<String> targetUsernames;
    public String actionType;
    public String actionRefId;
    public String actionStatus;
}
