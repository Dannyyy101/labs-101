package com.labs_101.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.dtos.notification.NotificationDto;
import com.labs_101.backend.dtos.notification.UnreadCountDto;
import com.labs_101.backend.services.NotificationService;
import com.labs_101.backend.security.CurrentUser;

@RestController()
@RequestMapping("/api/users/me/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("")
    public List<NotificationDto> getNotifications(@CurrentUser String userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer limit) {
        return notificationService.getNotifications(userId, unreadOnly, limit);
    }

    @GetMapping("/unread-count")
    public UnreadCountDto getUnreadCount(@CurrentUser String userId) {
        return new UnreadCountDto(notificationService.countUnread(userId));
    }

    @PostMapping("")
    public ResponseEntity<NotificationDto> createNotification(@CurrentUser String userId,
            @RequestBody CreateNotificationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(userId, dto));
    }

    @PutMapping("/{id}/read")
    public NotificationDto markRead(@CurrentUser String userId, @PathVariable Long id) {
        return notificationService.markRead(userId, id);
    }

    @DeleteMapping("/{id}/read")
    public NotificationDto markUnread(@CurrentUser String userId, @PathVariable Long id) {
        return notificationService.markUnread(userId, id);
    }

    @PutMapping("/read")
    public ResponseEntity<Void> markAllRead(@CurrentUser String userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@CurrentUser String userId, @PathVariable Long id) {
        notificationService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
