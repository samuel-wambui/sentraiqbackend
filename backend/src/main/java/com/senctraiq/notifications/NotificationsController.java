package com.senctraiq.notifications;


import com.senctraiq.ApiResponse.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users/notifications")
@CrossOrigin(origins = "http://localhost:8080")
public class NotificationsController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/createNotification")
    public ResponseEntity<ApiResponse<Notification>> createNotification(@RequestBody NotificationsDto dto) {
        ApiResponse<Notification> response = new ApiResponse<>();
        try {
            Notification notification = notificationService.createNotification(dto);
            response.setEntity(notification);
            response.setMessage("Notification created successfully");
            response.setStatusCode(HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.BAD_REQUEST.value());
            response.setMessage(iae.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Mark as read using username
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long notificationId, @RequestParam String username) {
        ApiResponse<String> response = new ApiResponse<>();
        try {
            notificationService.markAsRead(username, notificationId);
            response.setEntity("SUCCESS");
            response.setMessage("Notification marked as read");
            response.setStatusCode(HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(iae.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Soft delete using username
    @DeleteMapping("/deleteNotification/{notificationId}")
    public ResponseEntity<ApiResponse<String>> deleteNotification(@PathVariable Long notificationId, @RequestParam String username) {
        ApiResponse<String> response = new ApiResponse<>();
        try {
            notificationService.deleteNotification(username, notificationId);
            response.setEntity("SUCCESS");
            response.setMessage("Notification deleted successfully");
            response.setStatusCode(HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(iae.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get user notifications by username
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<List<UserNotification>>> getUserNotifications(@PathVariable String username) {
        ApiResponse<List<UserNotification>> response = new ApiResponse<>();
        try {
            List<UserNotification> notifications = notificationService.getUserNotifications(username);
            response.setEntity(notifications);
            response.setMessage("Notifications fetched successfully");
            response.setStatusCode(HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(iae.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get unread count by username
    @GetMapping("/user/{username}/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@PathVariable String username) {
        ApiResponse<Long> response = new ApiResponse<>();
        try {
            Long count = notificationService.getUnreadCount(username);
            response.setEntity(count);
            response.setMessage("Unread count fetched successfully");
            response.setStatusCode(HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(iae.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/sent/{username}")
    public ResponseEntity<ApiResponse<List<Notification>>> getSentNotifications(@PathVariable String username) {
        ApiResponse<List<Notification>> response = new ApiResponse<>();
        try {
            List<Notification> notifications = notificationService.getSentNotifications(username);
            response.setEntity(notifications);
            response.setMessage("Sent notifications fetched successfully");
            response.setStatusCode(HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Edit notification (authenticated principal or fallback)
    @PutMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Notification>> editNotification(@PathVariable Long notificationId,
                                                                      @RequestBody EditNotificationDto dto,
                                                                      Principal principal) {
        ApiResponse<Notification> response = new ApiResponse<>();
        try {
            String username = principal != null ? principal.getName() : null;
            if (username == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                response.setMessage("Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Notification updated = notificationService.editNotification(username, notificationId, dto.getMessage());
            response.setEntity(updated);
            response.setMessage("Notification updated");
            response.setStatusCode(HttpStatus.OK.value());
            return ResponseEntity.ok(response);
        } catch (SecurityException se) {
            response.setStatusCode(HttpStatus.FORBIDDEN.value());
            response.setMessage(se.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(iae.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Reply to notification (authenticated principal)
    @PostMapping("/{notificationId}/reply")
    public ResponseEntity<ApiResponse<Notification>> replyToNotification(@PathVariable Long notificationId,
                                                                         @RequestBody ReplyDto dto,
                                                                         Principal principal) {
        ApiResponse<Notification> response = new ApiResponse<>();
        try {
            String username = principal != null ? principal.getName() : null;
            if (username == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED.value());
                response.setMessage("Unauthorized");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Notification reply = notificationService.replyToNotification(username, notificationId, dto.getMessage());
            response.setEntity(reply);
            response.setMessage("Reply created");
            response.setStatusCode(HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException iae) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(iae.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
