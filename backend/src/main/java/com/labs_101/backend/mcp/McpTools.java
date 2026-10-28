package com.labs_101.backend.mcp;

import static com.labs_101.backend.mcp.McpTool.date;
import static com.labs_101.backend.mcp.McpTool.integer;
import static com.labs_101.backend.mcp.McpTool.schema;
import static com.labs_101.backend.mcp.McpTool.string;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.labs_101.backend.repositories.LoginDayRepository;
import com.labs_101.backend.services.CalendarService;
import com.labs_101.backend.services.FoodService;
import com.labs_101.backend.services.HealthOverviewService;
import com.labs_101.backend.services.HealthService;
import com.labs_101.backend.services.ProfileService;
import com.labs_101.backend.services.RunService;
import com.labs_101.backend.services.WorkoutService;
import com.labs_101.backend.services.WorkoutSessionService;

/**
 * The tools of the MCP server, so Claude can analyze training, health and
 * nutrition of the signed in user. They only read, nothing is changed.
 */
@Component
class McpTools {
    private static final Map<String, Object> RANGE = Map.of(
            "from", date("First day (YYYY-MM-DD), included"),
            "to", date("Last day (YYYY-MM-DD), included, defaults to today"));

    private final Map<String, McpTool> tools;

    McpTools(ProfileService profileService, HealthOverviewService overviewService, HealthService healthService,
            WorkoutSessionService sessionService, WorkoutService workoutService, RunService runService,
            FoodService foodService, CalendarService calendarService) {
        List<McpTool> list = List.of(
                new McpTool("get_profile", "Profil",
                        "Login streak, days with activity in the last year, achievements and the daily calorie goal of the user.",
                        schema(Map.of()),
                        (userId, args) -> profileService.getProfile(userId, LocalDate.now(LoginDayRepository.ZONE),
                                LoginDayRepository.ZONE)),

                new McpTool("get_daily_health_overview", "Tagesübersicht",
                        "Sleep, recovery, strain, activity, vitals, heart rate and workouts of one day plus the scores of the "
                                + "days before, as on the home page of the app. Times are in seconds, energy in kcal, distances in "
                                + "meters, days are counted in Europe/Berlin.",
                        schema(Map.of("date", date("The day (YYYY-MM-DD), defaults to today"))),
                        (userId, args) -> overviewService.getOverview(userId,
                                args.date("date", LocalDate.now(McpArguments.ZONE)), McpArguments.ZONE)),

                new McpTool("list_health_data_types", "Health-Datentypen",
                        "The Apple Health data types synced for the user with their number of samples and time span. "
                                + "Use a type with get_health_samples.",
                        schema(Map.of()),
                        (userId, args) -> healthService.getSummary(userId)),

                new McpTool("get_health_samples", "Health-Samples",
                        "Raw Apple Health samples of one type (e.g. HKQuantityTypeIdentifierHeartRate), newest first. "
                                + "Prefer short ranges, a type like heart rate has thousands of samples a day.",
                        schema(withRange(Map.of(
                                "type", string("The data type, see list_health_data_types"),
                                "limit", integer("Maximum number of samples, defaults to 200", 1, HealthService.MAX_PAGE_SIZE))),
                                "type"),
                        (userId, args) -> {
                            McpArguments.Range range = args.range(7, 366);
                            return healthService.getSamples(userId, args.requiredString("type"), range.start(), range.end(), 0,
                                    args.integer("limit", 200, 1, HealthService.MAX_PAGE_SIZE));
                        }),

                new McpTool("list_workouts", "Trainings",
                        "Finished strength workouts with their exercises, done sets (reps, weight in kg, RPE) and notes, "
                                + "newest first. Defaults to the last 30 days.",
                        schema(RANGE),
                        (userId, args) -> {
                            McpArguments.Range range = args.range(30, 366);
                            return sessionService.getSessions(userId, range.start(), range.end());
                        }),

                new McpTool("list_workout_templates", "Trainingspläne",
                        "The workout templates of the user with their planned exercises and sets.",
                        schema(Map.of()),
                        (userId, args) -> workoutService.getAll(userId)),

                new McpTool("list_runs", "Läufe",
                        "Runs from Apple Health with distance (m), moving time (s), elevation, heart rate, heart rate zones "
                                + "and best efforts, newest first. Defaults to the last 30 days.",
                        schema(RANGE),
                        (userId, args) -> {
                            McpArguments.Range range = args.range(30, 366);
                            return runService.getRuns(userId, range.start(), range.end());
                        }),

                new McpTool("get_run", "Lauf",
                        "One run with its splits, heart rate and route, the id comes from list_runs.",
                        schema(Map.of("id", string("Id of the run")), "id"),
                        (userId, args) -> runService.getRun(userId, uuid(args.requiredString("id")))),

                new McpTool("get_nutrition", "Ernährung",
                        "Tracked meals per day with the foods, their nutrients per 100 g and the eaten amount in g. "
                                + "Defaults to the last 7 days, at most 31 days at once.",
                        schema(RANGE),
                        (userId, args) -> {
                            McpArguments.Range range = args.range(7, 31);
                            List<Map<String, Object>> days = new ArrayList<>();
                            // the app counts the tracked foods in UTC days
                            for (LocalDate day = range.to(); !day.isBefore(range.from()); day = day.minusDays(1)) {
                                Map<String, Object> entry = new LinkedHashMap<>();
                                entry.put("date", day.toString());
                                entry.put("entries", foodService.getTrackedFoodForUser(userId,
                                        day.atStartOfDay(ZoneOffset.UTC).toInstant()));
                                days.add(entry);
                            }
                            return days;
                        }),

                new McpTool("list_calendar_events", "Kalender",
                        "Planned events of the user's calendar (created or invited to). Defaults to the next 14 days "
                                + "when no range is given.",
                        schema(RANGE),
                        (userId, args) -> {
                            McpArguments.Range range = args.string("from") == null && args.string("to") == null
                                    ? new McpArguments.Range(LocalDate.now(McpArguments.ZONE),
                                            LocalDate.now(McpArguments.ZONE).plusDays(13))
                                    : args.range(14, 366);
                            return calendarService.getAllCalendarEvents(userId, range.start(), range.end());
                        }));

        tools = list.stream().collect(Collectors.toMap(McpTool::name, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    List<McpTool> all() {
        return List.copyOf(tools.values());
    }

    Optional<McpTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    private static Map<String, Object> withRange(Map<String, Object> properties) {
        Map<String, Object> all = new LinkedHashMap<>(properties);
        all.putAll(RANGE);
        return all;
    }

    private static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("id has to be the id of a run from list_runs");
        }
    }
}
