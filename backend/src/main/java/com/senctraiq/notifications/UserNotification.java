package com.senctraiq.notifications;

import com.senctraiq.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity

public class UserNotification {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne
        @JoinColumn(name = "user_id")
        private User user;

        @ManyToOne
        @JoinColumn(name = "notification_id")
        private Notification notification;

        private Boolean isRead = false;
        private Boolean isDeleted = false;

        private LocalDateTime readAt;
}
