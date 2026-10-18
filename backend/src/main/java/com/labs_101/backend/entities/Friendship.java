package com.labs_101.backend.entities;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A friend request from {@code requester} to {@code addressee}, the two are
 * friends once it is accepted. There is at most one row per pair, in whichever
 * direction the request was sent (FriendService checks both).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "friendship",
        uniqueConstraints = @UniqueConstraint(columnNames = { "requester_id", "addressee_id" }),
        indexes = @Index(name = "idx_friendship_addressee", columnList = "addressee_id"))
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User addressee;

    @Column(nullable = false)
    private Instant createdAt;

    /** null while the request is open */
    private Instant acceptedAt;

    public Friendship(User requester, User addressee, Instant createdAt) {
        this.requester = requester;
        this.addressee = addressee;
        this.createdAt = createdAt;
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    /** The one of the two that isn't {@code userId}. */
    public User other(String userId) {
        return requester.getId().equals(userId) ? addressee : requester;
    }
}
