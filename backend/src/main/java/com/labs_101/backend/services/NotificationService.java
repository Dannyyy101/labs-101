package com.labs_101.backend.services;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.dtos.notification.NotificationDto;
import com.labs_101.backend.entities.Notification;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.mapper.NotificationMapper;
import com.labs_101.backend.repositories.NotificationRepository;
import com.labs_101.backend.repositories.UserRepository;

/**
 * Notifications for a user, created through the API or by other services with {@link #notify}.
 */
@Service
public class NotificationService {

    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 200;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /** Newest first. */
    public List<NotificationDto> getNotifications(String userId, boolean unreadOnly, Integer limit) {
        PageRequest page = PageRequest.of(0, limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT));
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, page)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, page);
        return notifications.stream().map(NotificationMapper::fromNotification).toList();
    }

    public long countUnread(String userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    public NotificationDto create(String userId, CreateNotificationDto dto) {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException.user(userId);
        }
        validate(dto);

        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setType(dto.getType() == null ? NotificationType.INFO : dto.getType());
        notification.setTitle(dto.getTitle().strip());
        notification.setMessage(blankToNull(dto.getMessage()));
        notification.setLink(blankToNull(dto.getLink()));
        notification.setLinkLabel(notification.getLink() == null ? null : blankToNull(dto.getLinkLabel()));
        notification.setCreatedAt(Instant.now());
        return NotificationMapper.fromNotification(notificationRepository.save(notification));
    }

    /** Shortcut for other services, e.g. {@code notify(userId, SUCCESS, "Training beendet", null, "/workouts")}. */
    public NotificationDto notify(String userId, NotificationType type, String title, String message, String link) {
        return create(userId, new CreateNotificationDto(type, title, message, link, null));
    }

    public NotificationDto markRead(String userId, Long id) {
        return setRead(userId, id, true);
    }

    public NotificationDto markUnread(String userId, Long id) {
        return setRead(userId, id, false);
    }

    @Transactional
    public int markAllRead(String userId) {
        return notificationRepository.markAllRead(userId, Instant.now());
    }

    public void delete(String userId, Long id) {
        notificationRepository.delete(findNotification(userId, id));
    }

    private NotificationDto setRead(String userId, Long id, boolean read) {
        Notification notification = findNotification(userId, id);
        // keep the first read time when it is marked read again
        if (!read) {
            notification.setReadAt(null);
        } else if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return NotificationMapper.fromNotification(notificationRepository.save(notification));
    }

    // only the user's own notifications, others are reported as not found
    private Notification findNotification(String userId, Long id) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> NotFoundException.notification(id));
    }

    private void validate(CreateNotificationDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw BadRequestException.notificationTitle();
        }
        String link = blankToNull(dto.getLink());
        if (link != null && !isAllowedLink(link)) {
            throw BadRequestException.notificationLink(link);
        }
    }

    /**
     * A path of the app or an absolute http(s) url, anything else (e.g. "javascript:")
     * would run in the browser of the user clicking it.
     */
    static boolean isAllowedLink(String link) {
        // "//host" is a url to another host without a scheme
        if (link.startsWith("/")) {
            return !link.startsWith("//") && !link.startsWith("/\\");
        }
        try {
            URI uri = new URI(link);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
