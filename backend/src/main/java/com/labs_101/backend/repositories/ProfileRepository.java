package com.labs_101.backend.repositories;

import static com.labs_101.backend.repositories.HealthSampleRepository.getInstant;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Tracked meals and workouts per day for the profile page, the runs come from {@link RunRepository}. */
@Repository
public class ProfileRepository {

    /** @param kcal energy of everything tracked on the day */
    public record MealDayRow(LocalDate day, int meals, double kcal) {
    }

    public record WorkoutRow(Instant startedAt, Instant endedAt) {
    }

    private final JdbcTemplate jdbc;

    public ProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Oldest first. Foods without a meal count as one meal of their own. */
    public List<MealDayRow> mealDays(String userId, ZoneId zone) {
        return jdbc.query("""
                SELECT (fu.create_date AT TIME ZONE ?)::date AS day,
                    count(DISTINCT coalesce(fu.meal, '')) AS meals,
                    sum(coalesce(f.kcal, 0) * coalesce(fu.amount, 0) * coalesce(p.grams, 1) / 100) AS kcal
                FROM food_user fu
                JOIN food f ON f.id = fu.food_id
                LEFT JOIN food_portion p ON p.id = fu.portion_id
                WHERE fu.user_id = ? AND fu.create_date IS NOT NULL
                GROUP BY day ORDER BY day
                """, (rs, i) -> new MealDayRow(rs.getObject("day", LocalDate.class), rs.getInt("meals"),
                rs.getDouble("kcal")), zone.getId(), userId);
    }

    /** Finished sessions, oldest first. */
    public List<WorkoutRow> workouts(String userId) {
        return jdbc.query("""
                SELECT started_at, ended_at FROM workout_session
                WHERE user_id = ? AND ended_at IS NOT NULL ORDER BY started_at
                """, (rs, i) -> new WorkoutRow(getInstant(rs, "started_at"), getInstant(rs, "ended_at")), userId);
    }

    public Integer calorieGoal(String userId) {
        return jdbc.query("SELECT calorie_goal FROM user_settings WHERE user_id = ?",
                (rs, i) -> rs.getObject("calorie_goal", Integer.class), userId).stream().findFirst().orElse(null);
    }
}
