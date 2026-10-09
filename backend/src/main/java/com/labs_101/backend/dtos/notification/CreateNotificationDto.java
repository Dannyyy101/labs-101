package com.labs_101.backend.dtos.notification;

import com.labs_101.backend.entities.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Only the title is required, the type defaults to INFO. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateNotificationDto {
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private String linkLabel;

    public CreateNotificationDto(String title) {
        this.title = title;
    }
}
