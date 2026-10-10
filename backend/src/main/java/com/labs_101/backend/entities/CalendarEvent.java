package com.labs_101.backend.entities;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import com.labs_101.backend.entities.workout.Workout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "calendar_event")
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private Instant startDate;
    private Instant endDate;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User creator;
    /** The workout planned for this event, null for a plain event. */
    @ManyToOne
    @JoinColumn(name = "workout_id")
    private Workout workout;
    /** People invited by the creator, they see the event in their calendar too. */
    @ManyToMany
    @JoinTable(name = "calendar_event_invitee", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> invitees = new LinkedHashSet<>();
}
