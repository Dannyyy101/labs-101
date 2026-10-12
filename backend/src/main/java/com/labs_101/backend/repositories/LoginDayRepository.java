package com.labs_101.backend.repositories;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Days a user used the app, the login streak of the profile page counts them.
 * Every request of the user marks the day, the ids already marked today are
 * kept in memory so only the first request of the day hits the database.
 */
@Repository
public class LoginDayRepository {

    /** Days change at midnight in Berlin, like the rest of the app without a zone. */
    public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private final JdbcTemplate jdbc;

    private volatile LocalDate markedDay = LocalDate.MIN;
    private final Set<String> markedUsers = ConcurrentHashMap.newKeySet();

    public LoginDayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void markToday(String userId) {
        LocalDate today = LocalDate.now(ZONE);
        if (!today.equals(markedDay)) {
            markedUsers.clear();
            markedDay = today;
        }
        if (!markedUsers.add(userId))
            return;
        jdbc.update("INSERT INTO login_day (user_id, day) VALUES (?, ?) ON CONFLICT DO NOTHING", userId, today);
    }

    /** Oldest first. */
    public List<LocalDate> days(String userId) {
        return jdbc.query("SELECT day FROM login_day WHERE user_id = ? ORDER BY day",
                (rs, i) -> rs.getObject("day", LocalDate.class), userId);
    }
}
