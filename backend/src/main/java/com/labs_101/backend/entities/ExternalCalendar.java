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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A calendar of the user in another app, its events are shown in the user's calendar but aren't stored. A calendar
 * of an {@link AppleAccount} is read over CalDAV, otherwise {@link #url} is the iCalendar feed of a shared calendar.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "external_calendar")
public class ExternalCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    private String name;

    /**
     * https, a webcal link is stored with https. Anyone with the link of a shared calendar can read it, the CalDAV
     * url of an account's calendar needs the account's password.
     */
    @Column(length = 2048)
    private String url;

    private Instant createdAt;

    /** Null for a shared calendar, removed together with the account. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppleAccount account;
}
