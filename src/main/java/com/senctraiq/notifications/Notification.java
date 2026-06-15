package com.senctraiq.notifications;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;
    public String message;
    public String targetGroup;
    public String targetUsername;
    public String createdBy;
    public LocalDateTime createdAt;
    public String actionType;
    public String actionRefId;
    public String actionStatus;

    // NEW: link to parent notification (for replies)
    public Long parentId;

    // NEW: last edited timestamp (nullable)
    public LocalDateTime editedAt;
}
