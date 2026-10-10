package com.labs_101.backend.mapper;

import com.labs_101.backend.dtos.notification.NotificationDto;
import com.labs_101.backend.entities.Notification;

public class NotificationMapper {
    public static NotificationDto fromNotification(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.getLinkLabel(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
