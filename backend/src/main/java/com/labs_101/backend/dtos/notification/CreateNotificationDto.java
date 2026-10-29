package com.labs_101.backend.dtos.notification;

import org.jspecify.annotations.Nullable;

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
    private @Nullable NotificationType type;
    private String title;
    private @Nullable String message;
    private @Nullable String link;
    private @Nullable String linkLabel;

    public CreateNotificationDto(String title) {
        this.title = title;
    }
}
