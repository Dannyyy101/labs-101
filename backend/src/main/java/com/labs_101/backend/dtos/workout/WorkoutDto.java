package com.labs_101.backend.dtos.workout;

import java.util.ArrayList;
import java.util.List;

import com.labs_101.backend.dtos.calendar.CalendarUserDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkoutDto extends WorkoutHeaderDto {
    private List<ExerciseItem> workoutExercises = new ArrayList<>();
    /** Name of the creator, null if unknown. */
    private String ownerName;
    /** Whether the requesting user may change and delete the template. */
    private boolean editable;
    /** Whether a friend shared the template with the requesting user. */
    private boolean sharedWithMe;
    /** The friends the template is shared with, only for its creator, empty for everyone else. */
    private List<CalendarUserDto> sharedWith = new ArrayList<>();

    public WorkoutDto(Long id, String name, List<ExerciseItem> exercises, String ownerName, boolean editable,
            boolean sharedWithMe, List<CalendarUserDto> sharedWith) {
        super(id, name);
        this.workoutExercises = exercises;
        this.ownerName = ownerName;
        this.editable = editable;
        this.sharedWithMe = sharedWithMe;
        this.sharedWith = sharedWith;
    }
}
