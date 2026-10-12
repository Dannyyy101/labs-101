package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.profile.AchievementDto;
import com.labs_101.backend.dtos.profile.ActivityDayDto;
import com.labs_101.backend.dtos.profile.ProfileDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.repositories.LoginDayRepository;
import com.labs_101.backend.repositories.ProfileRepository;
import com.labs_101.backend.repositories.RunRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.ProfileService;
import com.labs_101.backend.services.RunService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ ProfileService.class, ProfileRepository.class, LoginDayRepository.class, RunService.class,
        RunRepository.class })
public class ProfileTest {
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Autowired
    private ProfileService profileService;
    @Autowired
    private LoginDayRepository loginDayRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private String userId;
    private long foodId;

    @BeforeEach
    void setUp() {
        userId = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
        // 500 kcal per 100 g
        foodId = jdbc.queryForObject("INSERT INTO food (name, kcal, create_date, update_date) VALUES ('Testessen', 500, now(), now()) RETURNING id",
                Long.class);
    }

    private void login(LocalDate day) {
        jdbc.update("INSERT INTO login_day (user_id, day) VALUES (?, ?)", userId, day);
    }

    // 100 g, 23:30 local time so the day must come from the zone and not UTC
    private void track(LocalDate day, String meal, double grams) {
        jdbc.update("INSERT INTO food_user (user_id, food_id, amount, meal, create_date, update_date) VALUES (?, ?, ?, ?, ?, now())",
                userId, foodId, grams, meal, ZonedDateTime.of(day.atTime(23, 30), ZONE).toOffsetDateTime());
    }

    private void workout(LocalDate day, int minutes) {
        ZonedDateTime start = ZonedDateTime.of(day.atTime(18, 0), ZONE);
        jdbc.update("INSERT INTO workout_session (user_id, name, started_at, ended_at, log) VALUES (?, 'Push', ?, ?, '{\"exercises\":[]}'::jsonb)",
                userId, start.toOffsetDateTime(), start.plusMinutes(minutes).toOffsetDateTime());
    }

    private static Map<String, AchievementDto> byId(ProfileDto profile) {
        return profile.achievements().stream().collect(Collectors.toMap(AchievementDto::id, Function.identity()));
    }

    @Test
    void testStreakCountsUntilYesterday() {
        // record of 4, then a gap, then 3 days up to yesterday
        for (int i = 10; i < 14; i++)
            login(TODAY.minusDays(i));
        for (int i = 1; i <= 3; i++)
            login(TODAY.minusDays(i));

        ProfileDto profile = profileService.getProfile(userId, TODAY, ZONE);
        assertEquals(3, profile.streak().current());
        assertEquals(4, profile.streak().record());
        assertFalse(profile.streak().today());

        login(TODAY);
        profile = profileService.getProfile(userId, TODAY, ZONE);
        assertEquals(4, profile.streak().current());
        assertTrue(profile.streak().today());
    }

    @Test
    void testStreakAchievement() {
        for (int i = 0; i < 7; i++)
            login(TODAY.minusDays(20 - i));

        Map<String, AchievementDto> achievements = byId(profileService.getProfile(userId, TODAY, ZONE));
        assertEquals(TODAY.minusDays(14), achievements.get("STREAK_7").achievedAt());
        assertNull(achievements.get("STREAK_30").achievedAt());
        assertEquals(7, achievements.get("STREAK_30").progress());
    }

    @Test
    void testMarkTodayOnlyOnce() {
        loginDayRepository.markToday(userId);
        loginDayRepository.markToday(userId);
        assertEquals(1, loginDayRepository.days(userId).size());
    }

    @Test
    void testMealsCountPerMealAndDay() {
        LocalDate day = TODAY.minusDays(2);
        track(day, "BREAKFAST", 100);
        track(day, "BREAKFAST", 50);
        track(day, "DINNER", 100);
        for (int i = 0; i < 8; i++)
            track(TODAY.minusDays(30 + i), "LUNCH", 100);

        ProfileDto profile = profileService.getProfile(userId, TODAY, ZONE);
        ActivityDayDto activity = profile.days().stream().filter(d -> d.date().equals(day)).findFirst().orElseThrow();
        assertEquals(2, activity.meals());

        AchievementDto meals = byId(profile).get("MEALS_10");
        assertEquals(10, meals.progress());
        assertEquals(day, meals.achievedAt());
    }

    @Test
    void testCalorieGoalStreak() {
        jdbc.update("INSERT INTO user_settings (user_id, calorie_goal) VALUES (?, 2000)", userId);
        // 400 g of 500 kcal = 2000 kcal, the 4th day is 2500 kcal and breaks the streak
        for (int i = 0; i < 10; i++)
            track(TODAY.minusDays(20 - i), "LUNCH", i == 3 ? 500 : 400);

        AchievementDto goal = byId(profileService.getProfile(userId, TODAY, ZONE)).get("CALORIE_GOAL_7");
        assertEquals(6, goal.progress());
        assertNull(goal.achievedAt());

        track(TODAY.minusDays(10), "LUNCH", 410);
        goal = byId(profileService.getProfile(userId, TODAY, ZONE)).get("CALORIE_GOAL_7");
        assertEquals(TODAY.minusDays(10), goal.achievedAt());
    }

    @Test
    void testWorkouts() {
        for (int i = 0; i < 10; i++)
            workout(TODAY.minusDays(40 - i), 50);

        ProfileDto profile = profileService.getProfile(userId, TODAY, ZONE);
        Map<String, AchievementDto> achievements = byId(profile);
        assertEquals(TODAY.minusDays(40), achievements.get("WORKOUTS_1").achievedAt());
        assertEquals(TODAY.minusDays(31), achievements.get("WORKOUTS_10").achievedAt());
        assertNull(achievements.get("WORKOUTS_50").achievedAt());

        ActivityDayDto day = profile.days().getFirst();
        assertEquals(1, day.workouts());
        assertEquals(50, day.workoutMinutes(), 0.01);
    }

    @Test
    void testHeatmapLeavesOutOlderDays() {
        login(TODAY.minusDays(400));
        login(TODAY);

        ProfileDto profile = profileService.getProfile(userId, TODAY, ZONE);
        assertEquals(1, profile.days().size());
        assertEquals(TODAY, profile.days().getFirst().date());
    }
}
