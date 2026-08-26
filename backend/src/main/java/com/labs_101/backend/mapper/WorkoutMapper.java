package com.labs_101.backend.mapper;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.labs_101.backend.dtos.workoutTemplate.CreateWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.GetWorkoutTemplateDto;
import com.labs_101.backend.dtos.workoutTemplate.PlanItemDto;
import com.labs_101.backend.dtos.workoutTemplate.RunItemDto;
import com.labs_101.backend.dtos.workoutTemplate.StrengthItemDto;

import com.labs_101.backend.entities.workoutTemplate.PlanItem;
import com.labs_101.backend.entities.workoutTemplate.PlannedSet;
import com.labs_101.backend.entities.workoutTemplate.RunItem;
import com.labs_101.backend.entities.workoutTemplate.StrengthItem;
import com.labs_101.backend.entities.workoutTemplate.WorkoutTemplate;

public class WorkoutMapper {
    public static WorkoutTemplate fromCreateRunDto(CreateWorkoutTemplateDto dto){
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
                    items.add(new StrengthItem(strengthItemDto.exerciseId(), strengthItemDto.order(), "strength-training", sets));
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

    public static GetWorkoutTemplateDto toGetWorkoutTemplateDto(WorkoutTemplate template){
        // Improve type checking later for now it is fine
        String type = template.getItems().getFirst().type();
        return new GetWorkoutTemplateDto(template.getId(), type, template.getName());
    }
}
