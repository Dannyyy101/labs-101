package com.labs_101.backend.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.profile.AchievementDto;
import com.labs_101.backend.dtos.profile.ActivityDayDto;
import com.labs_101.backend.dtos.profile.ProfileDto;
import com.labs_101.backend.dtos.profile.StreakDto;
import com.labs_101.backend.dtos.run.BestEffortDto;
import com.labs_101.backend.dtos.run.RunSummaryDto;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.LoginDayRepository;
import com.labs_101.backend.repositories.ProfileRepository;
import com.labs_101.backend.repositories.ProfileRepository.MealDayRow;
import com.labs_101.backend.repositories.ProfileRepository.WorkoutRow;
import com.labs_101.backend.repositories.UserRepository;

/**
 * The profile page: login streak, a year of activity as heatmap and the
 * achievements. Achievements come in tiers per category (10, 100, 500 meals,
 * …), so there is always a next one within reach. Each one knows its
 * progress and the day it was reached, computed from the data every time, so
 * deleting a run also takes back an achievement that depended on it.
 */
@Service
public class ProfileService {

    /** 53 weeks, the heatmap starts on the Monday of the first one. */
    static final int HEATMAP_DAYS = 53 * 7;
    /** Share of the calorie goal a day may be off and still count as reached. */
    static final double CALORIE_TOLERANCE = 0.1;
    static final double SUB_25_SECONDS = 25 * 60;

    private final UserRepository userRepository;
    private final LoginDayRepository loginDayRepository;
    private final ProfileRepository profileRepository;
    private final RunService runService;

    ProfileService(UserRepository userRepository, LoginDayRepository loginDayRepository,
            ProfileRepository profileRepository, RunService runService) {
        this.userRepository = userRepository;
        this.loginDayRepository = loginDayRepository;
        this.profileRepository = profileRepository;
        this.runService = runService;
    }

    public ProfileDto getProfile(String userId, LocalDate today, ZoneId zone) {
        User user = userRepository.findById(userId).orElseThrow(() -> NotFoundException.user(userId));
        TreeSet<LocalDate> loginDays = new TreeSet<>(loginDayRepository.days(userId));
        List<MealDayRow> meals = profileRepository.mealDays(userId, zone);
        List<WorkoutRow> workouts = profileRepository.workouts(userId);
        List<RunSummaryDto> runs = runService.getAllRuns(userId);
        Integer calorieGoal = profileRepository.calorieGoal(userId);

        return new ProfileDto(user.getCreatedAt(), today, streak(loginDays, today),
                days(today, zone, loginDays, meals, workouts, runs),
                achievements(zone, loginDays, meals, workouts, runs, calorieGoal), calorieGoal);
    }

    // MARK: streak

    static StreakDto streak(TreeSet<LocalDate> days, LocalDate today) {
        boolean usedToday = days.contains(today);
        int current = 0;
        for (LocalDate d = usedToday ? today : today.minusDays(1); days.contains(d); d = d.minusDays(1))
            current++;
        return new StreakDto(current, longestStreak(days), usedToday);
    }

    static int longestStreak(Set<LocalDate> days) {
        return (int) streakReached(new TreeSet<>(days), Integer.MAX_VALUE)[0];
    }

    /**
     * [longest streak, epoch day the streak first reached {@code target} or -1],
     * {@code days} sorted.
     */
    private static long[] streakReached(TreeSet<LocalDate> days, int target) {
        int longest = 0, run = 0;
        long reached = -1;
        LocalDate previous = null;
        for (LocalDate day : days) {
            run = previous != null && previous.plusDays(1).equals(day) ? run + 1 : 1;
            longest = Math.max(longest, run);
            if (run >= target && reached < 0)
                reached = day.toEpochDay();
            previous = day;
        }
        return new long[] { longest, reached };
    }

    // MARK: heatmap

    private static List<ActivityDayDto> days(LocalDate today, ZoneId zone, Set<LocalDate> loginDays,
            List<MealDayRow> meals, List<WorkoutRow> workouts, List<RunSummaryDto> runs) {
        LocalDate first = today.minusDays(HEATMAP_DAYS - 1);
        Map<LocalDate, Day> days = new TreeMap<>();
        for (LocalDate d : loginDays)
            if (!d.isBefore(first))
                days.computeIfAbsent(d, x -> new Day()).loggedIn = true;
        for (MealDayRow m : meals)
            if (!m.day().isBefore(first))
                days.computeIfAbsent(m.day(), x -> new Day()).meals += m.meals();
        for (WorkoutRow w : workouts) {
            LocalDate d = date(w.startedAt(), zone);
            if (!d.isBefore(first)) {
                Day day = days.computeIfAbsent(d, x -> new Day());
                day.workouts++;
                day.workoutMinutes += Duration.between(w.startedAt(), w.endedAt()).toSeconds() / 60.0;
            }
        }
        for (RunSummaryDto r : runs) {
            LocalDate d = date(r.startDate(), zone);
            if (!d.isBefore(first)) {
                Day day = days.computeIfAbsent(d, x -> new Day());
                day.runs++;
                day.runDistance += r.distance();
            }
        }
        return days.entrySet().stream().filter(e -> !e.getKey().isAfter(today))
                .map(e -> e.getValue().toDto(e.getKey())).toList();
    }

    private static final class Day {
        int meals, runs, workouts;
        double runDistance, workoutMinutes;
        boolean loggedIn;

        ActivityDayDto toDto(LocalDate date) {
            return new ActivityDayDto(date, meals, runs, runDistance, workouts, workoutMinutes, loggedIn);
        }
    }

    // MARK: achievements

    private static List<AchievementDto> achievements(ZoneId zone, TreeSet<LocalDate> loginDays,
            List<MealDayRow> meals, List<WorkoutRow> workouts, List<RunSummaryDto> runs, Integer calorieGoal) {
        List<AchievementDto> result = new ArrayList<>();

        // meals: one per breakfast, lunch, … of a day
        List<Amount> mealAmounts = meals.stream().map(m -> new Amount(m.day(), m.meals())).toList();
        for (int target : new int[] { 10, 100, 500, 1000 })
            result.add(cumulative("MEALS_" + target, target, mealAmounts));

        TreeSet<LocalDate> goalDays = new TreeSet<>();
        if (calorieGoal != null && calorieGoal > 0)
            meals.stream().filter(m -> Math.abs(m.kcal() - calorieGoal) <= calorieGoal * CALORIE_TOLERANCE)
                    .forEach(m -> goalDays.add(m.day()));
        result.add(streak("CALORIE_GOAL_7", 7, goalDays));

        // runs
        List<Amount> runCount = runs.stream().map(r -> new Amount(date(r.startDate(), zone), 1)).toList();
        result.add(cumulative("FIRST_RUN", 1, runCount));
        List<Amount> kilometers = runs.stream().map(r -> new Amount(date(r.startDate(), zone), r.distance() / 1000))
                .toList();
        for (int target : new int[] { 100, 500, 1000 })
            result.add(cumulative("KM_" + target, target, kilometers));
        result.add(longestRun("RUN_10K", 10_000, runs, zone));
        result.add(longestRun("HALF_MARATHON", 21_097.5, runs, zone));
        result.add(longestRun("MARATHON", 42_195, runs, zone));
        result.add(sub25(runs, zone));

        // strength workouts
        List<Amount> workoutCount = workouts.stream().map(w -> new Amount(date(w.startedAt(), zone), 1)).toList();
        for (int target : new int[] { 1, 10, 50, 100 })
            result.add(cumulative("WORKOUTS_" + target, target, workoutCount));

        // login streak
        for (int target : new int[] { 7, 30, 100, 365 })
            result.add(streak("STREAK_" + target, target, loginDays));

        return result;
    }

    private record Amount(LocalDate day, double value) {
    }

    /** Sum of all amounts, reached on the day the sum got to the target. {@code amounts} oldest first. */
    private static AchievementDto cumulative(String id, double target, List<Amount> amounts) {
        double sum = 0;
        LocalDate reached = null;
        for (Amount a : amounts) {
            sum += a.value();
            if (reached == null && sum >= target - 1e-9)
                reached = a.day();
        }
        return new AchievementDto(id, sum, target, reached);
    }

    /** Longest streak of days, reached the day the streak first got to the target. */
    private static AchievementDto streak(String id, int target, TreeSet<LocalDate> days) {
        long[] streak = streakReached(days, target);
        return new AchievementDto(id, (double) streak[0], target,
                streak[1] >= 0 ? LocalDate.ofEpochDay(streak[1]) : null);
    }

    /** Longest single run, reached with the first run that got to the target. {@code runs} oldest first. */
    private static AchievementDto longestRun(String id, double target, List<RunSummaryDto> runs, ZoneId zone) {
        double best = 0;
        LocalDate reached = null;
        for (RunSummaryDto r : runs) {
            double v = r.distance();
            best = Math.max(best, v);
            if (reached == null && v >= target)
                reached = date(r.startDate(), zone);
        }
        return new AchievementDto(id, best, target, reached);
    }

    /** Fastest 5 km of any run (best effort, so a 10 km run with a fast first half counts too). */
    private static AchievementDto sub25(List<RunSummaryDto> runs, ZoneId zone) {
        Double best = null;
        LocalDate reached = null;
        for (RunSummaryDto r : runs) {
            for (BestEffortDto effort : r.bestEfforts()) {
                if (effort.distance() != 5000)
                    continue;
                best = best == null ? effort.time() : Math.min(best, effort.time());
                if (reached == null && effort.time() < SUB_25_SECONDS)
                    reached = date(r.startDate(), zone);
            }
        }
        return new AchievementDto("SUB_25", best, SUB_25_SECONDS, reached);
    }

    private static LocalDate date(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }
}
