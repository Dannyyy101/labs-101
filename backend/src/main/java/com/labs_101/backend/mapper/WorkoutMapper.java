package com.labs_101.backend.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutExerciseDto;
import com.labs_101.backend.dtos.workout.RunWorkoutItemDto;
import com.labs_101.backend.dtos.workout.StrengthWorkoutItemDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.dtos.workoutTemplate.CreateWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.GetWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.PlanItemDto;
import com.labs_101.backend.dtos.workoutTemplate.RunItemDto;
import com.labs_101.backend.dtos.workoutTemplate.StrengthItemDto;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.workout.StrengthWorkoutItem;
import com.labs_101.backend.entities.workout.TrainingSet;
import com.labs_101.backend.entities.workout.Workout;
import com.labs_101.backend.entities.workout.WorkoutExercise;
import com.labs_101.backend.entities.workoutTemplate.PlanItem;
import com.labs_101.backend.entities.workoutTemplate.PlannedSet;
import com.labs_101.backend.entities.workoutTemplate.RunItem;
import com.labs_101.backend.entities.workoutTemplate.StrengthItem;
import com.labs_101.backend.entities.workoutTemplate.WorkoutTemplate;

public class WorkoutMapper {
    public static WorkoutTemplate fromCreateRunDto(CreateWorkoutTemplateDto dto) {
        ArrayList<PlanItem> items = new ArrayList<>();
        PlanItemDto i = dto.getItems().get(0);
        System.out.println(i.order());
        dto.getItems().forEach((itemDto) -> {
            switch (itemDto.type()) {
                case "strength-training":
                    StrengthItemDto strengthItemDto = (StrengthItemDto) itemDto;
                    ArrayList<PlannedSet> sets = new ArrayList<>();
                    strengthItemDto.sets().forEach((set) -> {
                        sets.add(new PlannedSet(set.weight(), set.reps(), set.order()));
                    });
                    items.add(new StrengthItem(strengthItemDto.exerciseId(), strengthItemDto.order(),
                            "strength-training", sets));
                    break;
                case "run-training":
                    RunItemDto runItemDto = (RunItemDto) itemDto;
                    items.add(new RunItem(runItemDto.exerciseId(), runItemDto.order(), "run-training"));
                    break;
                default:
                    break;
            }
        });
        return new WorkoutTemplate(null, dto.getName(), items);
    }

    public static GetWorkoutTemplateDto toGetWorkoutTemplateDto(WorkoutTemplate template) {
        // Improve type checking later for now it is fine
        String type = template.getItems().getFirst().type();
        return new GetWorkoutTemplateDto(template.getId(), type, template.getName());
    }

    public static WorkoutDto toWorkoutDto(Workout workout) {
        return null;
    }

    public static Workout fromCreateWorkoutDto(CreateWorkoutDto dto, List<Exercise> exercises) {
        ArrayList<WorkoutExercise> items = new ArrayList<>();

        dto.getItems().forEach((itemDto) -> {
            switch (itemDto.type()) {
                case "strength-training":
                    StrengthWorkoutItemDto strengthItemDto = (StrengthWorkoutItemDto) itemDto;
                    ArrayList<TrainingSet> sets = new ArrayList<>();
                    strengthItemDto.sets().forEach((set) -> {
                        sets.add(new TrainingSet(set.weight(), set.reps(), set.order()));
                    });
                    System.out.println(strengthItemDto.exerciseId());
                    Exercise exercise = exercises.stream().filter((e) -> e.getId().equals(strengthItemDto.exerciseId()))
                            .findFirst().orElseThrow();
                    items.add(new StrengthWorkoutItem(exercise, strengthItemDto.order(),
                            "strength-training", sets));
                    break;
                default:
                    break;
            }
        });
        return null;

    }
}
