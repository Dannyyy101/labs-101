package com.labs_101.backend.entities;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.labs_101.backend.entities.workout.WorkoutSession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A friend cheering on a running workout, once per friend and workout. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cheer", uniqueConstraints = @UniqueConstraint(columnNames = { "sender_id", "session_id" }))
public class Cheer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User sender;

    // the one training is the user of the session
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WorkoutSession session;

    @Column(nullable = false)
    private Instant createdAt;

    public Cheer(User sender, WorkoutSession session, Instant createdAt) {
        this.sender = sender;
        this.session = session;
        this.createdAt = createdAt;
    }
}
