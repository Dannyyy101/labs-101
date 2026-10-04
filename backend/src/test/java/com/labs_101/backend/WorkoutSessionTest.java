package com.labs_101.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.labs_101.backend.dtos.exercises.ExerciseDto;
import com.labs_101.backend.dtos.workout.CreateWorkoutDto;
import com.labs_101.backend.dtos.workout.SetDto;
import com.labs_101.backend.dtos.workout.StrengthItemDto;
import com.labs_101.backend.dtos.workout.WorkoutDto;
import com.labs_101.backend.dtos.workout.session.CreateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.SessionExerciseDto;
import com.labs_101.backend.dtos.workout.session.SessionSetDto;
import com.labs_101.backend.dtos.workout.session.UpdateWorkoutSessionDto;
import com.labs_101.backend.dtos.workout.session.WorkoutSessionDto;
import com.labs_101.backend.entities.Exercise;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.ExerciseRepository;
import com.labs_101.backend.repositories.UserRepository;
import com.labs_101.backend.services.WorkoutService;
import com.labs_101.backend.services.WorkoutSessionService;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = { EmbeddedPostgresConfiguration.class })
@Import({ WorkoutService.class, WorkoutSessionService.class })
public class WorkoutSessionTest {

    @Autowired
    private WorkoutService workoutService;
    @Autowired
    private WorkoutSessionService sessionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;

    private String userId;
    private ExerciseDto bench;
    private ExerciseDto squat;

    @BeforeEach
    void setUp() {
        userId = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();
        bench = exercise("Bankdrücken");
        squat = exercise("Kniebeuge");
    }

    @Test
    void testStartCopiesTemplate() {
        WorkoutDto workout = workout("Push", bench, squat);

        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));

        assertEquals("Push", session.name());
        assertEquals(workout.getId(), session.workoutId());
        assertNull(session.endedAt());
        assertEquals(List.of("Bankdrücken", "Kniebeuge"), session.exercises().stream().map(SessionExerciseDto::name).toList());
        SessionSetDto set = session.exercises().getFirst().sets().getFirst();
        assertEquals(10, set.reps());
        assertEquals(60, set.weightKg());
        assertTrue(session.exercises().stream().flatMap((e) -> e.sets().stream()).noneMatch(SessionSetDto::done));
        assertEquals(session.id(), sessionService.getActive(userId).orElseThrow().id());
    }

    @Test
    void testOnlyOneActiveSession() {
        sessionService.start(userId, new CreateWorkoutSessionDto(null, null));
        assertThrows(BadRequestException.class, () -> sessionService.start(userId, new CreateWorkoutSessionDto(null, null)));
    }

    @Test
    void testFinishKeepsDoneSets() {
        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(null, "Beine"));
        assertEquals("Beine", session.name());
        assertTrue(session.exercises().isEmpty());

        List<SessionExerciseDto> exercises = List.of(
                new SessionExerciseDto(squat.getId(), squat.getName(), List.of(
                        new SessionSetDto(5, 100, 8, true),
                        new SessionSetDto(5, 100, null, false))),
                new SessionExerciseDto(bench.getId(), bench.getName(), List.of(
                        new SessionSetDto(8, 70, null, false))));
        sessionService.update(userId, session.id(), new UpdateWorkoutSessionDto(null, exercises));
        assertEquals(2, sessionService.getSession(userId, session.id()).exercises().size());

        WorkoutSessionDto finished = sessionService.finish(userId, session.id(), null);

        assertNotNull(finished.endedAt());
        assertEquals(1, finished.exercises().size());
        assertEquals(List.of(new SessionSetDto(5, 100, 8, true)), finished.exercises().getFirst().sets());
        assertTrue(sessionService.getActive(userId).isEmpty());
        // a save arriving after finishing must not bring back the dropped sets
        assertThrows(BadRequestException.class,
                () -> sessionService.update(userId, session.id(), new UpdateWorkoutSessionDto(null, exercises)));

        List<WorkoutSessionDto> sessions = sessionService.getSessions(userId, Instant.now().minusSeconds(3600), null);
        assertEquals(List.of(finished.id()), sessions.stream().map(WorkoutSessionDto::id).toList());

        // a new one can be started once the last one is finished
        sessionService.start(userId, new CreateWorkoutSessionDto(null, null));
    }

    @Test
    void testSessionsOfOtherUsersAreHidden() {
        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(null, null));
        String other = userRepository.saveAndFlush(new User(UUID.randomUUID().toString())).getId();

        assertThrows(NotFoundException.class, () -> sessionService.getSession(other, session.id()));
        assertThrows(NotFoundException.class, () -> sessionService.delete(other, session.id()));
        assertTrue(sessionService.getActive(other).isEmpty());
    }

    @Test
    void testUpdateAndDeleteWorkout() {
        WorkoutDto workout = workout("Push", bench);
        WorkoutDto updated = workoutService.update(workout.getId(), new CreateWorkoutDto("Push B", null, List.of(
                new StrengthItemDto(squat, List.of(new SetDto(0, 5, 120, null))),
                new StrengthItemDto(bench, List.of()))));

        assertEquals("Push B", updated.getName());
        assertEquals(List.of(squat.getId(), bench.getId()),
                updated.getWorkoutExercises().stream().map((e) -> e.exercise().getId()).toList());

        WorkoutSessionDto session = sessionService.start(userId, new CreateWorkoutSessionDto(workout.getId(), null));
        sessionService.finish(userId, session.id(), null);
        workoutService.delete(workout.getId());

        // the history stays, without the template
        assertThrows(NotFoundException.class, () -> workoutService.getById(workout.getId()));
        assertNull(sessionService.getSession(userId, session.id()).workoutId());
    }

    private ExerciseDto exercise(String name) {
        Exercise exercise = exerciseRepository.saveAndFlush(new Exercise(null, name, "", "Strength Training", new ArrayList<>()));
        return new ExerciseDto(exercise.getId(), name, "", "Strength Training", new ArrayList<>());
    }

    private WorkoutDto workout(String name, ExerciseDto... exercises) {
        return workoutService.create(new CreateWorkoutDto(name, null, List.of(exercises).stream()
                .map((e) -> (com.labs_101.backend.dtos.workout.ExerciseItem) new StrengthItemDto(e, List.of(
                        new SetDto(0, 10, 60, null), new SetDto(1, 10, 60, null))))
                .toList()));
    }
}
