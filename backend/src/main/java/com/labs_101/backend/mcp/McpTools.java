package com.labs_101.backend.mcp;

import static com.labs_101.backend.mcp.McpTool.array;
import static com.labs_101.backend.mcp.McpTool.date;
import static com.labs_101.backend.mcp.McpTool.dateTime;
import static com.labs_101.backend.mcp.McpTool.enumeration;
import static com.labs_101.backend.mcp.McpTool.id;
import static com.labs_101.backend.mcp.McpTool.integer;
import static com.labs_101.backend.mcp.McpTool.nonNegative;
import static com.labs_101.backend.mcp.McpTool.number;
import static com.labs_101.backend.mcp.McpTool.schema;
import static com.labs_101.backend.mcp.McpTool.string;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.labs_101.backend.dtos.calendar.CalendarEventDto;
import com.labs_101.backend.dtos.calendar.CreateCalendarEventDto;
import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.food.CreateFoodUserDto;
import com.labs_101.backend.dtos.food.FoodPortionDto;
import com.labs_101.backend.dtos.food.FoodWithLastEntryAndPortionsDto;
import com.labs_101.backend.dtos.food.FoodWithPortionsDto;
import com.labs_101.backend.dtos.food.SearchFoodResponseDto;
import com.labs_101.backend.dtos.food.TrackedFoodDto;
import com.labs_101.backend.dtos.workout.CardioItemDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.ExerciseItem;
import com.labs_101.backend.dtos.workout.SetDto;
import com.labs_101.backend.dtos.workout.StrengthItemDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.entities.Recurrence;
import com.labs_101.backend.entities.food.MealType;
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
 * nutrition of the signed in user. Claude can also track meals and remove
 * tracked entries, create workout templates and plan calendar events.
 */
@Component
class McpTools {
    private static final int SEARCH_RESULTS = 8;
    // like the app, see CARDIO_TYPES in the frontend
    private static final List<String> CARDIO_TYPES = List.of("Cardio", "Running", "Swimming");
    private static final int DEFAULT_REST_SECONDS = 90;
    private static final Map<String, Object> SET = schema(Map.of(
            "reps", integer("Repetitions", 1, 1000),
            "weightKg", nonNegative("Weight in kg, 0 for bodyweight"),
            "rpe", integer("Planned RPE", 1, 10),
            "restSeconds", integer("Rest after the set in seconds, defaults to 90, 0 turns the timer off", 0, 3600)),
            "reps");
    private static final Map<String, Object> TEMPLATE_EXERCISE = schema(Map.of(
            "exerciseId", id("Id of the exercise from list_exercises"),
            "sets", array("The sets of a strength exercise, in order", SET),
            "minutes", integer("Minutes of a cardio exercise", 1, 600)),
            "exerciseId");
    private static final Map<String, Object> RANGE = Map.of(
            "from", date("First day (YYYY-MM-DD), included"),
            "to", date("Last day (YYYY-MM-DD), included, defaults to today"));

    private final Map<String, McpTool> tools;

    McpTools(ProfileService profileService, HealthOverviewService overviewService, HealthService healthService,
            WorkoutSessionService sessionService, WorkoutService workoutService, RunService runService,
            FoodService foodService, CalendarService calendarService) {
        List<McpTool> list = List.of(
                McpTool.read("get_profile", "Profil",
                        "Login streak, days with activity in the last year, achievements and the daily calorie goal of the user.",
                        schema(Map.of()),
                        (userId, args) -> profileService.getProfile(userId, LocalDate.now(LoginDayRepository.ZONE),
                                LoginDayRepository.ZONE)),

                McpTool.read("get_daily_health_overview", "Tagesübersicht",
                        "Sleep, recovery, strain, activity, vitals, heart rate and workouts of one day plus the scores of the "
                                + "days before, as on the home page of the app. Times are in seconds, energy in kcal, distances in "
                                + "meters, days are counted in Europe/Berlin.",
                        schema(Map.of("date", date("The day (YYYY-MM-DD), defaults to today"))),
                        (userId, args) -> overviewService.getOverview(userId,
                                args.date("date", LocalDate.now(McpArguments.ZONE)), McpArguments.ZONE)),

                McpTool.read("list_health_data_types", "Health-Datentypen",
                        "The Apple Health data types synced for the user with their number of samples and time span. "
                                + "Use a type with get_health_samples.",
                        schema(Map.of()),
                        (userId, args) -> healthService.getSummary(userId)),

                McpTool.read("get_health_samples", "Health-Samples",
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

                McpTool.read("list_workouts", "Trainings",
                        "Finished strength workouts with their exercises, done sets (reps, weight in kg, RPE) and notes, "
                                + "newest first. Defaults to the last 30 days.",
                        schema(RANGE),
                        (userId, args) -> {
                            McpArguments.Range range = args.range(30, 366);
                            return sessionService.getSessions(userId, range.start(), range.end());
                        }),

                McpTool.read("list_workout_templates", "Trainingspläne",
                        "The workout templates of the user with their planned exercises and sets.",
                        schema(Map.of()),
                        (userId, args) -> workoutService.getAll(userId)),

                McpTool.read("list_exercises", "Übungen",
                        "The exercises of the app with their id and type, for create_workout_template. Exercises of the "
                                + "types " + String.join(", ", CARDIO_TYPES) + " are cardio and are planned in minutes, the "
                                + "others in sets.",
                        schema(Map.of("name", string("Part of the name, e.g. \"Bank\", leave it out for all"))),
                        (userId, args) -> workoutService.getAllExercises(args.string("name"), null).stream()
                                .map(McpTools::exercise).toList()),

                McpTool.write("create_workout_template", "Trainingsplan erstellen",
                        "Creates a workout template for the user, as in the workout editor of the app. Every exercise "
                                + "needs its id from list_exercises and either sets (strength) or minutes (cardio). "
                                + "Tell the user what you created.",
                        schema(Map.of(
                                "name", string("Name of the template, e.g. \"Push\""),
                                "exercises", array("The exercises in order", TEMPLATE_EXERCISE)),
                                "name", "exercises"),
                        false,
                        (userId, args) -> createWorkoutTemplate(workoutService, userId, args)),

                McpTool.read("list_runs", "Läufe",
                        "Runs from Apple Health with distance (m), moving time (s), elevation, heart rate, heart rate zones "
                                + "and best efforts, newest first. Defaults to the last 30 days.",
                        schema(RANGE),
                        (userId, args) -> {
                            McpArguments.Range range = args.range(30, 366);
                            return runService.getRuns(userId, range.start(), range.end());
                        }),

                McpTool.read("get_run", "Lauf",
                        "One run with its splits, heart rate and route, the id comes from list_runs.",
                        schema(Map.of("id", string("Id of the run")), "id"),
                        (userId, args) -> runService.getRun(userId, uuid(args.requiredString("id")))),

                McpTool.read("get_nutrition", "Ernährung",
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

                McpTool.read("search_foods", "Lebensmittel suchen",
                        "Searches the food database (German names work best, e.g. \"Haferflocken\") for track_food. "
                                + "Returns the nutrients per 100 g, the portions (e.g. 1 Scheibe = 30 g) and the amount "
                                + "the user tracked last time (grams, or the number of portions). A result with openFoodId comes from Open Food Facts and "
                                + "can only be tracked in grams.",
                        schema(Map.of("query", string("Name of the food")), "query"),
                        (userId, args) -> searchFoods(foodService, userId, args.requiredString("query"))),

                McpTool.write("track_food", "Essen tracken",
                        "Tracks a food the user ate, like the app does. Give either foodId or openFoodId from "
                                + "search_foods, and either grams or portionId with portions (the number of portions). "
                                + "Tell the user what you tracked.",
                        schema(Map.of(
                                "foodId", id("Id of the food from search_foods"),
                                "openFoodId", id("openFoodId from search_foods, for foods from Open Food Facts"),
                                "grams", number("Eaten amount in grams"),
                                "portionId", id("Id of one of the portions of the food"),
                                "portions", number("Number of portions, e.g. 2 or 0.5, defaults to 1"),
                                "meal", enumeration("The meal", "BREAKFAST", "LUNCH", "DINNER", "SNACK"),
                                "date", date("The day it was eaten (YYYY-MM-DD), defaults to today")),
                                "meal"),
                        false,
                        (userId, args) -> trackFood(foodService, userId, args)),

                McpTool.write("delete_tracked_food", "Eintrag löschen",
                        "Removes a tracked food of the user, the id is the id of an entry from get_nutrition. "
                                + "Ask the user before removing anything they didn't ask to remove.",
                        schema(Map.of("trackedFoodId", id("Id of the entry")),
                                "trackedFoodId"),
                        true,
                        (userId, args) -> {
                            Long id = args.id("trackedFoodId");
                            if (id == null) {
                                throw new IllegalArgumentException("trackedFoodId is required");
                            }
                            foodService.deleteTrackedFoodOfUser(userId, id);
                            return Map.of("deleted", id);
                        }),

                McpTool.read("list_calendar_events", "Kalender",
                        "Planned events of the user's calendar (created or invited to). Defaults to the next 14 days "
                                + "when no range is given.",
                        schema(RANGE),
                        (userId, args) -> {
                            McpArguments.Range range = args.string("from") == null && args.string("to") == null
                                    ? new McpArguments.Range(LocalDate.now(McpArguments.ZONE),
                                            LocalDate.now(McpArguments.ZONE).plusDays(13))
                                    : args.range(14, 366);
                            return calendarService.getAllCalendarEvents(userId, range.start(), range.end());
                        }),

                McpTool.write("create_calendar_event", "Termin erstellen",
                        "Creates an event in the user's calendar, optionally with a workout template from "
                                + "list_workout_templates to plan a training. Times are local times in Europe/Berlin. "
                                + "Tell the user what you created.",
                        schema(Map.of(
                                "title", string("Title of the event"),
                                "start", dateTime("Start as local date and time, e.g. 2026-10-28T18:30"),
                                "end", dateTime("End as local date and time, defaults to one hour after the start"),
                                "workoutId", id("Id of a workout template from list_workout_templates"),
                                "recurrence", enumeration("How often the event repeats, leave it out for a single event",
                                        "DAILY", "WEEKLY", "MONTHLY"),
                                "recurrenceEnd", date("Last day (YYYY-MM-DD) the event repeats on, leave it out to repeat "
                                        + "forever")),
                                "title", "start"),
                        false,
                        (userId, args) -> createCalendarEvent(calendarService, userId, args)));

        tools = list.stream().collect(Collectors.toMap(McpTool::name, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    List<McpTool> all() {
        return List.copyOf(tools.values());
    }

    Optional<McpTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    // MARK: training

    private static WorkoutDto createWorkoutTemplate(WorkoutService workoutService, String userId,
            McpArguments args) {
        String name = args.requiredString("name");
        Map<Long, ExerciseDto> exercises = workoutService.getAllExercises(null, null).stream()
                .collect(Collectors.toMap(ExerciseDto::getId, Function.identity()));
        List<ExerciseItem> items = new ArrayList<>();
        for (McpArguments entry : args.objects("exercises")) {
            Long exerciseId = entry.id("exerciseId");
            if (exerciseId == null) {
                throw new IllegalArgumentException("every exercise needs an exerciseId");
            }
            ExerciseDto exercise = exercises.get(exerciseId);
            if (exercise == null) {
                throw new IllegalArgumentException(exerciseId + " is no exercise from list_exercises");
            }
            items.add(item(exercise, entry));
        }
        return workoutService.create(userId, new CreateWorkoutDto(name, null, items));
    }

    private static ExerciseItem item(ExerciseDto exercise, McpArguments entry) {
        if (CARDIO_TYPES.contains(exercise.getType())) {
            Integer minutes = entry.integer("minutes", 1, 600);
            if (minutes == null) {
                throw new IllegalArgumentException(exercise.getName() + " is cardio and needs minutes");
            }
            return new CardioItemDto(exercise, minutes, List.of());
        }
        List<SetDto> sets = new ArrayList<>();
        for (McpArguments set : entry.objects("sets")) {
            sets.add(new SetDto(sets.size(), set.integer("reps", 0, 1, 1000), set.nonNegative("weightKg", 0),
                    set.integer("rpe", 1, 10), set.integer("restSeconds", DEFAULT_REST_SECONDS, 0, 3600)));
        }
        return new StrengthItemDto(exercise, sets);
    }

    private static Map<String, Object> exercise(ExerciseDto exercise) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exerciseId", exercise.getId());
        result.put("name", exercise.getName());
        result.put("type", exercise.getType());
        return result;
    }

    // MARK: calendar

    private static CalendarEventDto createCalendarEvent(CalendarService calendarService, String userId,
            McpArguments args) {
        String title = args.requiredString("title");
        Instant start = args.dateTime("start");
        Instant end = args.string("end") == null ? start.plus(Duration.ofHours(1)) : args.dateTime("end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end has to be after start");
        }
        Recurrence recurrence = recurrence(args.string("recurrence"));
        LocalDate lastDay = args.date("recurrenceEnd", null);
        if (lastDay != null && recurrence == null) {
            throw new IllegalArgumentException("recurrenceEnd needs a recurrence");
        }
        // the end of the last day, so an occurrence on that day is still included
        Instant recurrenceEnd = lastDay == null ? null
                : lastDay.plusDays(1).atStartOfDay(McpArguments.ZONE).toInstant().minusSeconds(1);
        if (recurrenceEnd != null && recurrenceEnd.isBefore(start)) {
            throw new IllegalArgumentException("recurrenceEnd can't be before start");
        }
        return calendarService.createCalendarEvent(new CreateCalendarEventDto(title, start, end, userId,
                args.id("workoutId"), null, recurrence, recurrenceEnd));
    }

    private static Recurrence recurrence(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Recurrence.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("recurrence has to be DAILY, WEEKLY or MONTHLY");
        }
    }

    // MARK: nutrition

    private static List<Map<String, Object>> searchFoods(FoodService foodService, String userId, String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchFoodResponseDto hit : foodService.searchByNameAndUserId(PageRequest.of(0, SEARCH_RESULTS), query,
                userId)) {
            Map<String, Object> result = new LinkedHashMap<>();
            if (hit.id() != null) {
                FoodWithLastEntryAndPortionsDto food = foodService.getFoodWithLastEntry(userId, hit.id());
                result.put("foodId", food.id());
                result.put("name", food.name());
                result.put("per100g", nutrients(food.kcal(), food.protein(), food.carbohydrates(), food.fat(), food.fiber()));
                result.put("portions", food.portions().stream().map(McpTools::portion).toList());
                if (food.lastEntry() != null) {
                    result.put("lastTrackedAmount", food.lastEntry().amount());
                }
            } else {
                FoodWithPortionsDto food = foodService.getOpenFood(hit.openFoodId());
                result.put("openFoodId", hit.openFoodId());
                result.put("name", food.name());
                result.put("per100g", nutrients(food.kcal(), food.protein(), food.carbohydrates(), food.fat(), food.fiber()));
            }
            results.add(result);
        }
        return results;
    }

    private static Map<String, Object> trackFood(FoodService foodService, String userId, McpArguments args) {
        Long foodId = args.id("foodId");
        Long openFoodId = args.id("openFoodId");
        if ((foodId == null) == (openFoodId == null)) {
            throw new IllegalArgumentException("give either foodId or openFoodId");
        }
        MealType meal = meal(args.requiredString("meal"));
        Double grams = args.number("grams");
        Long portionId = args.id("portionId");
        if ((grams == null) == (portionId == null)) {
            throw new IllegalArgumentException("give either grams or portionId");
        }
        Double portions = args.number("portions");
        if (portionId == null && portions != null) {
            throw new IllegalArgumentException("portions needs a portionId");
        }

        String name;
        double kcalPer100g;
        double eatenGrams;
        if (foodId != null) {
            FoodWithLastEntryAndPortionsDto food = foodService.getFoodWithLastEntry(userId, foodId);
            name = food.name();
            kcalPer100g = food.kcal() != null ? food.kcal() : 0;
            if (portionId != null) {
                FoodPortionDto portion = food.portions().stream().filter((p) -> p.id().equals(portionId)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("portionId is no portion of " + food.name()));
                if (portion.grams() == null) {
                    throw new IllegalArgumentException("the portion has no weight, track it in grams");
                }
                eatenGrams = (portions != null ? portions : 1) * portion.grams();
            } else {
                eatenGrams = grams;
            }
        } else {
            if (portionId != null) {
                throw new IllegalArgumentException("foods from Open Food Facts can only be tracked in grams");
            }
            FoodWithPortionsDto food = foodService.getOpenFood(openFoodId);
            name = food.name();
            kcalPer100g = food.kcal() != null ? food.kcal() : 0;
            eatenGrams = grams;
        }

        // the app stores grams, or the number of portions when a portion is chosen
        double amount = portionId != null ? (portions != null ? portions : 1) : grams;
        LocalDate today = LocalDate.now(McpArguments.ZONE);
        LocalDate day = args.date("date", today);
        if (day.isAfter(today)) {
            throw new IllegalArgumentException("date can't be in the future");
        }
        // another day at noon, so it is the same day in UTC (tracked foods are counted in UTC days) and in Berlin
        Instant at = day.equals(today) ? null : day.atTime(LocalTime.NOON).atZone(McpArguments.ZONE).toInstant();
        TrackedFoodDto tracked = foodService.trackFoodAt(
                new CreateFoodUserDto(foodId, userId, amount, meal.name(), portionId), openFoodId, at);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trackedFoodId", tracked.id());
        result.put("food", name);
        result.put("meal", meal.name());
        result.put("date", day.toString());
        result.put("grams", Math.round(eatenGrams * 10) / 10.0);
        result.put("kcal", Math.round(eatenGrams * kcalPer100g / 100));
        return result;
    }

    private static Map<String, Object> portion(FoodPortionDto portion) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("portionId", portion.id());
        result.put("label", portion.label());
        result.put("grams", portion.grams());
        return result;
    }

    private static MealType meal(String value) {
        try {
            return MealType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("meal has to be BREAKFAST, LUNCH, DINNER or SNACK");
        }
    }

    private static Map<String, Object> nutrients(Double kcal, Double protein, Double carbohydrates, Double fat,
            Double fiber) {
        Map<String, Object> nutrients = new LinkedHashMap<>();
        nutrients.put("kcal", kcal);
        nutrients.put("protein", protein);
        nutrients.put("carbohydrates", carbohydrates);
        nutrients.put("fat", fat);
        nutrients.put("fiber", fiber);
        return nutrients;
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
