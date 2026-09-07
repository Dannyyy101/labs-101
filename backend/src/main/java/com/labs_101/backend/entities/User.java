package com.labs_101.backend.entities;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "\"user\"")
public class User {
    @Id
    private String id;
    private String name;
    private String email;

    @Column(name = "\"emailVerified\"")
    private Boolean emailVerified;

    private String image;

    @Column(name = "\"createdAt\"")
    private Instant createdAt;

    @Column(name = "\"updatedAt\"")
    private Instant updatedAt;

    @OneToMany(mappedBy = "creator")
    private List<CalendarEvent> calendarEvents;

    @OneToMany(mappedBy = "user")
    private List<FoodUser> trackedFood;

    public User(String id) {
        this.id = id;
    }
}
