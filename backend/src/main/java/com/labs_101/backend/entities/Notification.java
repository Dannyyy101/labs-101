package com.labs_101.backend.entities;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A message for one user. Everything except the title is optional,
 * e.g. a link only makes sense when there is a page to open.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notification_user_created", columnList = "user_id, createdAt"))
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // notifications are deleted together with the user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String message;

    /** Page opened when clicking the notification, a path of the app ("/workouts/3") or an http(s) url. */
    @Column(length = 2048)
    private String link;

    /** Text of the link button, the client shows a default without one. */
    private String linkLabel;

    @Column(nullable = false)
    private Instant createdAt;

    /** null while unread */
    private Instant readAt;
}
