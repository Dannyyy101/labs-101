package com.labs_101.backend.mapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.calendar.CalendarUserDto;
import com.labs_101.backend.dtos.workout.CardioItemDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutExerciseDto;
import com.labs_101.backend.dtos.workout.ExerciseItem;
import com.labs_101.backend.dtos.workout.RunWorkoutItemDto;
import com.labs_101.backend.dtos.workout.SetDto;
import com.labs_101.backend.dtos.workout.StrengthItemDto;
import com.labs_101.backend.dtos.workout.StrengthWorkoutItemDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.dtos.workout.WorkoutHeaderDto;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.workout.ExerciseType;
import com.labs_101.backend.entities.workout.TrainingSet;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.entities.workout.WorkoutExercise;
import com.labs_101.backend.entities.workout.workoutExercises.CardioItem;
import com.labs_101.backend.entities.workout.workoutExercises.StrengthItem;

public class WorkoutMapper {

    public static WorkoutHeaderDto toWorkoutDto(Workout workout) {
        return null;
    }

    public static Workout fromCreateWorkoutDto(CreateWorkoutDto dto, List<Exercise> exercises) {
        ArrayList<WorkoutExercise> items = new ArrayList<>();

        return null;

    }

    public static WorkoutHeaderDto fromWorkoutToWorkoutHeaderDto(Workout workout) {
        return new WorkoutHeaderDto(workout.getId(), workout.getName());
    }

    /** The template as the user sees it, `editable` if they may change it, only its creator sees whom it is shared with. */
    public static WorkoutDto fromWorkoutToWorkoutDto(Workout workout, String userId, Function<String, String> avatarUrl) {
        List<ExerciseItem> items = workout.getExercises().stream()
                .map((exercise) -> fromWorkoutExerciseToExerciseItem(exercise)).toList();
        boolean editable = workout.isEditableBy(userId);
        List<CalendarUserDto> sharedWith = editable
                ? workout.getSharedWith().stream().map((user) -> CalendarMapper.fromUser(user, avatarUrl))
                        // sorted, the stored set has no order
                        .sorted(Comparator.comparing(CalendarUserDto::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList()
                : List.of();

        return new WorkoutDto(workout.getId(), workout.getName(), items,
                workout.getOwner() != null ? workout.getOwner().getName() : null, editable,
                !editable && workout.isSharedWith(userId), sharedWith);
    }

    private static ExerciseItem fromWorkoutExerciseToExerciseItem(WorkoutExercise<?> exercise) {
        return switch (exercise) {
            case StrengthItem s -> new StrengthItemDto(
                    ExerciseMapper.fromExercise(exercise.getExercise()),
                    s.getSettings().sets().stream()
                            .map(set -> new SetDto(set.order(), set.reps(), set.weightKg(), set.rpe(), set.restSeconds()))
                            .toList());
            case CardioItem c -> new CardioItemDto(ExerciseMapper.fromExercise(exercise.getExercise()),
                    c.getSettings().minutes(), List.of());
            default -> null;

        };
    }
}
