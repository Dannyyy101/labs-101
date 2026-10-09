package com.labs_101.backend.dtos.notification;

import java.time.Instant;

import com.labs_101.backend.entities.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private String linkLabel;
    private Instant createdAt;
    private Instant readAt;
}
